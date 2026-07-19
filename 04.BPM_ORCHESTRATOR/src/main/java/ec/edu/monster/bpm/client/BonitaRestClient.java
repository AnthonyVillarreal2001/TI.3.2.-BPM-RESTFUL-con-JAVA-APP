package ec.edu.monster.bpm.client;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class BonitaRestClient {

    private final String baseUrl;
    private final String username;
    private final String password;

    private String jsessionid;
    private String bpmToken;
    private String bpmTokenValue;
    
    private final HttpClient httpClient;

    public BonitaRestClient(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.username = username;
        this.password = password;
        this.httpClient = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .build();
    }

    /**
     * Authenticates with Bonita Portal REST API and stores session cookies.
     */
    public void login() throws IOException, InterruptedException {
        String loginUrl = baseUrl + "/loginservice";
        
        String form = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8)
                + "&redirect=false";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(loginUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 204) {
            throw new RuntimeException("Autenticación fallida en Bonita. Código de estado: " + response.statusCode());
        }

        // Extract session cookies manually
        Map<String, List<String>> headers = response.headers().map();
        List<String> setCookies = headers.get("set-cookie");
        if (setCookies != null) {
            for (String cookie : setCookies) {
                if (cookie.contains("JSESSIONID=")) {
                    jsessionid = extractCookieValue(cookie, "JSESSIONID");
                }
                if (cookie.contains("BPM_TOKEN=")) {
                    bpmToken = extractCookieValue(cookie, "BPM_TOKEN");
                    bpmTokenValue = bpmToken; // CSRF token is the value of BPM_TOKEN cookie
                }
            }
        }

        if (jsessionid == null || bpmToken == null) {
            throw new RuntimeException("No se recibieron las cookies JSESSIONID o BPM_TOKEN de Bonita. Verifique las credenciales.");
        }
    }

    private String extractCookieValue(String cookieHeader, String cookieName) {
        // e.g. JSESSIONID=abc123xyz; Path=/; HttpOnly
        for (String part : cookieHeader.split(";")) {
            part = part.trim();
            if (part.startsWith(cookieName + "=")) {
                return part.split("=", 2)[1];
            }
        }
        return null;
    }

    /**
     * Resolves the process definition ID by its process name and version.
     */
    public String getProcessId(String processName, String version) throws IOException, InterruptedException {
        if (jsessionid == null || bpmToken == null) {
            login();
        }

        String filterName = URLEncoder.encode("name=" + processName, StandardCharsets.UTF_8);
        String filterVersion = URLEncoder.encode("version=" + version, StandardCharsets.UTF_8);
        String processUrl = baseUrl + "/API/bpm/process?f=" + filterName + "&f=" + filterVersion;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(processUrl))
                .header("Cookie", "JSESSIONID=" + jsessionid + "; BPM_TOKEN=" + bpmToken)
                .header("X-Bonita-API-Token", bpmTokenValue)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Error al buscar proceso '" + processName + "'. Status code: " + response.statusCode());
        }

        JSONArray processes = new JSONArray(response.body());
        if (processes.isEmpty()) {
            throw new RuntimeException("Proceso no encontrado en Bonita: '" + processName + "' (versión: " + version + "). ¿Está desplegado y habilitado?");
        }

        JSONObject process = processes.getJSONObject(0);
        return process.getString("id");
    }

    /**
     * Instantiates a process in Bonita by sending the JSON payload required by the contract.
     */
    public String instantiateProcess(String processId, JSONObject contractInputs) throws IOException, InterruptedException {
        if (jsessionid == null || bpmToken == null) {
            login();
        }

        String url = baseUrl + "/API/bpm/process/" + processId + "/instantiation";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Cookie", "JSESSIONID=" + jsessionid + "; BPM_TOKEN=" + bpmToken)
                .header("X-Bonita-API-Token", bpmTokenValue)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(contractInputs.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new RuntimeException("Error al instanciar el proceso (ID: " + processId + "). Status code: " 
                    + response.statusCode() + ", Respuesta: " + response.body());
        }

        JSONObject result = new JSONObject(response.body());
        return result.optString("caseId", "Desconocido (Instanciación exitosa)");
    }
}
