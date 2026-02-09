# ParejaFinanzas - Sistema de Gestión Financiera

Sistema de gestión financiera para parejas desarrollado con Spring Boot, que permite administrar transacciones, cuotas, métodos de pago, categorías y usuarios en un entorno multi-tenant.

## 📋 Tabla de Contenidos

- [Características](#características)
- [Requisitos](#requisitos)
- [Instalación](#instalación)
- [Configuración](#configuración)
- [Ejecución](#ejecución)
- [Arquitectura](#arquitectura)
- [API Endpoints](#api-endpoints)
- [Modelos de Datos](#modelos-de-datos)
- [Solución de Problemas](#solución-de-problemas)
- [Tecnologías](#tecnologías)

## ✨ Características

- **Multi-tenant**: Soporte para múltiples parejas/usuarios
- **Gestión de Transacciones**: Registro de ingresos y gastos
- **Cuotas/Instalamentos**: Manejo de pagos en cuotas
- **Categorías**: Organización de transacciones por categorías
- **Métodos de Pago**: Gestión de diferentes métodos de pago
- **Reportes Financieros**: Generación de reportes y estadísticas
- **API RESTful**: Endpoints documentados con Swagger/OpenAPI
- **Validación**: Validación de datos con Jakarta Validation

## 🔧 Requisitos

- **Java**: JDK 21 o superior
- **Maven**: 3.6+ 
- **PostgreSQL**: 12 o superior
- **Git**: Para control de versiones

## 📥 Instalación

### 1. Clonar el repositorio

```bash
git clone <url-del-repositorio>
cd finanzas
```

### 2. Configurar la base de datos

Crear una base de datos PostgreSQL:

```sql
CREATE DATABASE pareja_finanzas;
```

### 3. Configurar credenciales

Editar el archivo `src/main/resources/application.yml` con tus credenciales:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pareja_finanzas
    username: tu_usuario
    password: tu_password
```

### 4. Compilar el proyecto

```bash
mvn clean install
```

## ⚙️ Configuración

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pareja_finanzas
    username: postgres
    password: tu_password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update  # usa 'validate' en producción
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

## 🚀 Ejecución

### Desarrollo

```bash
mvn spring-boot:run
```

### Producción

```bash
mvn clean package -DskipTests
java -jar target/app-0.0.1-SNAPSHOT.jar
```

### Ejecutar tests

```bash
mvn test
```

La aplicación estará disponible en: `http://localhost:8080`

Documentación Swagger: `http://localhost:8080/swagger-ui.html`

## 🏗️ Arquitectura

### Estructura del Proyecto

```
src/main/java/org/example/app/
├── AppApplication.java           # Punto de entrada
├── domain/
│   ├── entity/                   # Entidades JPA
│   │   ├── Category.java
│   │   ├── Installment.java
│   │   ├── PaymentMethod.java
│   │   ├── Tenant.java
│   │   ├── Transaction.java
│   │   └── User.java
│   └── repository/               # Repositorios Spring Data
│   │   ├── CategoryRepository.java
│   │   ├── InstallmentRepository.java
│   │   ├── PaymentMethodRepository.java
│   │   ├── TenantRepository.java
│   │   ├── TransactionRepository.java
│   │   └── UserRepository.java
├── facade/                       # Capa de acceso a datos
│   ├── CategoryFacade.java
│   ├── InstallmentFacade.java
│   ├── PaymentMethodFacade.java
│   ├── TenantFacade.java
│   ├── TransactionFacade.java
│   └── UserFacade.java
├── service/                      # Lógica de negocio
│   ├── CategoryService.java
│   ├── FinanceReportService.java
│   ├── PaymentMethodService.java
│   ├── TenantService.java
│   ├── TransactionService.java
│   └── UserService.java
├── web/
│   ├── controller/               # Controladores REST
│   │   ├── CategoryController.java
│   │   ├── FinanceReportController.java
│   │   ├── PaymentMethodController.java
│   │   ├── TenantController.java
│   │   ├── TransactionController.java
│   │   └── UserController.java
│   └── model/                    # DTOs y modelos de request/response
└── util/                         # Utilidades
```

### Capas de la Aplicación

1. **Controller Layer**: Maneja las peticiones HTTP y respuestas
2. **Service Layer**: Contiene la lógica de negocio
3. **Facade Layer**: Abstracción sobre los repositorios
4. **Repository Layer**: Acceso a datos con Spring Data JPA
5. **Entity Layer**: Modelos de dominio (entidades JPA)

## 📡 API Endpoints

### Tenants

- `POST /api/tenants` - Crear tenant
- `GET /api/tenants/{id}` - Obtener tenant por ID
- `GET /api/tenants` - Listar todos los tenants
- `PUT /api/tenants/{id}` - Actualizar tenant
- `DELETE /api/tenants/{id}` - Eliminar tenant

### Usuarios

- `POST /api/users` - Crear usuario
- `GET /api/users/{id}` - Obtener usuario por ID
- `GET /api/users/tenant/{tenantId}` - Listar usuarios por tenant
- `PUT /api/users/{id}` - Actualizar usuario
- `DELETE /api/users/{id}` - Eliminar usuario

### Categorías

- `POST /api/categories` - Crear categoría
- `GET /api/categories/{id}` - Obtener categoría por ID
- `GET /api/categories/tenant/{tenantId}` - Listar categorías por tenant
- `PUT /api/categories/{id}` - Actualizar categoría
- `DELETE /api/categories/{id}` - Eliminar categoría

### Métodos de Pago

- `POST /api/payment-methods` - Crear método de pago
- `GET /api/payment-methods/{id}` - Obtener método de pago por ID
- `GET /api/payment-methods/tenant/{tenantId}` - Listar métodos de pago por tenant
- `PUT /api/payment-methods/{id}` - Actualizar método de pago
- `DELETE /api/payment-methods/{id}` - Eliminar método de pago

### Transacciones

- `POST /api/transactions` - Crear transacción
- `GET /api/transactions/{id}` - Obtener transacción por ID
- `GET /api/transactions/tenant/{tenantId}` - Listar transacciones por tenant
- `GET /api/transactions/shared/{tenantId}` - Listar transacciones compartidas
- `PUT /api/transactions/{id}` - Actualizar transacción
- `DELETE /api/transactions/{id}` - Eliminar transacción

### Reportes Financieros

- `GET /api/reports/summary/{tenantId}` - Resumen financiero
- `GET /api/reports/by-category/{tenantId}` - Reporte por categorías
- `GET /api/reports/by-payment-method/{tenantId}` - Reporte por métodos de pago

## 📊 Modelos de Datos

### Tenant
Representa una pareja o grupo de usuarios que comparten finanzas.

```java
{
  "id": Long,
  "name": String,
  "isActive": Boolean,
  "createdAt": LocalDateTime,
  "updatedAt": LocalDateTime
}
```

### User
Usuario asociado a un tenant.

```java
{
  "id": Long,
  "name": String,
  "email": String,
  "tenantId": Long,
  "isActive": Boolean,
  "createdAt": LocalDateTime
}
```

### Category
Categoría para clasificar transacciones.

```java
{
  "id": Long,
  "name": String,
  "description": String,
  "tenantId": Long,
  "isActive": Boolean
}
```

### PaymentMethod
Método de pago utilizado en transacciones.

```java
{
  "id": Long,
  "name": String,
  "type": String,
  "tenantId": Long,
  "isActive": Boolean
}
```

### Transaction
Transacción financiera (ingreso o gasto).

```java
{
  "id": Long,
  "description": String,
  "amount": BigDecimal,
  "date": LocalDate,
  "type": String, // "INCOME" o "EXPENSE"
  "categoryId": Long,
  "paymentMethodId": Long,
  "userId": Long,
  "tenantId": Long,
  "isShared": Boolean,
  "totalInstallments": Integer,
  "installments": List<Installment>
}
```

### Installment
Cuota de una transacción dividida en pagos.

```java
{
  "id": Long,
  "transactionId": Long,
  "installmentNumber": Integer,
  "amount": BigDecimal,
  "dueDate": LocalDate,
  "isPaid": Boolean,
  "paidDate": LocalDate
}
```

## 🐛 Solución de Problemas

### Error: No property 'transactionDate' found for type 'Transaction'

**Causa**: Los métodos del repositorio usan nombres de propiedades que no existen en la entidad.

**Solución**: Asegúrate de que los métodos del repositorio usen los nombres exactos de las propiedades de la entidad. Por ejemplo, si la propiedad es `date` y no `transactionDate`:

```java
// ❌ Incorrecto
List<Transaction> findByTenantIdAndTransactionDateBetween(Long tenantId, LocalDate start, LocalDate end);

// ✅ Correcto
List<Transaction> findByTenantIdAndDateBetween(Long tenantId, LocalDate start, LocalDate end);
```

### Error: Cannot find symbol method

**Causa**: Falta declarar métodos en las interfaces Facade o Service.

**Solución**: Asegúrate de que todos los métodos públicos estén declarados en las interfaces correspondientes.

### Error de compilación después de cambios

**Solución**: Limpiar y recompilar el proyecto:

```bash
mvn clean install -U
```

### La aplicación no inicia

1. Verificar que PostgreSQL esté corriendo
2. Verificar credenciales en `application.yml`
3. Verificar que la base de datos exista
4. Revisar logs en la consola

### Errores de validación

Asegúrate de que los DTOs incluyan las anotaciones de validación necesarias:

```java
@NotNull
@NotBlank
@Size(min = 1, max = 100)
```

## 🛠️ Tecnologías

- **Spring Boot 3.2.2**: Framework principal
- **Spring Data JPA**: Persistencia de datos
- **PostgreSQL**: Base de datos relacional
- **Hibernate**: ORM
- **Lombok**: Reducción de código boilerplate
- **Jakarta Validation**: Validación de datos
- **SpringDoc OpenAPI**: Documentación de API
- **Maven**: Gestión de dependencias y build

## 📝 Buenas Prácticas

1. **Nombres consistentes**: Mantén los nombres de propiedades en entidades sincronizados con métodos de repositorio
2. **Validación**: Siempre valida los datos de entrada
3. **Transacciones**: Usa `@Transactional` en métodos de servicio que modifican datos
4. **DTOs**: Usa modelos separados para request/response (no expongas entidades directamente)
5. **Paginación**: Implementa paginación para listados grandes
6. **Manejo de errores**: Implementa manejo centralizado de excepciones
7. **Tests**: Escribe tests unitarios y de integración

## 🔐 Seguridad

⚠️ **Importante**: Este proyecto es una versión de desarrollo. Para producción considera:

- Implementar autenticación (Spring Security + JWT)
- Encriptar passwords
- Usar HTTPS
- Validar permisos por tenant
- Implementar rate limiting
- Sanitizar inputs
- Usar variables de entorno para credenciales

## 📄 Licencia

Este proyecto es de uso privado.

## 👥 Contribución

Para contribuir al proyecto:

1. Fork el repositorio
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📞 Contacto

Ana Gabriela Pardo - [@anapardo1010](https://github.com/anapardo1010)

---

**Última actualización**: Febrero 2026

