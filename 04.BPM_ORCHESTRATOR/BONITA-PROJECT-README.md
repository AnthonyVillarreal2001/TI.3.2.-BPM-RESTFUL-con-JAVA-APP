# Guía de Modelado y Configuración en Bonita Studio

Este documento detalla cómo crear, modelar y configurar los **4 procesos bancarios** (Consultar, Retirar, Depositar y Transferir) en **Bonita Studio Community Edition**, utilizando únicamente el portal y sus conectores REST nativos sin alterar el código interno de Bonita.

---

## 1. Instalación y Puesta en Marcha
1. Descarga e instala **Bonita Studio Community Edition** desde la web oficial de Bonitasoft.
2. Al arrancar el Studio, se levantará de forma automática el motor **Bonita Engine** embebido en un servidor Apache Tomcat local (generalmente en `http://localhost:8082/bonita` o `http://localhost:8080/bonita` si no hay colisión)## 2. Paso a Paso Detallado para Configurar los 4 Procesos en Bonita Studio (Español)

A continuación, se presenta la guía detallada paso a paso para crear, configurar y mapear cada uno de los 4 procesos bancarios utilizando la interfaz en español de Bonita Studio.

---

### PROCESO 1: ConsultarSaldo (Versión 1.0.0)

Este proceso permite consultar el saldo de una cuenta bancaria consumiendo el endpoint GET del servidor.

#### Paso 1.1: Crear el Pool del Proceso
1. En Bonita Studio, haz clic en **Archivo > Nuevo > Diagrama de proceso**.
2. Selecciona el borde exterior del **Pool** (el contenedor principal del diagrama).
3. En el panel inferior de propiedades, ve a la pestaña **General > Pool** (o **General > Proceso**) y define:
   - **Nombre**: `ConsultarSaldo`
   - **Versión**: `1.0.0`

#### Paso 1.2: Definir el Contrato de Inicio (Contrato)
El contrato define los parámetros que el cliente de consola debe enviar para iniciar el caso.
1. Con el **Pool** seleccionado, ve a la pestaña **Ejecución > Contrato**.
2. En la sección **Entradas**, haz clic en **Añadir**.
3. Configura la entrada con:
   - **Nombre**: `cuentaInput`
   - **Tipo**: `TEXT` (Texto)
   - **Multiplicidad**: `Única` (Single, para un solo valor)

#### Paso 1.3: Declarar y Mapear Variables de Proceso
Las variables de proceso guardan los datos durante el ciclo de vida del flujo.
1. Selecciona el **Pool** y ve a la pestaña **Datos > Variables del proceso**.
2. Haz clic en **Añadir** para crear la primera variable:
   - **Nombre**: `cuenta`
   - **Tipo**: `Texto`
   - **Valor por defecto**: Haz clic en el botón de lápiz (editar expresión) a la derecha, selecciona **Groovy** como tipo de expresión y escribe el siguiente código para leer del contrato:
     ```groovy
     return cuentaInput
     ```
3. Haz clic en **Añadir** para crear la segunda variable que guardará el JSON retornado por el servidor:
   - **Nombre**: `saldoResponse`
   - **Tipo**: `Texto`
   - (Deja el valor por defecto vacío)

#### Paso 1.4: Configurar la Tarea de Servicio (Conector REST GET)
1. Arrastra una tarea desde la paleta izquierda al diagrama.
2. Selecciónala, haz clic en el icono de llave inglesa (cambiar tipo) y cámbiale el tipo a **Tarea de servicio**. Nómbrala `Consultar Saldo REST`.
3. Con la tarea seleccionada, ve a **Ejecución > Conectores de salida** en el panel inferior.
4. Haz clic en **Añadir...**, selecciona **REST** en la lista y haz clic en **REST GET**. Pulsa **Siguiente**.
5. Nómbralo `getSaldoREST` y haz clic en **Siguiente**.
6. En la pestaña de configuración de conexión:
   - **URL**: Haz clic en el botón de lápiz a la derecha de la URL, selecciona **Groovy** y escribe:
     ```groovy
     return "http://localhost:8080/WSEurekaBank_Restfull_Java_G4/resources/corebancario/saldo?cuenta=" + cuenta
     ```
