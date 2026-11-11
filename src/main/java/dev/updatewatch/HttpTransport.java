package dev.updatewatch;

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Bounded HTTPS transport; every redirect is validated before opening a connection. */
final class HttpTransport {
    /** Default total attempts, including the first request. */
    static final int MAX_RETRIES = 3;
    static final String USER_AGENT = "PluginUpdateWatch/" + buildVersion() + " (+https://github.com/dev-dyly/PluginUpdateWatch)";
    private static String buildVersion() {
        var properties = new Properties();
        try (var in = HttpTransport.class.getResourceAsStream("/build.properties")) {
            if (in != null) properties.load(in);
        } catch (IOException ignored) { /* An unpackaged development build still identifies the client. */ }
        String version = properties.getProperty("version", "dev");
        return version.matches("[A-Za-z0-9.+_-]+") ? version : "dev";
    }
    private static final long MAX_INLINE_WAIT_MILLIS = 4000;
    interface Connector { HttpURLConnection connect(URI uri) throws IOException; }
    interface Sleeper { void sleep(long millis) throws InterruptedException; }
    private final Connector connector;
    private final Sleeper sleeper;
    private final Settings settings;
    private record Cooldown(int status, long untilMillis) {}
    private final Map<String, Cooldown> cooldowns = new HashMap<>();
    HttpTransport(Settings settings) { this(settings, HttpTransport::connectPublic, Thread::sleep); }
    HttpTransport(Settings settings, Connector connector, Sleeper sleeper) {
        this.settings = settings; this.connector = connector; this.sleeper = sleeper;
    }
    private static final Set<String> HOSTS = Set.of("api.github.com", "github.com", "release-assets.githubusercontent.com",
            "objects.githubusercontent.com", "github-releases.githubusercontent.com", "api.modrinth.com", "cdn.modrinth.com",
            "api.spiget.org", "cdn.spiget.org", "spigotmc.org", "www.spigotmc.org");
    static URI safeUri(String value) throws IOException {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getFragment() != null || (uri.getPort() != -1 && uri.getPort() != 443)
                    || !HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT)))
                throw new IOException("URL rejected: expected an approved HTTPS provider or download host; use the release page for external downloads");
            return uri;
        } catch (IllegalArgumentException | NullPointerException e) { throw new IOException("Malformed remote URL", e); }
    }
    /** Used only for explicitly configured web sources; DNS is checked before each connection. */
    static URI webUri(String value) throws IOException {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            if (value.length() > 4096 || !"https".equalsIgnoreCase(uri.getScheme()) || host == null
                    || uri.getUserInfo() != null || uri.getFragment() != null || (uri.getPort() != -1 && uri.getPort() != 443)
                    || !host.contains(".") || host.endsWith(".") || host.contains(":")
                    || host.matches("[0-9.]+") || host.toLowerCase(Locale.ROOT).endsWith(".localhost")
                    || host.toLowerCase(Locale.ROOT).endsWith(".local"))
                throw new IOException("Web source requires a public HTTPS hostname on port 443");
            return uri;
        } catch (IllegalArgumentException | NullPointerException e) { throw new IOException("Malformed web URL", e); }
    }
    static class Body extends FilterInputStream {
        final URI uri;
        Body(InputStream in, URI uri) { super(in); this.uri = uri; }
    }
    private static HttpURLConnection connectPublic(URI uri) throws IOException {
        for (InetAddress a : InetAddress.getAllByName(uri.getHost())) {
            byte[] b = a.getAddress();
            if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress()
                    || a.isMulticastAddress() || (a instanceof Inet6Address && (b[0] & 0xe0) != 0x20)
                    || (a instanceof Inet4Address && ((b[0] & 255) == 0 || (b[0] & 255) >= 224
                    || ((b[0] & 255) == 100 && (b[1] & 255) >= 64 && (b[1] & 255) <= 127))))
                throw new IOException("Non-public remote address rejected");
        }
        return (HttpURLConnection) uri.toURL().openConnection();
    }
    InputStream open(String url, int seconds) throws IOException {
        return open(url, seconds, false);
    }
    InputStream openWeb(String url, int seconds) throws IOException { return open(url, seconds, true); }
    private InputStream open(String url, int seconds, boolean web) throws IOException {
        URI initial = web ? webUri(url) : safeUri(url);
        if (seconds <= 0) throw new IOException("Request timeout must be positive");
        checkCooldown(initial.getHost());
        long deadline = System.nanoTime() + seconds * 1_000_000_000L;
        for (int attempt = 1; ; attempt++) {
            long delay = 250L * attempt;
            String waitedHost = null;
            try { return request(initial, deadline, web); }
            catch (Remote.HttpError error) {
                boolean rateLimited = error.code == 429 || (error.code == 403 && error.retryAfterSeconds > 0);
                if (!rateLimited && !retryable(error.code)) throw error;
                if (error.retryAfterSeconds > 0) {
                    rememberCooldown(initial.getHost(), error.host, error.code, error.retryAfterSeconds);
                    if (error.retryAfterSeconds > MAX_INLINE_WAIT_MILLIS / 1000 || attempt >= settings.attempts()) throw error;
                    delay = Math.max(delay, error.retryAfterSeconds * 1000);
                    if (remaining(deadline) <= delay) throw error;
                    waitedHost = error.host;
                } else if (attempt >= settings.attempts()) {
                    if (rateLimited) {
                        // An unspecified limit gets bounded attempts first, then a per-scan cooldown.
                        rememberCooldown(initial.getHost(), error.host, error.code, 60);
                        throw new Remote.HttpError(error.code, error.host, 60);
                    }
                    throw error;
                }
            } catch (SocketTimeoutException | SocketException | EOFException error) {
                if (attempt >= settings.attempts()) throw error;
            }
            if (remaining(deadline) <= delay) throw new SocketTimeoutException("Request deadline exceeded during retry backoff");
            try { sleeper.sleep(delay); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new InterruptedIOException("Request cancelled"); }
            if (waitedHost != null) {
                // The required wait completed; do not leave a stale cooldown after a successful retry.
                cooldowns.remove(hostKey(initial.getHost()));
                cooldowns.remove(hostKey(waitedHost));
            }
        }
    }
    static boolean retryable(int status) { return status == 408 || status == 429 || (status >= 500 && status <= 599); }
    private static String hostKey(String host) { return host.toLowerCase(Locale.ROOT); }
    private void rememberCooldown(String initialHost, String responseHost, int status, long seconds) {
        long now = System.currentTimeMillis();
        long until = seconds > (Long.MAX_VALUE - now) / 1000 ? Long.MAX_VALUE : now + seconds * 1000;
        var cooldown = new Cooldown(status, until);
        cooldowns.put(hostKey(initialHost), cooldown);
        cooldowns.put(hostKey(responseHost), cooldown);
    }
    private void checkCooldown(String host) throws Remote.HttpError {
        Cooldown cooldown = cooldowns.get(hostKey(host));
        if (cooldown == null) return;
        long millis = cooldown.untilMillis() - System.currentTimeMillis();
        if (millis > 0) throw new Remote.HttpError(cooldown.status(), host, millis / 1000 + (millis % 1000 == 0 ? 0 : 1));
        cooldowns.remove(hostKey(host));
    }
    private InputStream request(URI uri, long deadline, boolean web) throws IOException {
        Set<URI> visited = new HashSet<>();
        for (int redirects = 0; redirects <= 5; redirects++) {
            if (!visited.add(uri)) throw new IOException("Redirect loop detected");
            remaining(deadline);
            checkCooldown(uri.getHost());
            HttpURLConnection c = connector.connect(uri);
            boolean handedOff = false;
            try {
                c.setInstanceFollowRedirects(false);
                c.setConnectTimeout(Math.min(settings.connectMillis(), remaining(deadline)));
                c.setReadTimeout(Math.min(settings.readMillis(), remaining(deadline)));
                c.setRequestProperty("User-Agent", USER_AGENT);
                int status = c.getResponseCode();
                remaining(deadline);
                if (Set.of(301, 302, 303, 307, 308).contains(status)) {
                    String location = c.getHeaderField("Location");
                    if (location == null) throw new IOException("Redirect missing Location header");
                    try { uri = web ? webUri(uri.resolve(location).toString()) : safeUri(uri.resolve(location).toString()); }
                    catch (IllegalArgumentException e) { throw new IOException("Malformed redirect", e); }
                    continue;
                }
                if (status != 200) throw new Remote.HttpError(status, uri.getHost(), retryAfter(c, Instant.now()));
                InputStream body = c.getInputStream();
                handedOff = true;
                return new Body(body, uri) {
                    private void prepare() throws IOException { c.setReadTimeout(Math.min(settings.readMillis(), remaining(deadline))); }
                    @Override public int read() throws IOException { prepare(); int n = in.read(); remaining(deadline); return n; }
                    @Override public int read(byte[] b, int off, int len) throws IOException { prepare(); int n = in.read(b, off, len); remaining(deadline); return n; }
                    @Override public void close() throws IOException { try { super.close(); } finally { c.disconnect(); } }
                };
            } finally { if (!handedOff) c.disconnect(); }
        }
        throw new IOException("Too many redirects (maximum 5)");
    }
    private static int remaining(long deadline) throws IOException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Request cancelled");
        long millis = (deadline - System.nanoTime()) / 1_000_000;
        if (millis <= 0) throw new SocketTimeoutException("Request deadline exceeded");
        return (int) Math.min(Integer.MAX_VALUE, millis);
    }
    static long retryAfter(HttpURLConnection c, Instant now) {
        String value = c.getHeaderField("Retry-After");
        if (value != null) {
            try { return Math.max(1, Long.parseLong(value.trim())); } catch (NumberFormatException ignored) { }
            try {
                Duration wait = Duration.between(now, ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant());
                // HTTP dates have second precision; round up so the retry never starts early.
                return Math.max(1, wait.getSeconds() + (wait.getNano() == 0 ? 0 : 1));
            }
            catch (RuntimeException ignored) { }
        }
        if ("0".equals(c.getHeaderField("X-RateLimit-Remaining"))) {
            try {
                long reset = Long.parseLong(c.getHeaderField("X-RateLimit-Reset"));
                if (c.getURL().getHost().equalsIgnoreCase("api.github.com"))
                    return reset <= now.getEpochSecond() ? 1 : reset - now.getEpochSecond();
                return Math.max(1, reset);
            } catch (RuntimeException ignored) { }
        }
        return 0;
    }
}
