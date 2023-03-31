#!groovy

def call(Map configMap){
    echo "Pipeline started just now"
    def pomMap = [:]
    pipeline{
        agent any
        environment{
            AWS_ACCOUNT_ID="752692907119"
            REGION="ap-south-1"
            IMAGE_REPO="node-api"
            IMAGE_TAG="latest"
            REPO_URI="${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${IMAGE_REPO}"
            CI=false
        }
        /* options{
            timeout(time: 1, unit: 'HOURS')
            disableConcurrentBuilds()
            skipDefaultCheckout()
            timestamps()
        } */
        stages{
            stage('Clone'){
                steps{
                    script{
                        echo "Clone started"
                        gitInfo = checkout scm
                        
                    }
                }
            }
            stage('Build'){
                steps{
                    script{
                        sh '''
                            npm install
                        '''
                    }
                }
            }

        stage('Docker build'){
            steps{
                script{
                    sh "docker build -t ${IMAGE_REPO}:latest . "
                }
            }
        }
       
        stage('Image push to ECR'){
            steps{
                script{
                    withAWS(credentials: 'aws-auth', region: "${REGION}") {
                        sh """
                            aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com
                        docker tag ${IMAGE_REPO}:latest  ${REPO_URI}:latest
                        docker push ${REPO_URI}:latest
                        """
                    }
                }
            }
        }
        stage('Deploy'){
            steps{
                script{
                    withAWS(credentials: 'aws-auth', region: "${REGION}") {
                        sh """
                         aws eks update-kubeconfig --name toptal-cluster
                         kubectl get nodes
                         kubectl apply -f manifest.yaml
                        """
                    }
                }
            }
        }
        }
    }
}