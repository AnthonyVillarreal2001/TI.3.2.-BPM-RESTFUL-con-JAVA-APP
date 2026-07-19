package ec.edu.monster.bpm;

import ec.edu.monster.bpm.client.BonitaRestClient;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

public class MainApp {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   INICIANDO CLIENTE DE CONTROL BONITA BPM       ");
        System.out.println("=================================================");

        Properties props = new Properties();
        try (InputStream input = MainApp.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.err.println("Error: No se pudo encontrar el archivo application.properties");
                return;
            }
            props.load(input);
        } catch (Exception e) {
            System.err.println("Error al cargar configuración: " + e.getMessage());
            return;
        }

        String url = props.getProperty("bonita.url");
        String user = props.getProperty("bonita.username");
        String pass = props.getProperty("bonita.password");
        String version = props.getProperty("bonita.process.version", "1.0.0");

        BonitaRestClient client = new BonitaRestClient(url, user, pass);
        
        System.out.println("Configuración cargada:");
        System.out.println("-> URL de Bonita: " + url);
        System.out.println("-> Usuario: " + user);
        System.out.println("-> Versión de procesos: " + version);
        System.out.println("Intentando conectar al motor de Bonita...");

        try {
            client.login();
            System.out.println("[OK] Conexión establecida y sesión iniciada en Bonita Portal.");
        } catch (Exception e) {
            System.err.println("[ADVERTENCIA] No se pudo establecer conexión inicial con Bonita BPM.");
            System.err.println("Detalle del error: " + e.getMessage());
            System.err.println("Nota: Podrás continuar, pero las solicitudes al motor fallarán si no levantas Bonita Studio.");
        }

        Scanner scanner = new Scanner(System.in);
        boolean salir = false;

        while (!salir) {
            System.out.println("\n=================================================");
            System.out.println("          MENÚ DE OPERACIONES BANCARIAS          ");
            System.out.println("=================================================");
            System.out.println("1. CONSULTAR SALDO (Proceso: " + props.getProperty("bonita.process.consultar") + ")");
            System.out.println("2. RETIRAR DINERO (Proceso: " + props.getProperty("bonita.process.retirar") + ")");
            System.out.println("3. DEPOSITAR DINERO (Proceso: " + props.getProperty("bonita.process.depositar") + ")");
            System.out.println("4. TRANSFERENCIA (Proceso: " + props.getProperty("bonita.process.transferir") + ")");
            System.out.println("5. SALIR");
            System.out.print("Seleccione una opción: ");

            String opcion = scanner.nextLine().trim();

            try {
                switch (opcion) {
                    case "1":
                        ejecutarConsulta(scanner, client, props, version);
                        break;
                    case "2":
                        ejecutarRetiro(scanner, client, props, version);
                        break;
                    case "3":
                        ejecutarDeposito(scanner, client, props, version);
                        break;
                    case "4":
                        ejecutarTransferencia(scanner, client, props, version);
                        break;
                    case "5":
                        salir = true;
                        System.out.println("Saliendo del Cliente de Control Bonita. ¡Hasta luego!");
                        break;
                    default:
                        System.err.println("Opción no válida. Intente de nuevo.");
                }
            } catch (Exception e) {
                System.err.println("\n[ERROR] Ocurrió un fallo al disparar el proceso BPM:");
                System.err.println("-> " + e.getMessage());
            }
        }
        scanner.close();
    }

    private static void ejecutarConsulta(Scanner scanner, BonitaRestClient client, Properties props, String version) throws Exception {
        System.out.println("\n--- OPERACIÓN: CONSULTAR SALDO ---");
        System.out.print("Ingrese el número de cuenta: ");
        String cuenta = scanner.nextLine().trim();

        if (cuenta.isEmpty()) {
            System.err.println("El número de cuenta no puede estar vacío.");
            return;
        }

        // Build contract inputs for Bonita process
        JSONObject inputs = new JSONObject();
        inputs.put("cuentaInput", cuenta);

        String processName = props.getProperty("bonita.process.consultar");
        System.out.println("Buscando proceso '" + processName + "'...");
        String processId = client.getProcessId(processName, version);

        System.out.println("Iniciando instancia de proceso (ID Proceso: " + processId + ")...");
        String caseId = client.instantiateProcess(processId, inputs);

        System.out.println("[ÉXITO] Caso de Consulta de Saldo iniciado en Bonita.");
        System.out.println("ID del Caso (Case ID): " + caseId);
        System.out.println("El proceso ejecutará la consulta REST al Core Bancario en el fondo.");
    }

    private static void ejecutarRetiro(Scanner scanner, BonitaRestClient client, Properties props, String version) throws Exception {
        System.out.println("\n--- OPERACIÓN: RETIRAR DINERO ---");
        System.out.print("Ingrese el número de cuenta: ");
        String cuenta = scanner.nextLine().trim();
        System.out.print("Ingrese el importe a retirar: ");
        double importe = Double.parseDouble(scanner.nextLine().trim());

        if (cuenta.isEmpty() || importe <= 0) {
            System.err.println("Datos inválidos. La cuenta no debe estar vacía y el importe debe ser mayor a 0.");
            return;
        }

        JSONObject inputs = new JSONObject();
        inputs.put("cuentaInput", cuenta);
        inputs.put("importeInput", importe);

        String processName = props.getProperty("bonita.process.retirar");
        System.out.println("Buscando proceso '" + processName + "'...");
        String processId = client.getProcessId(processName, version);

        System.out.println("Iniciando instancia de proceso (ID Proceso: " + processId + ")...");
        String caseId = client.instantiateProcess(processId, inputs);

        System.out.println("[ÉXITO] Caso de Retiro de Dinero iniciado en Bonita.");
        System.out.println("ID del Caso (Case ID): " + caseId);
    }

    private static void ejecutarDeposito(Scanner scanner, BonitaRestClient client, Properties props, String version) throws Exception {
        System.out.println("\n--- OPERACIÓN: DEPOSITAR DINERO ---");
        System.out.print("Ingrese el número de cuenta: ");
        String cuenta = scanner.nextLine().trim();
        System.out.print("Ingrese el importe a depositar: ");
        double importe = Double.parseDouble(scanner.nextLine().trim());

        if (cuenta.isEmpty() || importe <= 0) {
            System.err.println("Datos inválidos. La cuenta no debe estar vacía y el importe debe ser mayor a 0.");
            return;
        }

        JSONObject inputs = new JSONObject();
        inputs.put("cuentaInput", cuenta);
        inputs.put("importeInput", importe);

        String processName = props.getProperty("bonita.process.depositar");
        System.out.println("Buscando proceso '" + processName + "'...");
        String processId = client.getProcessId(processName, version);

        System.out.println("Iniciando instancia de proceso (ID Proceso: " + processId + ")...");
        String caseId = client.instantiateProcess(processId, inputs);

        System.out.println("[ÉXITO] Caso de Depósito de Dinero iniciado en Bonita.");
        System.out.println("ID del Caso (Case ID): " + caseId);
    }

    private static void ejecutarTransferencia(Scanner scanner, BonitaRestClient client, Properties props, String version) throws Exception {
        System.out.println("\n--- OPERACIÓN: TRANSFERENCIA BANCARIA ---");
        System.out.print("Ingrese el número de cuenta origen: ");
        String cuentaOrigen = scanner.nextLine().trim();
        System.out.print("Ingrese el número de cuenta destino: ");
        String cuentaDestino = scanner.nextLine().trim();
        System.out.print("Ingrese el importe a transferir: ");
        double importe = Double.parseDouble(scanner.nextLine().trim());

        if (cuentaOrigen.isEmpty() || cuentaDestino.isEmpty() || importe <= 0) {
            System.err.println("Datos inválidos. Las cuentas no deben estar vacías y el importe debe ser mayor a 0.");
            return;
        }

        JSONObject inputs = new JSONObject();
        inputs.put("cuentaOrigenInput", cuentaOrigen);
        inputs.put("cuentaDestinoInput", cuentaDestino);
        inputs.put("importeInput", importe);

        String processName = props.getProperty("bonita.process.transferir");
        System.out.println("Buscando proceso '" + processName + "'...");
        String processId = client.getProcessId(processName, version);

        System.out.println("Iniciando instancia de proceso (ID Proceso: " + processId + ")...");
        String caseId = client.instantiateProcess(processId, inputs);

        System.out.println("[ÉXITO] Caso de Transferencia Bancaria iniciado en Bonita.");
        System.out.println("ID del Caso (Case ID): " + caseId);
    }
}
