# 📚 Acceso a Swagger en Render

## 🔗 URLs de Acceso

### En Producción (Render)
- **Swagger UI**: `https://finanzas-pareja-back.onrender.com/swagger-ui.html`
- **API Docs (JSON)**: `https://finanzas-pareja-back.onrender.com/v3/api-docs`

### En Local
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **API Docs (JSON)**: `http://localhost:8080/v3/api-docs`

## ✅ Cambios Realizados para Solucionar CORS y Swagger

### 1. **Eliminación de Configuraciones CORS Duplicadas**
   - ❌ `SimpleCorsFilter.java` → Comentado (conflictuaba)
   - ❌ `CorsConfig.java` → Comentado (duplicado)
   - ✅ Solo queda la configuración en `SecurityConfig.java`

### 2. **Configuración de Springdoc en `application.yml`**
```yaml
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
```

### 3. **Permisos en SecurityConfig**
Los siguientes endpoints están **sin autenticación**:
- `/v3/api-docs/**` - Documentación de API
- `/swagger-ui/**` - Interfaz de Swagger
- `/swagger-ui.html` - Página principal de Swagger
- `/api/v1/auth/**` - Endpoints de autenticación

## 🚀 Despliegue en Render

### Variables de Entorno Necesarias
Asegúrate de tener estas variables configuradas en Render:

```bash
CORS_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:4201,https://finanzas-pareja-front.onrender.com
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:<port>/<database>
SPRING_DATASOURCE_USERNAME=<usuario>
SPRING_DATASOURCE_PASSWORD=<contraseña>
JWT_SECRET=<tu-clave-secreta>
```

### Pasos para Desplegar
1. Hacer commit de los cambios:
   ```bash
   git add .
   git commit -m "fix: Corregir CORS y habilitar Swagger en Render"
   git push origin main
   ```

2. Render detectará los cambios y redesployará automáticamente

3. Esperar a que el deploy termine (puede tomar 2-5 minutos)

4. Acceder a: `https://finanzas-pareja-back.onrender.com/swagger-ui.html`

## 🔍 Verificación

### Verificar que Swagger está funcionando:
```bash
# API Docs
curl https://finanzas-pareja-back.onrender.com/v3/api-docs

# Swagger UI (debe devolver HTML)
curl https://finanzas-pareja-back.onrender.com/swagger-ui.html
```

### Verificar que CORS está funcionando:
```bash
curl -X OPTIONS \
  -H "Origin: https://finanzas-pareja-front.onrender.com" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type" \
  -v \
  https://finanzas-pareja-back.onrender.com/api/v1/auth/register
```

Deberías ver en la respuesta:
- `Access-Control-Allow-Origin: https://finanzas-pareja-front.onrender.com`
- `Access-Control-Allow-Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS`

## ⚠️ Problemas Comunes

### 1. Swagger no carga en Render
**Solución**: Verificar en los logs de Render que la aplicación inició correctamente:
```
✅ CORS: Origen permitido -> https://finanzas-pareja-front.onrender.com
🔧 Configurando SecurityFilterChain...
✅ SecurityFilterChain configurado correctamente
```

### 2. CORS sigue bloqueado
**Solución**: Verificar que la variable `CORS_ALLOWED_ORIGINS` en Render incluya tu frontend:
- `https://finanzas-pareja-front.onrender.com`

### 3. 401 en Swagger
**Solución**: Swagger está correctamente configurado como público. Si aparece 401, es porque el endpoint requiere autenticación (usar el botón "Authorize" en Swagger).

## 📝 Notas Importantes

1. **Solo hay UNA configuración de CORS** (en SecurityConfig)
2. **Swagger NO requiere autenticación** para acceder
3. **OPTIONS siempre se permite** sin autenticación (preflight CORS)
4. Los **endpoints /api/v1/auth/** son públicos

