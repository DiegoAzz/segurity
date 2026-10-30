# Procedimiento de Rollback y Recuperación ante Desastres — FamilyGuard 2.0

## 1. Procedimiento de Rollback en Coolify

Si un nuevo commit en `familyguard-v2` presenta un comportamiento no deseado en producción:

1. Entra a la aplicación en **Coolify**.
2. Ve a la pestaña **Deployments**.
3. Localiza el despliegue previo que funcionaba correctamente y haz clic en **Redeploy this commit**.
4. Coolify reconstruirá de inmediato la versión previa sin tocar la base de datos persistente.

---

## 2. Procedimiento de Restauración de Base de Datos (PostgreSQL)

Si necesitas restaurar una copia de seguridad en caso de fallo crítico de datos:

1. Localiza el archivo de respaldo más reciente en `/var/backups/familyguard_v2/` (ej: `fg2_backup_20261030_030000.sql.gz`).
2. Descomprime y restaura en el contenedor de PostgreSQL con este comando:

```bash
gunzip -c /var/backups/familyguard_v2/fg2_backup_20261030_030000.sql.gz | docker exec -i fg2_postgres psql -U fg_admin -d familyguard_prod
```

3. Reinicia el contenedor de backend en Coolify para refrescar las conexiones del pool:
```bash
docker restart fg2_backend
```
4. Verifica los registros estructurados:
```bash
docker logs --tail 50 -f fg2_backend
```
