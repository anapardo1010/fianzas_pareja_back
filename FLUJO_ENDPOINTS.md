# Flujo de Endpoints - Sistema de Gestión Financiera
## Guía completa de uso de la API con autenticación JWT y Multi-tenant

---

## 📋 FLUJO COMPLETO PARA USUARIO NUEVO (SIMPLIFICADO)

### **PASO 1: Registrarse (Todo-en-Uno)**
**Endpoint:** `POST /api/auth/register`  
**Autenticación:** No requerida (endpoint público)  
**Descripción:** Un usuario nuevo se registra. El sistema automáticamente:
- ✅ Crea un tenant (organización) para él
- ✅ Lo registra como usuario ADMIN
- ✅ Retorna un token JWT (sesión iniciada automáticamente)

```json
POST /api/auth/register
Content-Type: application/json

{
  "email": "juan@email.com",
  "password": "MiPassword123!",
  "name": "Juan Pérez"
}
```

**Respuesta:**
```json
{
  "businessCode": "FZ_SC_200",
  "message": "Usuario registrado correctamente. Sesión iniciada.",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "tenantId": 1,
    "role": "ADMIN"
  }
}
```

**⚠️ IMPORTANTE:** El tenant se crea automáticamente con el nombre "Finanzas de Juan Pérez". Ya tienes tu token para usarlo inmediatamente.

---

### **PASO 2: Invitar a tu Pareja**
**Endpoint:** `POST /api/auth/register`  
**Autenticación:** Bearer Token requerido (opcional, puede ser público)  
**Descripción:** Como ADMIN, invitas a tu pareja proporcionando tu `tenantId`.

```json
POST /api/auth/register
Content-Type: application/json
Authorization: Bearer <tu-token-jwt>

{
  "email": "maria@email.com",
  "password": "OtroPassword456!",
  "name": "María López",
  "tenantId": 1
}
```

**Respuesta:**
```json
{
  "businessCode": "FZ_SC_200",
  "message": "Usuario registrado correctamente. Sesión iniciada.",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 2,
    "tenantId": 1,
    "role": "USER"
  }
}
```

**📝 Nota:** María automáticamente queda como USER del mismo tenant (tenant 1). Ahora ambos comparten las finanzas.

---

### **PASO 3: Configurar Categorías de Gastos**
**Endpoint:** `POST /api/categories`  
**Autenticación:** Bearer Token requerido  
**Descripción:** Crea categorías para organizar los gastos.

```json
POST /api/categories
Content-Type: application/json
Authorization: Bearer <tu-token-jwt>

{
  "name": "Alimentación",
  "description": "Supermercado, restaurantes",
  "tenantId": 1
}
```

**Respuesta:**
```json
{
  "businessCode": "FZ_SC_200",
  "message": "Categoría creada",
  "data": {
    "id": 1,
    "name": "Alimentación",
    "description": "Supermercado, restaurantes",
    "tenantId": 1
  }
}
```

**Repite este paso para crear más categorías:**
- Transporte
- Vivienda
- Entretenimiento
- Servicios
- Ahorro
- etc.

---

### **PASO 4: Configurar Métodos de Pago**
**Endpoint:** `POST /api/payment-methods`  
**Autenticación:** Bearer Token requerido  
**Descripción:** Registra las tarjetas, cuentas bancarias o métodos de pago.

```json
POST /api/payment-methods
Content-Type: application/json
Authorization: Bearer <tu-token-jwt>

{
  "name": "Tarjeta Visa Juan",
  "type": "CREDIT_CARD",
  "description": "Visa terminada en 1234",
  "tenantId": 1
}
```

**Respuesta:**
```json
{
  "businessCode": "FZ_SC_200",
  "message": "Método de pago creado",
  "data": {
    "id": 1,
    "name": "Tarjeta Visa Juan",
    "type": "CREDIT_CARD",
    "description": "Visa terminada en 1234",
    "tenantId": 1
  }
}
```

---

### **PASO 5: Registrar Transacciones**
**Endpoint:** `POST /api/transactions`  
**Autenticación:** Bearer Token requerido  
**Descripción:** Registra gastos o ingresos.

```json
POST /api/transactions
Content-Type: application/json
Authorization: Bearer <tu-token-jwt>

{
  "amount": 150.50,
  "description": "Compra en supermercado",
  "transactionType": "EXPENSE",
  "transactionDate": "2026-02-07",
  "categoryId": 1,
  "paymentMethodId": 1,
  "tenantId": 1,
  "userId": 1,
  "installments": 1
}
```

**Respuesta:**
```json
{
  "businessCode": "FZ_SC_200",
  "message": "Transacción creada",
  "data": {
    "id": 1,
    "amount": 150.50,
    "description": "Compra en supermercado",
    "transactionType": "EXPENSE",
    "transactionDate": "2026-02-07",
    "categoryId": 1,
    "paymentMethodId": 1,
    "tenantId": 1,
    "userId": 1
  }
}
```

---

