# Guía de Despliegue en Coolify — FamilyGuard 2.0 🚀

Esta guía detalla cómo desplegar **FamilyGuard 2.0** en tu servidor VPS mediante Coolify de forma **100% aislada e independiente**, sin interferir en los servicios existentes (como Sensei o la versión anterior).

---

## 1. Principios de Aislamiento y Seguridad

1. **Stack Independiente:** En Coolify crearemos un nuevo recurso tipo **Docker Compose** o **Application** llamado `familyguard-v2`.
2. **Red Privada:** PostgreSQL 16 y Redis 7 se ejecutan con `internal: true`. Sus puertos (`5432` y `6379`) **NO** están abiertos a internet, protegiendo los datos contra ataques externos.
3. **Persistencia Total:** Los datos de la base de datos se almacenan en el volumen nombrado `fg2_prod_postgres_data`. Ningún redespliegue ni reinicio borrará clientes, políticas ni hijos.
4. **Cero Conflicto con Sensei:** Los nombres de contenedores (`fg2_backend`, `fg2_postgres`, `fg2_redis`) y las redes privadas (`fg2-private-net`) están prefijados para no colisionar con otros contenedores del VPS.

---

## 2. Pasos para Configurar en Coolify

### Paso 1: Crear nuevo Recurso en Coolify
1. En tu panel de Coolify, entra a tu Proyecto / Entorno deseado.
2. Haz clic en **+ New Resource** -> **Docker Compose**.
3. Selecciona tu repositorio de GitHub: `https://github.com/DiegoAzz/segurity.git`.
4. En la configuración del repositorio:
   - **Branch:** `familyguard-v2` (¡Importante: NO seleccionar `main`!).
   - **Base Directory:** `/familyguard-v2`
   - **Compose File Path:** `infrastructure/coolify/docker-compose.coolify.yml`

### Paso 2: Configurar Variables de Entorno en Coolify
En la pestaña **Environment Variables** de tu nuevo stack, define:
```env
NODE_ENV=production
JWT_SECRET=genera_una_clave_secreta_aleatoria_de_al_menos_32_caracteres
POSTGRES_USER=fg_admin
POSTGRES_PASSWORD=tu_contrasena_segura_de_postgres
POSTGRES_DB=familyguard_prod
```

### Paso 3: Asignar Dominios / Subdominios con SSL Automático
En la configuración del servicio `familyguard-backend`:
- **FQDN / Dominio API:** `https://api-v2.tudominio.com` (o el subdominio que elijas)

En la configuración del servicio `familyguard-parent-web`:
- **FQDN / Dominio Panel:** `https://panel-v2.tudominio.com` (o el subdominio que elijas)

Coolify y Traefik generarán automáticamente los certificados SSL gratuitos mediante Let's Encrypt.

### Paso 4: Desplegar
Haz clic en **Deploy**. Coolify compilará la imagen de NestJS, levantará PostgreSQL, Redis y el panel web de Flutter en contenedores aislados y saludables.
