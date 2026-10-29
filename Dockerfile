# Utilizar imagen ligera oficial de Node.js Alpine
FROM node:20-alpine

# Definir directorio de trabajo
WORKDIR /app

# Instalar dependencias del sistema mínimas si fueran necesarias
RUN apk add --no-cache tzdata

# Copiar package.json y archivos base
COPY package.json ./

# Copiar código del servidor y archivos públicos
COPY server.js ./
COPY public/ ./public/
COPY data/ ./data/

# Asegurar permisos completos de lectura y escritura para el usuario node en el directorio de datos
RUN mkdir -p /app/data && chown -R node:node /app && chmod -R 775 /app/data

# Variables de entorno por defecto
ENV PORT=3080
ENV NODE_ENV=production

# Exponer el puerto de la aplicación
EXPOSE 3080

# Usuario sin privilegios por seguridad
USER node

# Comando de inicio
CMD ["node", "server.js"]