### **PASO 6: Consultar Reportes Financieros**
**Endpoint:** `GET /api/reports/summary`  
**Autenticación:** Bearer Token requerido  
**Descripción:** Obtén un resumen de ingresos, gastos y balance.

```http
GET /api/reports/summary?tenantId=1&startDate=2026-02-01&endDate=2026-02-28
Authorization: Bearer <tu-token-jwt>
```

**Respuesta:**
```json
{
  "businessCode": "FZ_SC_200",
  "message": "Reporte generado exitosamente",
  "data": {
    "totalIncome": 5000.00,
    "totalExpenses": 3200.50,
    "balance": 1799.50,
    "period": {
      "startDate": "2026-02-01",
      "endDate": "2026-02-28"
    }
  }
}
```

---

## 🔄 FLUJO DIARIO DE USO

### Usuario Ya Registrado:

#### **1. Login diario**
```json
POST /api/auth/login
{
  "email": "juan@email.com",
  "password": "MiPassword123!"
}
```
→ Obtén el token JWT

#### **2. Registrar gasto del día**
```json
POST /api/transactions
Authorization: Bearer <token>
{
  "amount": 25.00,
  "description": "Almuerzo",
  "transactionType": "EXPENSE",
  "transactionDate": "2026-02-07",
  "categoryId": 1,
  "paymentMethodId": 1,
  "tenantId": 1,
  "userId": 1
}
```

#### **3. Consultar transacciones del día**
```http
GET /api/transactions?tenantId=1&startDate=2026-02-07&endDate=2026-02-07
Authorization: Bearer <token>
```

#### **4. Ver resumen del mes**
```http
GET /api/reports/summary?tenantId=1&startDate=2026-02-01&endDate=2026-02-28
Authorization: Bearer <token>
```

---

## 🔐 DIFERENCIAS ENTRE ROLES

### **ADMIN (Juan - el que se registró primero)**
✅ Puede ver todas las transacciones del tenant sin filtros  
✅ Puede acceder a `GET /api/transactions/findAll`  
✅ Puede crear categorías y métodos de pago  
✅ Puede ver reportes globales  
✅ Puede gestionar usuarios  
✅ Puede invitar a su pareja al tenant

### **USER (María - invitada al tenant)**
✅ Solo ve transacciones de su tenant (filtradas automáticamente)  
❌ No puede acceder a `findAll()` sin filtros  
✅ Puede crear sus propias transacciones  
✅ Puede ver reportes de su tenant  
✅ Puede consultar sus propios gastos  

---

## 📊 ENDPOINTS PRINCIPALES POR CATEGORÍA

### **Autenticación (Públicos)**
- `POST /api/auth/register` - Registrar usuario (crea tenant automáticamente si es nuevo)
- `POST /api/auth/login` - Iniciar sesión

### **Tenants**
- `GET /api/tenants` - Listar tenants (solo ADMIN global)
- `GET /api/tenants/{id}` - Obtener tenant por ID
- `PUT /api/tenants/{id}` - Actualizar tenant

### **Usuarios**
- `GET /api/users?tenantId=1` - Listar usuarios del tenant
- `GET /api/users/{id}` - Obtener usuario por ID
- `PUT /api/users/{id}` - Actualizar usuario
- `DELETE /api/users/{id}` - Desactivar usuario

### **Categorías**
- `POST /api/categories` - Crear categoría
- `GET /api/categories?tenantId=1` - Listar categorías del tenant
- `GET /api/categories/{id}` - Obtener categoría por ID
- `PUT /api/categories/{id}` - Actualizar categoría

### **Métodos de Pago**
- `POST /api/payment-methods` - Crear método de pago
- `GET /api/payment-methods?tenantId=1` - Listar métodos del tenant
- `GET /api/payment-methods/{id}` - Obtener método por ID
- `PUT /api/payment-methods/{id}` - Actualizar método

### **Transacciones**
- `POST /api/transactions` - Crear transacción
- `GET /api/transactions?tenantId=1` - Listar transacciones del tenant
- `GET /api/transactions/{id}` - Obtener transacción por ID
- `PUT /api/transactions/{id}` - Actualizar transacción
- `DELETE /api/transactions/{id}` - Eliminar transacción

### **Reportes**
- `GET /api/reports/summary` - Resumen financiero
- `GET /api/reports/by-category` - Gastos por categoría
- `GET /api/reports/by-payment-method` - Gastos por método de pago
- `GET /api/reports/by-user` - Gastos por usuario

---

## 🧪 PROBAR EN SWAGGER

1. Abre: `http://localhost:8080/swagger-ui.html`
2. Ejecuta `POST /api/auth/register` (solo con email, password y name)
3. Copia el token de la respuesta
4. Haz clic en el botón **"Authorize"** 🔒 (arriba a la derecha)
5. Ingresa: `Bearer <tu-token-copiado>`
6. Haz clic en **"Authorize"**
7. Ahora todos los endpoints incluirán el token automáticamente

---

## ❌ MANEJO DE ERRORES