7. En la pestaña de parámetros de solicitud (Request parameters):
   - Agrega una cabecera (Header) con:
     - **Nombre**: `Accept`
     - **Valor**: `application/json`
   - Haz clic en **Siguiente**.
8. En la pestaña de operaciones de salida (Output operations):
   - Haz clic en **Añadir**.
   - En la columna izquierda, selecciona nuestra variable de proceso: `saldoResponse`.
   - En la columna del medio (operación), selecciona: `Toma el valor de` (Takes value of).
   - En la columna derecha (resultado), selecciona: `bodyAsString`.
9. Haz clic en **Finalizar**.

#### Paso 1.5: Configurar la Tarea de Usuario (Mostrar Saldo)
1. Arrastra una tarea a continuación de la tarea de servicio y conéctalas con una transición (flecha).
2. Selecciónala, haz clic en el icono de llave inglesa y asegúrate de que es de tipo **Tarea de usuario** (User Task / Tarea Humana). Nómbrala `Mostrar Saldo`.
3. Selecciona la tarea, ve a la pestaña **Ejecución > Formulario** y haz clic en **Nuevo...** al lado de "Formulario" para diseñar el formulario de resultados en el **UI Designer**.
4. En el **UI Designer**, configura las siguientes variables en el panel inferior:
   - Variable **`task`**:
     - **Nombre**: `task`
     - **Tipo**: `External API`
     - **URL**: `/bonita/API/bpm/userTask/{{taskId}}`
   - Variable **`datosSaldo`**:
     - **Nombre**: `datosSaldo`
     - **Tipo**: `External API`
     - **URL**: `/bonita/API/bpm/caseVariable/{{task.caseId}}/saldoResponse`
   - Variable **`saldoObjeto`** (Opcional, para parsear el JSON):
     - **Nombre**: `saldoObjeto`
     - **Tipo**: `JavaScript expression`
     - **Valor**: `return $data.datosSaldo && $data.datosSaldo.value ? JSON.parse($data.datosSaldo.value) : null;`
5. Arrastra un componente de **Texto** al formulario y edita su valor para mostrar el saldo:
   - Si usas la variable directa: `Saldo actual (JSON): {{datosSaldo.value}}`
   - Si usas el objeto parseado: `Saldo actual: ${{saldoObjeto.saldo}} USD`
6. Haz clic en **Guardar** (asegúrate de que el botón cambie a azul/guardado) y cierra el diseñador.

---

### PROCESO 2: Retirar (Versión 1.0.0)

Este proceso debita un importe de una cuenta realizando una petición POST.

#### Paso 2.1: Crear el Pool
1. Ve a **Archivo > Nuevo > Diagrama de proceso**.
2. Selecciona el Pool y en **General > Pool** define:
   - **Nombre**: `Retirar`
   - **Versión**: `1.0.0`

#### Paso 2.2: Definir el Contrato de Inicio
1. Con el **Pool** seleccionado, ve a **Ejecución > Contrato**.
2. Agrega dos entradas en la sección **Entradas**:
   - Entrada 1: **Nombre**: `cuentaInput` | **Tipo**: `TEXT`
   - Entrada 2: **Nombre**: `importeInput` | **Tipo**: `DECIMAL` (o DOUBLE)

#### Paso 2.3: Declarar y Mapear Variables
1. Selecciona el Pool y ve a **Datos > Variables del proceso**.
2. Crea las siguientes variables:
   - Variable 1: **Nombre**: `cuenta` | **Tipo**: `Texto` | **Valor por defecto (Groovy)**: `return cuentaInput`
   - Variable 2: **Nombre**: `importe` | **Tipo**: `Double` | **Valor por defecto (Groovy)**: `return importeInput`
   - Variable 3: **Nombre**: `retiroResponse` | **Tipo**: `Texto` | (Vacío)

