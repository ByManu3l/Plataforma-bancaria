# Prueba Técnica – Plataforma Bancaria

API REST para que los trabajadores de una entidad financiera administren
clientes, productos financieros (cuentas de ahorro y corriente) y las
transacciones (consignaciones, retiros y transferencias) sobre esos
productos. Incluye un frontend simple para operar la API desde el
navegador.

## Índice

- [Arquitectura](#arquitectura)
- [Patrones de diseño](#patrones-de-diseño)
- [Principios SOLID](#principios-solid)
- [Principio ACID](#principio-acid)
- [Modelo de datos (DDL/DML)](#modelo-de-datos-ddldml)
- [Reglas de negocio](#reglas-de-negocio)
- [Cómo ejecutar el proyecto](#cómo-ejecutar-el-proyecto)
- [Documentación de la API](#documentación-de-la-api)
- [Tests](#tests)
- [Servicios en la nube](#servicios-en-la-nube)
- [Control de versiones](#control-de-versiones)

## Arquitectura

Backend en **Java 17 + Spring Boot 3.3**, organizado por **capas dentro de
cada módulo de dominio** (`cliente`, `cuenta`, `transaccion`): `entity` →
`repository` → `service` (con interfaz + implementación) → `controller`,
más `dto` y `mapper` (MapStruct) para no exponer las entidades JPA
directamente en la API.

El enunciado permite explícitamente "hexagonal en lo posible **o MVC**".
Se optó por esta variante por capas (equivalente a MVC: Modelo = entidades
JPA, Vista = DTOs/JSON de la API REST, Controlador = `@RestController`)
en lugar de hexagonal puro (puertos y adaptadores con el dominio
completamente aislado de JPA) porque:

- El dominio de este proyecto es CRUD-céntrico con reglas de negocio
  acotadas; el desacople puertos/adaptadores agrega una capa de
  indirección (interfaces de repositorio + adaptadores + mapeadores
  dominio↔persistencia adicionales) sin un beneficio claro a este tamaño.
- Ya se logra el objetivo principal de la arquitectura hexagonal —que la
  lógica de negocio no dependa de detalles de framework— con la
  combinación de: `Service` como interfaz (los controladores dependen de
  la abstracción, no de la implementación), DTOs que aíslan el contrato
  HTTP del modelo de persistencia, y clases de dominio puras como
  `NumeroCuentaGenerator` o `OperacionCuenta` (crédito/débito) que no
  dependen de Spring MVC ni de Jakarta Persistence en su lógica.

Cada módulo (`cliente/`, `cuenta/`, `transaccion/`) es una **vertical
slice**: agrupa su entidad, repositorio, servicio, controlador y DTOs en
un mismo paquete, en lugar de paquetes horizontales
(`entities/`, `services/`, `controllers/`) a nivel de toda la aplicación.
Esto facilita ubicar todo lo relacionado con "Cuentas" en un solo lugar y
escala mejor a medida que crecen los módulos.

```
backend/src/main/java/com/bancaria/prueba/
├── cliente/          # entity, repository, service (+impl), controller, mapper, dto/
├── cuenta/            # ídem + NumeroCuentaGenerator
├── transaccion/       # ídem + operacion/ (patrón Strategy)
├── common/
│   ├── exception/     # excepciones de negocio + manejador global
│   └── validation/    # validador @MinAge
└── config/             # OpenAPI (Swagger) y CORS
```

## Patrones de diseño

| Patrón | Dónde | Para qué |
|---|---|---|
| **Strategy** | `transaccion.operacion.OperacionCuenta` con `CreditoOperacion`/`DebitoOperacion` | Encapsula el efecto de un movimiento sobre el saldo. Consignación y el lado receptor de una transferencia reutilizan `CreditoOperacion`; retiro y el lado emisor de una transferencia reutilizan `DebitoOperacion`, sin condicionales repetidos en el servicio. |
| **Repository** | `ClienteRepository`, `CuentaRepository`, `MovimientoRepository` (Spring Data JPA) | Aísla el acceso a datos detrás de una interfaz. |
| **DTO + Mapper** | `*Request`/`*Response` (records) + interfaces MapStruct | Evita exponer las entidades JPA en la API y desacopla el contrato HTTP del modelo de persistencia. |
| **Builder** | Entidades (`Cliente`, `Cuenta`, `Movimiento`) vía Lombok `@Builder` | Construcción legible de objetos con varios campos opcionales. |
| **Factory/Generator** | `NumeroCuentaGenerator` | Aísla y hace testeable en forma independiente la generación de números de cuenta únicos con el prefijo de cada tipo. |
| **Chain of Responsibility / Handler centralizado** | `GlobalExceptionHandler` (`@RestControllerAdvice`) | Traduce excepciones de negocio y de validación a respuestas HTTP consistentes en un único lugar, evitando duplicar manejo de errores en cada controlador. |
| **Inyección de dependencias** | Constructores generados con Lombok `@RequiredArgsConstructor` | Los servicios dependen de abstracciones (interfaces de repositorio/mapper) inyectadas por Spring, no de implementaciones concretas. |

## Principios SOLID

- **S – Responsabilidad única**: cada clase tiene un motivo de cambio.
  `NumeroCuentaGenerator` solo genera números de cuenta;
  `MinAgeValidator` solo valida mayoría de edad; `ClienteMapper` solo
  convierte entre `Cliente` y sus DTOs.
- **O – Abierto/cerrado**: `OperacionCuenta` permite agregar un nuevo
  efecto sobre el saldo (por ejemplo, un futuro "bloqueo de fondos")
  creando una nueva implementación, sin modificar
  `TransaccionServiceImpl`.
- **L – Sustitución de Liskov**: `ClienteServiceImpl`, `CuentaServiceImpl`
  y `TransaccionServiceImpl` son sustituibles por cualquier otra
  implementación de sus interfaces (`ClienteService`, `CuentaService`,
  `TransaccionService`) sin romper a quienes las consumen (los
  controladores).
- **I – Segregación de interfaces**: cada módulo expone su propia interfaz
  de servicio con solo las operaciones que le competen, en lugar de una
  interfaz genérica compartida.
- **D – Inversión de dependencias**: los controladores dependen de las
  interfaces de servicio (no de `*ServiceImpl`), y los servicios dependen
  de las interfaces de repositorio de Spring Data, no de una tecnología
  de persistencia concreta.

## Principio ACID

La operación más sensible del dominio es la **transferencia entre
cuentas** (`TransaccionServiceImpl.transferir`): debita la cuenta origen,
acredita la cuenta destino y registra dos movimientos, todo dentro de un
único método anotado `@Transactional`.

- **Atomicidad**: si falla cualquier paso (por ejemplo, la cuenta destino
  quedó inactiva justo antes), Spring revierte la transacción completa —
  no puede quedar una cuenta debitada sin su correspondiente crédito.
- **Consistencia**: las reglas de negocio (saldo mínimo en ahorros, cuenta
  debe estar activa, cuentas existentes) se validan antes de tocar el
  saldo, y además hay un `CHECK` a nivel de base de datos
  (`ck_cuentas_saldo_ahorros_no_negativo`) como segunda barrera.
- **Aislamiento**: el nivel por defecto de Spring/JPA sobre PostgreSQL
  (`READ COMMITTED`) evita que una transacción vea escrituras a medio
  confirmar de otra.
- **Durabilidad**: una vez confirmada la transacción, PostgreSQL garantiza
  que los cambios persisten (WAL) incluso ante una caída del proceso.

## Modelo de datos (DDL/DML)

El esquema se versiona con **Flyway** en
[`backend/src/main/resources/db/migration`](backend/src/main/resources/db/migration):

- [`V1__crear_tablas.sql`](backend/src/main/resources/db/migration/V1__crear_tablas.sql)
  (DDL): tablas `clientes`, `cuentas` y `movimientos`, con `FOREIGN KEY`,
  `UNIQUE` (identificación/correo/número de cuenta) y `CHECK` de dominio
  (tipo de cuenta, estado, tipo de movimiento, saldo no negativo en
  ahorros).
- [`V2__datos_semilla.sql`](backend/src/main/resources/db/migration/V2__datos_semilla.sql)
  (DML): datos de ejemplo para poder probar la API/el frontend
  inmediatamente después de levantar el proyecto.

Ejemplos de DML que ejecuta la aplicación en tiempo de ejecución (vía
Hibernate, a partir de las operaciones de la API):

```sql
-- Al crear un cliente
INSERT INTO clientes (id, tipo_identificacion, numero_identificacion, nombres, apellido,
                       correo_electronico, fecha_nacimiento, fecha_creacion)
VALUES (?, ?, ?, ?, ?, ?, ?, ?);

-- Al consignar (actualiza saldo + inserta el movimiento)
UPDATE cuentas SET saldo = ?, fecha_modificacion = ? WHERE id = ?;
INSERT INTO movimientos (id, cuenta_id, tipo_movimiento, valor, saldo_disponible, fecha, descripcion)
VALUES (?, ?, 'CONSIGNACION', ?, ?, ?, ?);

-- Al consultar el estado de cuenta
SELECT * FROM movimientos WHERE cuenta_id = ? ORDER BY fecha DESC;
```

## Reglas de negocio

**Clientes**
- No se puede crear ni existe en base de datos un cliente menor de edad
  (`@MinAge(18)` + `CHECK` implícito por validación en la capa de
  entrada).
- No se puede eliminar un cliente con cuentas vinculadas.
- `fechaCreacion`/`fechaModificacion` se calculan automáticamente
  (`@PrePersist`/`@PreUpdate`).
- Correo con formato válido, nombres/apellido de mínimo 2 caracteres,
  identificación y correo únicos.

**Cuentas**
- Solo dos tipos: `AHORROS` o `CORRIENTE`; solo existen vinculadas a un
  cliente.
- Número de cuenta de 10 dígitos, único, autogenerado con prefijo `53`
  (ahorros) o `33` (corriente).
- Las cuentas de ahorro nunca quedan con saldo negativo.
- Se crean activas por defecto; se pueden activar/inactivar en cualquier
  momento; solo se cancelan con saldo exactamente `$0`.

**Transacciones**
- Solo tres tipos: consignación, retiro, transferencia.
- Cada transacción actualiza el saldo (y el saldo disponible, registrado
  como snapshot en cada movimiento).
- Las transferencias solo operan entre cuentas existentes y activas, y
  generan un movimiento de crédito en la cuenta receptora y uno de débito
  en la cuenta emisora.

## Cómo ejecutar el proyecto

### Con Docker (recomendado)

```bash
docker compose up --build
```

Levanta PostgreSQL, el backend (`http://localhost:8080`) y el frontend
(`http://localhost:5173`). Las migraciones Flyway (incluyendo los datos de
ejemplo) se aplican automáticamente al arrancar el backend.

### En local, sin Docker

```bash
# Backend (requiere JDK 17 y un PostgreSQL corriendo en localhost:5432,
# ver backend/src/main/resources/application.yml para las variables)
cd backend
mvn spring-boot:run

# Frontend
cd frontend
npm install
npm run dev
```

## Documentación de la API

Con el backend corriendo: Swagger UI en
`http://localhost:8080/swagger-ui.html` (spec OpenAPI en
`/v3/api-docs`), configurado en
[`OpenApiConfig`](backend/src/main/java/com/bancaria/prueba/config/OpenApiConfig.java).

## Tests

```bash
cd backend
mvn test
```

- **Unitarios** (JUnit 5 + Mockito): capa `Service` de los tres módulos y
  clases de dominio (`NumeroCuentaGenerator`, `MinAgeValidator`,
  `CreditoOperacion`/`DebitoOperacion`), con dependencias mockeadas.
- **De componentes**: capa `Controller` con `@WebMvcTest` + MockMvc
  (contexto Spring real, servicio mockeado) y una prueba de extremo a
  extremo con `@SpringBootTest` + `@AutoConfigureMockMvc` que ejercita
  Controller→Service→Repository→base de datos real (H2, con el mismo
  esquema Flyway) para un flujo completo de cliente→cuentas→transacciones.
- **Cobertura**: `jacoco-maven-plugin` genera el reporte en
  `backend/target/site/jacoco/index.html` tras correr `mvn test`.

## Servicios en la nube

No se integró un proveedor cloud real (sin credenciales disponibles para
esta prueba), pero el proyecto está listo para desplegarse así:

- **Cómputo**: la imagen Docker del backend (`backend/Dockerfile`) se
  publicaría en **Amazon ECR** y se ejecutaría en **ECS Fargate** o
  **App Runner** (sin gestionar servidores), detrás de un
  **Application Load Balancer**.
- **Base de datos**: **Amazon RDS para PostgreSQL** (gestionado, con
  backups automáticos), reemplazando el contenedor de `postgres` de
  `docker-compose.yml` solo cambiando `DB_URL`/credenciales.
- **Configuración y secretos**: **AWS Secrets Manager** o
  **Parameter Store** para `DB_PASSWORD` y demás variables sensibles, en
  vez de pasarlas por entorno en texto plano.
- **Frontend**: el build estático (`frontend/dist`) en **Amazon S3** +
  **CloudFront** como CDN, en vez de servirlo con nginx en un contenedor.
- **Observabilidad**: logs de la aplicación hacia **CloudWatch Logs**.
- **CI/CD**: pipeline en **GitHub Actions** que en cada push a `main`
  compile, corra los tests (`mvn test`), construya y publique la imagen
  en ECR, y actualice el servicio de ECS.

## Control de versiones

Se usó un flujo tipo **git flow**:

- `main`: código listo para entrega/producción.
- `develop`: rama de integración de las features ya completas.
- `feature/*`: una rama por módulo o entregable
  (`feature/backend-setup`, `feature/clientes`, `feature/cuentas`,
  `feature/transacciones`, `feature/persistencia`, `feature/pruebas`,
  `feature/docker`, `feature/frontend`, `feature/documentacion`), cada una
  mergeada a `develop` con `--no-ff` para conservar el historial de cómo
  avanzó el proyecto.
- `release/*`: estabiliza `develop` antes de fusionarlo a `main` con un
  tag de versión.

```bash
git log --all --graph --oneline   # ver el historial completo de ramas y merges
```
