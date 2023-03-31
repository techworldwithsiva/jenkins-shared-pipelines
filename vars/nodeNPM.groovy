#!groovy

def call(Map configMap){
    echo "Pipeline started just now"
    def pomMap = [:]
    pipeline{
        agent any
       /*  parameters{
            string(name: 'COMPONENT_NAME', description: 'Enter the component name')
        } */
        environment{
            AWS_ACCOUNT_ID="752692907119"
            REGION="ap-south-1"
            COMPONENT_NAME="${configMap.COMPONENT_NAME}"
            IMAGE_TAG="latest"
            REPO_URI="${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${COMPONENT_NAME}"
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
                    sh "docker build -t ${COMPONENT_NAME}:latest . "
                }
            }
        }
       
        stage('Image push to ECR'){
            steps{
                script{
                    withAWS(credentials: 'aws-auth', region: "${REGION}") {
                        sh """
                            aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com
                        docker tag ${COMPONENT_NAME}:latest  ${REPO_URI}:latest
                        docker push ${REPO_URI}:latest
                        """
                    }
                }
            }
        }
        stage('Terraform Plan'){
            steps{
                script{
                    withAWS(credentials: 'aws-auth', region: "${REGION}") {
                        sh """
                        ls -ltr
                        cd terraform
                         terraform init
                         terraform plan
                        """
                    }
                }
            }
        }
        stage('Terraform Apply') {
            input {
                message "Should we continue?"
                ok "Yes, we should."
            }
            steps {
                script{
                withAWS(credentials: 'aws-auth', region: "ap-south-1") {
                        sh """
                         terraform apply -auto-approve
                        """
                    }
                }
            }
        }
        }
    }
}