#### Paso 2.4: Configurar la Tarea de Servicio (Conector REST POST)
1. Arrastra una tarea, cámbiale el tipo a **Tarea de servicio** y nómbrala `Registrar Retiro REST`.
2. Con la tarea seleccionada, ve a **Ejecución > Conectores de salida** y haz clic en **Añadir...**.
3. Selecciona **REST** y elige **REST POST**. Pulsa **Siguiente**.
4. Nómbralo `postRetiroREST` y haz clic en **Siguiente**.
5. En la configuración de conexión:
   - **URL (Expresión Groovy)**:
     ```groovy
     return "http://localhost:8080/WSEurekaBank_Restfull_Java_G4/resources/corebancario/retiro?cuenta=" + cuenta + "&importe=" + importe
     ```
6. En parámetros de solicitud:
   - Agrega la cabecera `Accept` con valor `application/json`.
7. En operaciones de salida:
   - Mapea la respuesta asignando `bodyAsString` a la variable de proceso `retiroResponse`.
8. Haz clic en **Finalizar**.

---

### PROCESO 3: Depositar (Versión 1.0.0)

Este proceso acredita un importe a una cuenta realizando una petición POST.

#### Paso 3.1: Configuración Inicial del Pool y Contrato
1. Crea un nuevo diagrama, selecciona el Pool y define:
   - **Nombre**: `Depositar` | **Versión**: `1.0.0`
2. En **Ejecución > Contrato**, agrega las entradas:
   - Entrada 1: **Nombre**: `cuentaInput` | **Tipo**: `TEXT`
   - Entrada 2: **Nombre**: `importeInput` | **Tipo**: `DECIMAL`

#### Paso 3.2: Declarar Variables
1. En **Datos > Variables del proceso**, agrega:
   - Variable 1: **Nombre**: `cuenta` | **Tipo**: `Texto` | **Valor por defecto (Groovy)**: `return cuentaInput`
   - Variable 2: **Nombre**: `importe` | **Tipo**: `Double` | **Valor por defecto (Groovy)**: `return importeInput`
   - Variable 3: **Nombre**: `depositoResponse` | **Tipo**: `Texto` | (Vacío)

#### Paso 3.3: Configurar el Conector REST POST
1. Crea una **Tarea de servicio** llamada `Registrar Depósito REST`.
2. En **Ejecución > Conectores de salida**, agrega un conector **REST POST**.
3. Configuración de URL (Expresión Groovy):
   ```groovy
   return "http://localhost:8080/WSEurekaBank_Restfull_Java_G4/resources/corebancario/deposito?cuenta=" + cuenta + "&importe=" + importe
   ```
4. Cabeceras de solicitud (Headers): `Accept: application/json`.
5. Mapea la respuesta asignando `bodyAsString` a la variable de proceso `depositoResponse`.

---

### PROCESO 4: Transferir (Versión 1.0.0)

Orquesta el envío de dinero entre dos cuentas bancarias mediante POST.

#### Paso 4.1: Configuración Inicial del Pool y Contrato
1. Crea un nuevo diagrama, selecciona el Pool y define:
   - **Nombre**: `Transferir` | **Versión**: `1.0.0`
2. En **Ejecución > Contrato**, agrega tres entradas:
   - Entrada 1: **Nombre**: `cuentaOrigenInput` | **Tipo**: `TEXT`
   - Entrada 2: **Nombre**: `cuentaDestinoInput` | **Tipo**: `TEXT`
   - Entrada 3: **Nombre**: `importeInput` | **Tipo**: `DECIMAL`

#### Paso 4.2: Declarar Variables
1. En **Datos > Variables del proceso**, agrega:
   - Variable 1: **Nombre**: `cuentaOrigen` | **Tipo**: `Texto` | **Valor por defecto (Groovy)**: `return cuentaOrigenInput`
   - Variable 2: **Nombre**: `cuentaDestino` | **Tipo**: `Texto` | **Valor por defecto (Groovy)**: `return cuentaDestinoInput`
   - Variable 3: **Nombre**: `importe` | **Tipo**: `Double` | **Valor por defecto (Groovy)**: `return importeInput`
   - Variable 4: **Nombre**: `transferResponse` | **Tipo**: `Texto` | (Vacío)

