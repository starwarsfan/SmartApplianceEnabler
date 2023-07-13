library identifier: 'icheko-jenkins-shared-lib@master',
        retriever: modernSCM([
                $class: 'GitSCMSource',
                id    : '13ebda5f-2be5-4751-83d4-4d4500603cc5',
                remote: 'https://github.com/camueller/jenkins-shared-lib',
                traits: [[$class: 'jenkins.plugins.git.traits.BranchDiscoveryTrait']]
        ]) _

pipeline {
    agent {
        label 'buildx'
    }

    parameters {
        booleanParam(name: 'DOCKER_PUSH', defaultValue: false, description: 'Push docker image to Dockerhub?')
        booleanParam(name: 'BETA_RELEASE', defaultValue: false, description: 'Is this a beta release?')
        booleanParam(name: 'SKIP_BROWSER_TESTS', defaultValue: false, description: 'Skip tests on different browsers')
        string(name: 'DOCKER_IMAGE_NAME', defaultValue: 'avanux/smartapplianceenabler', description: 'Default name of Docker image')
    }

    environment {
        VERSION = readMavenPom().getVersion()
        BROWSERSTACK_USERNAME = credentials('BROWSERSTACK_USERNAME')
        BROWSERSTACK_ACCESS_KEY = credentials('BROWSERSTACK_ACCESS_KEY')
        DOCKER_TAG = "${env.BETA_RELEASE == "true" ? "beta" : "latest"}"
    }

    stages {
//        stage('Checkout') {
//            steps {
//                scmSkip(deleteBuild: false, skipPattern: '.*\\[ci skip\\].*')
//            }
//        }
        stage('Build') {
            steps {
                sh(
                    script: """
                        mvn clean -B -Pweb  
                        mvn package -B -Pweb
                    """
                )
            }
        }
        stage('Dockerize') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                    sh(
                        script: """
                            echo $PASSWORD | docker login --username $USERNAME --password-stdin
                            ./buildImages.sh -t ${DOCKER_IMAGE_NAME}:ci -v ${VERSION}
                        """
                    )
                }
            }
        }
        stage('Chrome') {
            when {
                expression {
                    env.SKIP_BROWSER_TESTS != 'true'
                }
            }
            steps {
                startContainer("${DOCKER_IMAGE_NAME}")
                sh(
                    script: """
                        cd src/test/angular
                        npm i
                        npm run test:chrome
                    """
                )
            }
        }
        stage('Firefox') {
            when {
                expression {
                    env.SKIP_BROWSER_TESTS != 'true'
                }
            }
            steps {
                startContainer("${DOCKER_IMAGE_NAME}")
                sh(
                    script: """
                        cd src/test/angular
                        npm run test:firefox
                    """
                )
            }
        }
        stage('Safari') {
            when {
                expression {
                    env.SKIP_BROWSER_TESTS != 'true'
                }
            }
            steps {
                startContainer("${DOCKER_IMAGE_NAME}")
                sh(
                    script: """
                        cd src/test/angular
                        npm run test:safari
                    """
                )
            }
        }
        stage('Stop') {
            steps {
                sh "docker stop sae || true"
            }
        }
        stage('Publish') {
            when {
                expression { params.DOCKER_PUSH }
            }
            steps {
                sh(
                    script: """
                        ./buildImages.sh -7 -8 -t ${DOCKER_IMAGE_NAME}:${DOCKER_TAG} -v ${VERSION} -p
                    """
                )
            }
        }
    }
}

def startContainer(String DOCKER_IMAGE_NAME) {
    script {
        sh(
            script: """
                docker stop sae || true
                docker volume rm -f sae
                docker volume create sae
                docker run \
                    -d \
                    --rm \
                    -v sae:/opt/sae/data \
                    -p 8081:8080 \
                    -e SAE_HTTP_DISABLED=true \
                    -e SAE_MODBUS_DISABLED=true \
                    --name sae \
                    ${DOCKER_IMAGE_NAME}:ci
            """
        )
    }
}
