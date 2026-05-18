pipeline {
    agent {
        kubernetes {
            yaml """
            apiVersion: v1
            kind: Pod
            spec:
              serviceAccountName: jenkins
              automountServiceAccountToken: true
              containers:
              - name: maven
                image: maven:3.9-eclipse-temurin-21
                command:
                - tail
                args:
                - -f
                - /dev/null
                volumeMounts:
                - name: m2-cache
                  mountPath: /root/.m2
              - name: kaniko
                image: gcr.io/kaniko-project/executor:debug
                command:
                - tail
                args:
                - -f
                - /dev/null
              - name: kubectl
                image: lachlanevenson/k8s-kubectl
                command:
                - tail
                args:
                - -f
                - /dev/null
                tty: true
              volumes:
              - name: m2-cache
                emptyDir: {}
            """
        }
    }
    environment {
        REGISTRY_URL = 'registry.registry.svc.cluster.local:5000'
        IMAGE_NAME   = 'prospectos-backend-app'
        NAMESPACE    = 'backend-ns'
    }
    stages {
        stage('Clone Repository') {
            steps {
                git branch: 'main',
                    credentialsId: 'github-token',
                    url: 'https://github.com/Carol-chp/Seguimientos_Prospecto_bases.git'
            }
        }
        stage('Build & Test') {
            steps {
                container('maven') {
                    // Tests omitidos por completo (no se compilan ni ejecutan en CI).
                    sh './mvnw -B --no-transfer-progress clean verify -Dmaven.test.skip=true'
                }
            }
        }
        stage('Build and Push Image') {
            steps {
                container('kaniko') {
                    sh """
                    /kaniko/executor --context=\$(pwd) \
                        --dockerfile=\$(pwd)/Dockerfile \
                        --destination=${REGISTRY_URL}/${IMAGE_NAME}:latest \
                        --insecure --skip-tls-verify
                    """
                }
            }
        }
        stage('Restart Deployment') {
            steps {
                container('kubectl') {
                    sh "kubectl rollout restart deployment/${IMAGE_NAME} -n ${NAMESPACE}"
                }
            }
        }
    }
    post {
        success {
            echo "Pipeline completed successfully! Image: ${REGISTRY_URL}/${IMAGE_NAME}:latest"
        }
        failure {
            echo "Pipeline failed!"
        }
    }
}