#### Paso 4.3: Configurar el Conector REST POST
1. Crea una **Tarea de servicio** llamada `Ejecutar Transferencia REST`.
2. En **Ejecución > Conectores de salida**, agrega un conector **REST POST**.
3. Configuración de URL (Expresión Groovy):
   ```groovy
   return "http://localhost:8080/WSEurekaBank_Restfull_Java_G4/resources/corebancario/transferencia?cuentaOrigen=" + cuentaOrigen + "&cuentaDestino=" + cuentaDestino + "&importe=" + importe
   ```
4. Cabeceras de solicitud (Headers): `Accept: application/json`.
5. Mapea la respuesta asignando `bodyAsString` a la variable de proceso `transferResponse`.


---

## 3. Guía de Creación de Formularios (UI Designer)

Bonita Studio incluye **Bonita UI Designer** para construir interfaces responsivas sin necesidad de codificar a mano:

1. **Formularios de Inicio (Formularios de Instanciación)**:
   - Selecciona el Pool del proceso en Bonita Studio.
   - Ve a la pestaña **Ejecución > Formulario de inicio** (Instantiation Form).
   - Haz clic en el icono del lápiz junto a "Formulario" (Target Form). Esto abrirá el **UI Designer** en tu navegador.
   - La herramienta creará automáticamente un formulario básico basado en el **Contrato de Inicio** (campos de entrada de texto para `cuentaInput` y botones).
   - Puedes personalizar los textos, alineaciones y colores arrastrando componentes desde el panel lateral izquierdo.
   
2. **Formularios de Tarea de Usuario (Task Forms - Lectura de Variables)**:
   Debido a que el endpoint `/context` solo contiene metadatos de BDM (Business Data Model), para recuperar variables de proceso estándar (globales del Pool) en una tarea humana, debes recuperarlas a través del endpoint de consulta del caso (`caseVariable`). Sigue esta estructura en el **UI Designer**:
   
   - **Paso A: Obtener el ID del Caso**
     Crea una variable de tipo **External API**:
     * **Nombre**: `task`
     * **URL**: `/bonita/API/bpm/userTask/{{taskId}}`
     *(Esto trae el detalle de la tarea humana actual, incluyendo el `caseId` del proceso).*

   - **Paso B: Consultar la Variable del Proceso**
     Crea otra variable de tipo **External API** que consuma la variable del pool (reemplazando `nombreDeVariable` por la de tu proceso):
     * **Nombre**: `datosRespuesta`
     * **URL**: `/bonita/API/bpm/caseVariable/{{task.caseId}}/nombreDeVariable`
     *(Ejemplos: `/bonita/API/bpm/caseVariable/{{task.caseId}}/saldoResponse`, `/bonita/API/bpm/caseVariable/{{task.caseId}}/retiroResponse`, etc.)*

   - **Paso C: Parsear y Mostrar en Pantalla**
     Dado que la respuesta viene como un JSON que contiene un campo `value` con el valor real en texto plano (el cual suele ser otro JSON), crea una variable de tipo **JavaScript expression**:
     * **Nombre**: `resultado`
     * **Valor**:
       ```javascript
       return $data.datosRespuesta && $data.datosRespuesta.value ? JSON.parse($data.datosRespuesta.value) : null;
       ```
     * En tu formulario, arrastra un componente de **Texto** y en su propiedad **Texto/Valor** usa las llaves dobles de AngularJS para renderizar las propiedades:
       ```html
       Respuesta del servidor: {{resultado}}
       ```

---

## 4. Ejecución desde el Cliente de Consola

Una vez que tus 4 procesos están desplegados en Bonita Portal y el servidor Payara del core bancario está activo:
1. Inicia el cliente de consola Java (`04.BPM_ORCHESTRATOR`).
2. Elige la transacción en el menú.
3. El cliente se conectará por REST a Bonita, iniciará el caso del proceso correspondiente y te devolverá el **Case ID**.
4. Puedes ir a **Bonita Portal** (`http://localhost:8082/bonita`) y revisar la bandeja de entrada para verificar que la tarea humana final contiene la respuesta correcta del servidor bancario.
