# Prueba Técnica – Plataforma Bancaria

API REST para que los trabajadores de una entidad financiera administren
clientes, productos financieros (cuentas de ahorro y corriente) y las
transacciones (consignaciones, retiros y transferencias) sobre esos
productos. Incluye un frontend para operar la API desde el navegador.

**Estado:** 90 pruebas automatizadas en verde, 94,8% de cobertura de líneas
verificada en cada construcción.

## Índice

- [Arquitectura](#arquitectura)
- [Patrones de diseño](#patrones-de-diseño)
- [Principios SOLID](#principios-solid)
- [Principio ACID y concurrencia](#principio-acid-y-concurrencia)
- [Modelo de datos (DDL/DML)](#modelo-de-datos-ddldml)
- [Reglas de negocio](#reglas-de-negocio)
- [API](#api)
- [Cómo ejecutar el proyecto](#cómo-ejecutar-el-proyecto)
- [Tests](#tests)
- [Servicios en la nube](#servicios-en-la-nube)
- [Control de versiones](#control-de-versiones)
- [Decisiones de diseño y alcance](#decisiones-de-diseño-y-alcance)

## Arquitectura

Backend en **Java 17 + Spring Boot 3.3**, organizado por **capas dentro de
cada módulo de dominio**. Las cuatro capas que exige el enunciado están
presentes en cada módulo:

| Capa | Ejemplo en el módulo `cuenta` |
|---|---|
| `entity` | `Cuenta`, `EstadoCuenta`, `TipoCuenta` |
| `repository` | `CuentaRepository` |
| `service` | `CuentaService` (interfaz) + `CuentaServiceImpl` |
| `controller` | `CuentaController` |
| (apoyo) | `dto/`, `CuentaMapper`, `NumeroCuentaGenerator` |

El enunciado permite "hexagonal en lo posible **o MVC**". Se implementó la
variante por capas equivalente a MVC (Modelo = entidades JPA, Vista = DTOs
serializados como JSON, Controlador = `@RestController`), agrupando cada
módulo de negocio en su propio paquete en lugar de usar paquetes
horizontales globales (`entities/`, `services/`, …). La razón es de
cohesión: todo lo relativo a "Cuentas" vive junto, lo que reduce el
acoplamiento entre módulos y facilita ubicar el código.

No se adoptó arquitectura hexagonal pura (puertos y adaptadores con el
dominio aislado de JPA) porque a este tamaño la capa extra de indirección
—interfaces de puerto, adaptadores y un segundo juego de mapeadores
dominio↔persistencia— no aportaría beneficio proporcional. Aun así se
conserva su objetivo principal, que la lógica de negocio no dependa de
detalles de framework:

- los controladores dependen de **interfaces** de servicio, no de sus
  implementaciones;
- los DTOs aíslan el contrato HTTP del modelo de persistencia;
- las clases de dominio (`NumeroCuentaGenerator`, `GmfCalculator`, las
  estrategias `OperacionCuenta`) no dependen de Spring MVC ni de JPA y se
  prueban de forma aislada.

```
backend/src/main/java/com/bancaria/prueba/
├── cliente/           # entity, repository, service (+impl), controller, mapper, dto/
├── cuenta/            # ídem + NumeroCuentaGenerator
├── transaccion/       # ídem + GmfCalculator + operacion/ (patrón Strategy)
├── common/
│   ├── exception/     # excepciones de negocio + manejador global
│   └── validation/    # validador @MinAge
└── config/            # OpenAPI (Swagger) y CORS
```

## Patrones de diseño

| Patrón | Dónde | Para qué |
|---|---|---|
| **Strategy** | `OperacionCuenta` con `CreditoOperacion` / `DebitoOperacion` | Encapsula el efecto de un movimiento sobre el saldo. Consignación y el lado receptor de una transferencia reutilizan el crédito; retiro, cobro de GMF y el lado emisor reutilizan el débito, sin condicionales repetidos. |
| **Registry** | `OperacionCuentaRegistry` | Spring inyecta **todas** las implementaciones de `OperacionCuenta` y el registro las indexa por `TipoOperacion`. El servicio depende solo de la abstracción: publicar una nueva estrategia no obliga a tocar el registro ni la lógica existente. |
| **Repository** | `ClienteRepository`, `CuentaRepository`, `MovimientoRepository` | Aísla el acceso a datos detrás de una interfaz (Spring Data JPA). |
| **DTO + Mapper** | `*Request` / `*Response` (records) + interfaces MapStruct | Evita exponer entidades JPA en la API y desacopla el contrato HTTP del modelo de persistencia. |
| **Builder** | Entidades vía Lombok `@Builder` | Construcción legible de objetos con varios campos. |
| **Factory / Generator** | `NumeroCuentaGenerator` | Aísla y hace testeable la generación de números de cuenta únicos con el prefijo de cada tipo. |
| **Handler centralizado** | `GlobalExceptionHandler` (`@RestControllerAdvice`) | Traduce excepciones de negocio, validación e infraestructura a respuestas HTTP consistentes en un solo lugar. |
| **Inyección de dependencias** | Constructores generados con `@RequiredArgsConstructor` | Los servicios reciben abstracciones, no implementaciones concretas. |

## Principios SOLID

- **S – Responsabilidad única**: `NumeroCuentaGenerator` solo genera
  números de cuenta; `GmfCalculator` solo calcula el gravamen;
  `MinAgeValidator` solo valida mayoría de edad; `ClienteMapper` solo
  convierte entre entidad y DTOs.
- **O – Abierto/cerrado**: para introducir un nuevo efecto sobre el saldo
  basta publicar una implementación de `OperacionCuenta` como bean; el
  `OperacionCuentaRegistry` la incorpora automáticamente y las estrategias
  de crédito y débito ya existentes no se modifican.
- **L – Sustitución de Liskov**: cualquier implementación de
  `ClienteService`, `CuentaService` o `TransaccionService` es
  intercambiable para los controladores que las consumen; igualmente
  cualquier `OperacionCuenta` es intercambiable para el servicio.
- **I – Segregación de interfaces**: cada módulo expone su propia interfaz
  de servicio con solo las operaciones que le competen, en vez de una
  interfaz genérica compartida.
- **D – Inversión de dependencias**: los controladores dependen de las
  interfaces de servicio (no de `*ServiceImpl`); `TransaccionServiceImpl`
  depende de la abstracción `OperacionCuenta` resuelta por el registro, no
  de `CreditoOperacion` / `DebitoOperacion`; y los servicios dependen de
  interfaces de repositorio, no de una tecnología de persistencia concreta.

## Principio ACID y concurrencia

La operación más sensible es la **transferencia entre cuentas**
(`TransaccionServiceImpl.transferir`): debita el origen, acredita el
destino, cobra el GMF cuando aplica y registra los movimientos, todo
dentro de un único método `@Transactional`.

- **Atomicidad**: si cualquier paso falla (por ejemplo, saldo insuficiente
  para cubrir el gravamen), Spring revierte la transacción completa; nunca
  queda una cuenta debitada sin su crédito correspondiente.
- **Consistencia**: las reglas se validan antes de tocar el saldo y la base
  de datos las refuerza con `CHECK` y claves foráneas
  (`ck_cuentas_saldo_ahorros_no_negativo`, entre otras).
- **Aislamiento**: `READ COMMITTED` (por defecto en PostgreSQL) evita leer
  escrituras no confirmadas, pero **no** evita por sí solo la pérdida de
  actualizaciones en un ciclo leer-modificar-escribir. Por eso `Cuenta`
  lleva un campo `@Version`: si dos operaciones concurrentes modifican la
  misma cuenta, la segunda falla con `OptimisticLockingFailureException`
  (traducida a **HTTP 409**) en lugar de sobrescribir a la primera. El
  comportamiento está demostrado en `ConcurrenciaCuentaComponentTest`.
- **Durabilidad**: confirmada la transacción, PostgreSQL garantiza la
  persistencia mediante su WAL.

## Modelo de datos (DDL/DML)

El esquema se versiona con **Flyway** en
[`backend/src/main/resources/db/migration`](backend/src/main/resources/db/migration):

| Migración | Contenido |
|---|---|
| `V1__crear_tablas.sql` | **DDL**: tablas `clientes`, `cuentas`, `movimientos`, con claves foráneas, índices, `UNIQUE` (identificación, correo, número de cuenta) y `CHECK` de dominio, incluido el que impide saldo negativo en cuentas de ahorro. |
| `V2__datos_semilla.sql` | **DML**: datos de ejemplo para probar la API y el frontend nada más levantar el proyecto. |
| `V3__control_concurrencia_y_gmf.sql` | **DDL**: columna `version` para el bloqueo optimista y ampliación del `CHECK` de `tipo_movimiento` para admitir el movimiento `GMF`. |

Ejemplos de DML que ejecuta la aplicación en tiempo de ejecución:

```sql
-- Al crear un cliente
INSERT INTO clientes (id, tipo_identificacion, numero_identificacion, nombres, apellido,
                      correo_electronico, fecha_nacimiento, fecha_creacion)
VALUES (?, ?, ?, ?, ?, ?, ?, ?);

-- Al retirar de una cuenta no exenta: saldo, movimiento y cobro del gravamen.
-- El WHERE sobre `version` es el que materializa el bloqueo optimista.
UPDATE cuentas SET saldo = ?, fecha_modificacion = ?, version = version + 1
WHERE id = ? AND version = ?;
INSERT INTO movimientos (id, cuenta_id, tipo_movimiento, valor, saldo_disponible, fecha, descripcion)
VALUES (?, ?, 'RETIRO', ?, ?, ?, ?), (?, ?, 'GMF', ?, ?, ?, ?);

-- Estado de cuenta
SELECT * FROM movimientos WHERE cuenta_id = ? ORDER BY fecha DESC;
```

## Reglas de negocio

**Clientes**
- No se puede crear un cliente menor de edad (validador propio `@MinAge`).
- No se puede eliminar un cliente con productos vinculados.
- `fechaCreacion` y `fechaModificacion` se calculan automáticamente
  (`@PrePersist` / `@PreUpdate`) y la fecha de modificación viaja ya
  resuelta en la respuesta de la actualización.
- Correo con formato válido, nombres y apellido de mínimo 2 caracteres,
  identificación y correo únicos.

**Cuentas (productos financieros)**
- Solo dos tipos: `AHORROS` o `CORRIENTE`, y solo existen vinculadas a un
  cliente.
- Número de 10 dígitos, único y autogenerado, con prefijo `53` (ahorros) o
  `33` (corriente).
- Se crean **activas** y con saldo `$0`.
- Las cuentas de ahorro nunca quedan con saldo negativo.
- Se pueden activar/inactivar en cualquier momento; solo se cancelan con
  saldo exactamente `$0`, y **la cancelación es definitiva** (una cuenta
  cancelada no se reactiva ni se modifica).

**Transacciones**
- Tres operaciones: consignación, retiro y transferencia entre cuentas.
- Cada operación actualiza el saldo y registra el movimiento con el saldo
  disponible resultante.
- Solo operan cuentas en estado `ACTIVA`.
- Las transferencias exigen dos cuentas existentes y distintas, y generan
  el movimiento de crédito en la receptora y el de débito en la emisora.
- **GMF (4x1000)**: los retiros y las transferencias cobran el gravamen a
  la cuenta origen salvo que esté marcada como exenta. Se registra como un
  movimiento `GMF` independiente para que quede trazado, y se descuenta
  como un débito más, de modo que la validación de saldo de las cuentas de
  ahorro también lo cubre. La tarifa es configurable (`app.gmf.tarifa`).

## API

Documentación interactiva (Swagger UI) con la aplicación levantada:
`http://localhost:8080/swagger-ui.html`.

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/clientes` | Registrar cliente |
| `GET` | `/api/clientes` · `/api/clientes/{id}` | Consultar clientes |
| `PUT` | `/api/clientes/{id}` | Actualizar cliente |
| `DELETE` | `/api/clientes/{id}` | Eliminar cliente sin productos vinculados |
| `POST` | `/api/cuentas` | Crear cuenta para un cliente |
| `GET` | `/api/cuentas?clienteId=` · `/api/cuentas/{id}` | Consultar cuentas |
| `PATCH` | `/api/cuentas/{id}` | Actualizar la exención de GMF |
| `PATCH` | `/api/cuentas/{id}/estado` | Activar, inactivar o cancelar |
| `POST` | `/api/transacciones/consignaciones` | Consignar |
| `POST` | `/api/transacciones/retiros` | Retirar |
| `POST` | `/api/transacciones/transferencias` | Transferir entre cuentas |
| `GET` | `/api/transacciones?cuentaId=` | Listar movimientos |
| `GET` | `/api/transacciones/cuentas/{cuentaId}/estado` | Estado de cuenta |

Códigos de estado que devuelve la API:

| Situación | Código |
|---|---|
| Validación de campos, UUID mal formado, JSON inválido, enum no admitido | `400` |
| Recurso o ruta inexistente | `404` |
| Regla de negocio violada (menor de edad, saldo insuficiente, cliente con productos…) | `422` |
| Restricción de integridad o conflicto de concurrencia | `409` |

## Cómo ejecutar el proyecto

### Con Docker (recomendado)

```bash
docker compose up --build
```

Levanta PostgreSQL, el backend (`http://localhost:8080`) y el frontend
(`http://localhost:5173`). Flyway aplica las migraciones y los datos de
ejemplo automáticamente al arrancar.

### En local, sin Docker

El proyecto incluye el **Maven wrapper**, así que no hace falta tener Maven
instalado; sí un **JDK 17 o superior**:

```bash
cd backend
./mvnw spring-boot:run     # requiere PostgreSQL en localhost:5432

cd frontend
npm install && npm run dev
```

Las credenciales y la URL de la base de datos se configuran por variables
de entorno (ver `.env.example` y `backend/src/main/resources/application.yml`).

> **Sobre la versión del JDK.** El proyecto compila a bytecode de Java 17,
> que es su versión objetivo, pero se construye y se prueba correctamente
> con cualquier JDK desde el 17 en adelante. Spring Boot 3.3 fija versiones
> de Lombok, Mockito y Byte Buddy anteriores al soporte de los JDK más
> recientes, así que el `pom.xml` las eleva de forma explícita: sin ese
> ajuste, quien tuviera instalado un JDK 24 o posterior no podría ni
> compilar el proyecto. Las tres son dependencias de compilación o de
> pruebas, de modo que no alteran el comportamiento en producción.

## Tests

```bash
cd backend
./mvnw verify      # ejecuta las pruebas y valida el umbral de cobertura
```

**90 pruebas**, todas en verde:

- **Unitarias** (JUnit 5 + Mockito): capa `Service` de los tres módulos y
  clases de dominio (`NumeroCuentaGenerator`, `MinAgeValidator`,
  `GmfCalculator`, las estrategias y su registro), con las dependencias
  mockeadas.
- **De componentes** (capa `Controller`): `@WebMvcTest` + MockMvc con
  contexto Spring real, verificando el contrato HTTP de cada endpoint.
- **De componentes de extremo a extremo**: `@SpringBootTest` sin mocks,
  ejercitando Controller → Service → Repository → base de datos real (H2
  con el mismo esquema Flyway de producción). Cubren el flujo completo
  (cliente → cuentas → consignación → retiro → transferencia → estado de
  cuenta), las reglas de negocio, el cobro del GMF, los códigos de error y
  el control de concurrencia optimista.

La cobertura se mide con **JaCoCo** y es **exigible**: `verify` falla si
baja del 85% de líneas o del 70% de ramas. Cobertura actual: **94,8% de
líneas y 82,1% de ramas**, con las capas Service (97,6%) y Controller (100%) que exige el enunciado por encima del resto; el reporte HTML queda en
`backend/target/site/jacoco/index.html`.

Cada push a `main` o `develop` ejecuta en **GitHub Actions**
([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) la construcción y
las pruebas del backend con JDK 17 y el lint y build del frontend.

## Servicios en la nube

No se integró un proveedor cloud real (no se dispone de credenciales para
esta prueba), pero el proyecto está preparado para desplegarse así:

- **Cómputo**: la imagen Docker del backend se publicaría en **Amazon ECR**
  y se ejecutaría en **ECS Fargate** o **App Runner**, detrás de un
  **Application Load Balancer**.
- **Base de datos**: **Amazon RDS para PostgreSQL**, sustituyendo el
  contenedor de `postgres` con solo cambiar `DB_URL` y las credenciales.
- **Configuración y secretos**: **AWS Secrets Manager** o **Parameter
  Store** para `DB_PASSWORD`, en lugar de variables en texto plano.
- **Frontend**: el build estático en **Amazon S3** + **CloudFront**.
- **Observabilidad**: logs hacia **CloudWatch Logs**.
- **CI/CD**: el pipeline de GitHub Actions ya existente se extendería para
  publicar la imagen en ECR y actualizar el servicio de ECS.

## Control de versiones

Flujo **git flow**:

- `main`: código listo para entrega, con tags de versión.
- `develop`: rama de integración.
- `feature/*`: una rama por entregable (`backend-setup`, `clientes`,
  `cuentas`, `transacciones`, `persistencia`, `pruebas`, `docker`,
  `frontend`, `documentacion`), fusionadas con `--no-ff` para conservar la
  traza de cómo avanzó el proyecto.
- `release/*`: estabiliza `develop` antes de fusionar a `main`.

```bash
git log --all --graph --oneline    # historial completo de ramas y merges
```

## Decisiones de diseño y alcance

Decisiones tomadas deliberadamente, con su justificación:

- **Las transacciones no se editan ni se eliminan.** Un libro de
  movimientos es inmutable por naturaleza contable: corregir un
  movimiento se hace con otro movimiento que lo compense, nunca borrando
  el original. Por eso el módulo expone creación y consulta, pero no
  `PUT` ni `DELETE`.
- **Las cuentas no se eliminan, se cancelan.** El borrado físico
  destruiría el historial de movimientos asociado; la cancelación es el
  equivalente funcional y es definitiva. Lo único modificable de una
  cuenta es la exención de GMF: el tipo, el número y el titular son
  inmutables por definición del producto, y el saldo solo cambia mediante
  transacciones.
- **Las cuentas corrientes admiten sobregiro y las de ahorro no.** El
  enunciado restringe explícitamente solo el saldo de las cuentas de
  ahorro. En un sistema real el sobregiro de una cuenta corriente estaría
  limitado por un cupo aprobado, concepto que no forma parte del alcance.
- **`saldoDisponible` coincide con `saldo`.** Se expone como campo propio
  para dejar explícita la distinción que pide el enunciado, pero ambos
  coinciden porque el alcance no contempla retenciones ni embargos, que
  serían lo único capaz de separarlos.
- **La mayoría de edad se valida en la capa de entrada**, no con una
  restricción de base de datos: PostgreSQL no admite funciones no
  deterministas como `CURRENT_DATE` en un `CHECK`, de modo que la
  alternativa sería un disparador, con un costo de mantenimiento que no se
  justifica frente a la validación declarativa ya existente.
- **No se admite la tarjeta de identidad como documento.** En Colombia la
  expide el registro civil para personas de 7 a 17 años, así que sería
  incompatible con la regla que exige que todo cliente sea mayor de edad:
  habría quedado en el modelo un valor imposible de usar.
- **El cliente no expone la colección de sus cuentas.** La relación se
  navega siempre desde la cuenta hacia su titular, y las consultas por
  cliente se resuelven en `CuentaRepository`. Un `@OneToMany` en `Cliente`
  habría sido una relación sin uso que además invita a cargar todas las
  cuentas cuando no hacen falta.
- **No se implementó autenticación.** El enunciado no la pide y añadirla
  sin un modelo de usuarios y roles definido sería una decisión
  arbitraria. En producción, la vía natural sería Spring Security con JWT
  o un proveedor de identidad corporativo.
