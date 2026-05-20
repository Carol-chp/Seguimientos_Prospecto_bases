# OpenWA — gateway de WhatsApp (uso interno)

Servicio self-hosted que automatiza envíos por WhatsApp Web (`whatsapp-web.js`).
**El backend de prospectos lo consume internamente**; NO se expone a internet.

> ⚠️ **Riesgo conocido.** WhatsApp puede banear el número vinculado por
> automatización de outreach (viola sus ToS). Usar exclusivamente con un
> **número DEDICADO de la empresa**, jamás un personal.

## Despliegue (en este orden)

1. **Construir imagen** (una sola vez, y al actualizar OpenWA):
   - Crear un job en Jenkins apuntando a `k8s-manifests/openwa/Jenkinsfile`
     (clona `https://github.com/rmyndharis/OpenWA.git`, build con kaniko a
     `registry.registry.svc.cluster.local:5000/openwa-api:latest`).

2. **Aplicar manifests:**
   ```bash
   sudo kubectl apply -k k8s-manifests/openwa/
   sudo kubectl rollout status deploy/openwa-api -n backend-ns --timeout=180s
   ```

3. **Vincular el número (paso manual, una sola vez):**
   1. Crear la sesión:
      ```bash
      KEY=$(sudo kubectl get secret openwa-secret -n backend-ns \
            -o jsonpath='{.data.API_MASTER_KEY}' | base64 -d)
      sudo kubectl exec -n backend-ns deploy/openwa-api -- \
        sh -c "curl -s -X POST -H 'X-API-Key: $KEY' \
        -H 'Content-Type: application/json' \
        -d '{\"name\":\"empresa\"}' http://localhost:2785/api/sessions"
      sudo kubectl exec -n backend-ns deploy/openwa-api -- \
        sh -c "curl -s -X POST -H 'X-API-Key: $KEY' \
        http://localhost:2785/api/sessions/empresa/start"
      ```
   2. Obtener el QR (se descarga al pwd como PNG):
      ```bash
      sudo kubectl exec -n backend-ns deploy/openwa-api -- \
        sh -c "curl -s -H 'X-API-Key: $KEY' \
        http://localhost:2785/api/sessions/empresa/qr -o /tmp/qr.png"
      sudo kubectl cp backend-ns/$(sudo kubectl get po -n backend-ns \
        -l app=openwa-api -o jsonpath='{.items[0].metadata.name}'):/tmp/qr.png ./qr.png
      ```
   3. Abrir `qr.png` y **escanear con el teléfono del número dedicado**
      (WhatsApp → Dispositivos vinculados → Vincular un dispositivo).
   4. Verificar estado:
      ```bash
      sudo kubectl exec -n backend-ns deploy/openwa-api -- \
        sh -c "curl -s -H 'X-API-Key: $KEY' \
        http://localhost:2785/api/sessions/empresa"
      ```

4. **Smoke test (envío de prueba):**
   ```bash
   sudo kubectl exec -n backend-ns deploy/openwa-api -- \
     sh -c "curl -s -X POST -H 'X-API-Key: $KEY' \
     -H 'Content-Type: application/json' \
     -d '{\"chatId\":\"51XXXXXXXXX@c.us\",\"text\":\"prueba\"}' \
     http://localhost:2785/api/sessions/empresa/messages/send-text"
   ```

## Configuración (backend de prospectos)

El backend usa estas variables (Secret/ConfigMap a definir en su deploy):
- `OPENWA_URL = http://openwa-api-service.backend-ns.svc.cluster.local`
- `OPENWA_API_KEY = <mismo valor que API_MASTER_KEY>`
- `OPENWA_SESSION = empresa`

## Notas operativas

- **Una sola réplica** (la sesión vive en el PVC; no se puede escalar).
- **El teléfono vinculado debe quedar online** (si se apaga > X horas, se cae la sesión).
- **NO** exponer este servicio por ingress: lo usa el backend in-cluster.
- Para ver Swagger: port-forward `kubectl -n backend-ns port-forward svc/openwa-api-service 2785:80` → `http://localhost:2785/api/docs`.
- Si el número es baneado: se compra/dedica otro y se repite el paso 3.
