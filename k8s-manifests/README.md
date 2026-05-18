# k8s-manifests — Sistema de Seguimiento de Prospectos

Manifiestos Kubernetes para desplegar el sistema (backend Spring Boot + frontend Angular)
en el clúster k3s (Traefik, namespaces `backend-ns` / `frontend-ns`, registro in-cluster,
kustomize).

## Base de datos — se REUTILIZA el Postgres existente

NO se despliega un Postgres propio. El backend usa la instancia existente
`postgres-infralub-service.default.svc.cluster.local:5432`, con una **base de datos y
usuario dedicados** ya creados:

- DB: `seguimiento_prospectos`
- Rol: `prospectos_user` (privilegios completos solo sobre esa DB)

La carpeta `prospectos-postgres/` queda como referencia/uso opcional fuera de este clúster;
**no se aplica aquí**.

## Estructura

| Carpeta | Namespace | Contenido |
|---|---|---|
| `prospectos-backend/`  | `backend-ns` | Spring Boot (Deployment+HPA+PDB), ConfigMap, Secret, Service, Ingress |
| `prospectos-frontend/` | `frontend-ns`| Angular estático (nginx), Service, ExternalName→backend, Ingress |
| `jenkins-rbac.yaml`    | backend-ns/frontend-ns | RBAC para que el SA `jenkins` haga rollout |
| `prospectos-postgres/` | — | Referencia (NO aplicar: se reutiliza el Postgres del clúster) |

## Despliegue (en pdmadmin@47.205.131.212, k3s)

```bash
sudo kubectl apply -f jenkins-rbac.yaml
sudo kubectl apply -k prospectos-backend/     # Flyway aplica V1→V3 al primer arranque
sudo kubectl apply -k prospectos-frontend/
```

Las imágenes (`registry.registry.svc.cluster.local:5000/prospectos-{backend,frontend}-app:latest`)
las construye y publica Jenkins con los `Jenkinsfile` de cada repo (kaniko, `--insecure
--skip-tls-verify`) y luego hace `kubectl rollout restart`. Hasta el primer build de
Jenkins, los pods quedarán en `ImagePullBackOff` (esperado).

Verificar:

```bash
sudo kubectl -n backend-ns rollout status deploy/prospectos-backend-app
sudo kubectl -n frontend-ns rollout status deploy/prospectos-frontend-app
sudo kubectl -n backend-ns exec deploy/prospectos-backend-app -- \
  wget -qO- localhost:8081/actuator/health   # {"status":"UP"}
```

## Editar si cambia el dominio

- Ingress backend: `api.prospectos.pdmmonitor.com` · frontend: `prospectos.pdmmonitor.com`.
- `prospectos-backend/configMap.yaml` → `CORS_ALLOWED_ORIGINS` = URL pública exacta del front.
- `propectos_front/Jenkinsfile` → `API_URL` (se hornea en la imagen del front).

> Tras el primer despliegue, crear el usuario dueño real y **eliminar/cambiar** el seed
> `admin`/`Admin123!` — no debe quedar en producción.