### **Error: Credenciales inválidas**
```json
{
  "businessCode": "FZ_AU_401",
  "message": "Credenciales inválidas",
  "data": null
}
```

### **Error: Email ya registrado**
```json
{
  "businessCode": "FZ_AU_409",
  "message": "El correo ya está registrado",
  "data": null
}
```

### **Error: Token inválido o expirado**
```json
{
  "businessCode": "FZ_SC_401",
  "message": "Token inválido o expirado",
  "data": null
}
```

### **Error: Acceso denegado (rol insuficiente)**
```json
{
  "businessCode": "FZ_SC_403",
  "message": "No tienes permisos para acceder a este recurso",
  "data": null
}
```

---

## 🔑 NOTAS IMPORTANTES

1. **Registro simplificado** - Solo necesitas email, password y nombre. El tenant se crea automáticamente.

2. **El token JWT expira en 24 horas** - Debes hacer login nuevamente después de ese tiempo.

3. **Multi-tenant automático** - Cuando un USER hace login, solo verá datos de su tenant. No necesitas filtrar manualmente en cada request.

4. **Seguridad de passwords** - Las contraseñas se encriptan automáticamente con BCrypt antes de guardarse.

5. **JWT Claims** - El token contiene:
   - `userId`: ID del usuario autenticado
   - `tenantId`: ID del tenant al que pertenece
   - `role`: ADMIN o USER

6. **Invitar a tu pareja** - Usa el mismo endpoint de registro pero proporciona tu `tenantId` y ella será USER automáticamente.

7. **Swagger UI incluye Bearer Auth** - Puedes probar todos los endpoints protegidos desde Swagger.

---

## 📝 EJEMPLO COMPLETO EN CURL

```bash
# 1. Registrarse (crea tenant automáticamente)
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"juan@email.com","password":"Pass123!","name":"Juan Pérez"}'

# Respuesta incluye el token JWT
# Guarda el token y el tenantId de la respuesta
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
TENANT_ID=1

# 2. Invitar a tu pareja
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"email":"maria@email.com","password":"Pass456!","name":"María López","tenantId":'$TENANT_ID'}'

# 3. Crear categoría (con token)
curl -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Alimentación","tenantId":'$TENANT_ID'}'

# 4. Crear método de pago (con token)
curl -X POST http://localhost:8080/api/payment-methods \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Tarjeta Visa","type":"CREDIT_CARD","tenantId":'$TENANT_ID'}'

# 5. Crear transacción (con token)
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount":150.50,"description":"Supermercado","transactionType":"EXPENSE","transactionDate":"2026-02-07","categoryId":1,"paymentMethodId":1,"tenantId":'$TENANT_ID',"userId":1}'

# 6. Consultar transacciones (con token)
curl -X GET "http://localhost:8080/api/transactions?tenantId=$TENANT_ID" \
  -H "Authorization: Bearer $TOKEN"

# 7. Ver reportes (con token)
curl -X GET "http://localhost:8080/api/reports/summary?tenantId=$TENANT_ID&startDate=2026-02-01&endDate=2026-02-28" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🎯 RESUMEN DEL FLUJO INICIAL (SIMPLIFICADO)

```
1. POST /api/auth/register         → Te registras (tenant se crea automáticamente, eres ADMIN, obtienes token)
2. POST /api/auth/register         → Invitas a tu pareja (con tu tenantId, ella es USER)
3. POST /api/categories            → Crear categorías de gastos
4. POST /api/payment-methods       → Crear métodos de pago
5. POST /api/transactions          → Registrar transacciones
6. GET /api/reports/summary        → Ver reportes financieros
```

**¡Solo 2 pasos para empezar en lugar de 4!** 🎉

---

## 🚀 FLUJO VISUAL SIMPLIFICADO

### **Para el Usuario Nuevo (Juan):**
```
1. Clic en "Registrarme"
   ├─ Ingresa: email, password, nombre
   └─ ✅ Se crea automáticamente:
       ├─ Tenant: "Finanzas de Juan Pérez"
       ├─ Usuario: Juan como ADMIN
       └─ Token JWT (ya está logueado)

2. Va a "Invitar a mi pareja"
   ├─ Ingresa: email y nombre de María
   └─ ✅ María recibe invitación o se registra con el tenantId de Juan

3. Configuran juntos:
   ├─ Categorías
   ├─ Métodos de pago
   └─ Empiezan a registrar gastos
```

### **Para la Pareja (María):**
```
1. Recibe invitación o link con tenantId
2. Clic en "Registrarme"
   ├─ Ingresa: email, password, nombre, tenantId de Juan
   └─ ✅ Se une al tenant de Juan como USER

3. Ya puede:
   ├─ Ver transacciones del tenant
   ├─ Registrar sus propios gastos
   └─ Ver reportes compartidos
```

---

**Fecha de creación:** 07 de Febrero de 2026  
**Versión de la API:** 1.0  
**Autor:** Sistema de Gestión Financiera
