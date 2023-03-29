#!groovy

def call(Map configMap){
    echo "Pipeline started"
    def pomMap = [:]
    pipeline{
        agent {label 'JENKINS_SLAVE'}
        environment{
            AWS_ACCOUNT_ID="084767242532"
            REGION="ap-southeast-1"
        }
        options{
            timeout(time: 1, unit: 'HOURS')
            disableConcurrentBuilds()
            skipDefaultCheckout()
            timestamps()
        }
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
                        
                        withCredentials([usernamePassword(credentialsId: 'terraform-creds', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                            
                        sh '''
                        export AWS_ACCESS_KEY_ID="${USERNAME}"
                        export AWS_SECRET_ACCESS_KEY="${PASSWORD}"
                        terraform init    
                        terraform plan
                        '''
                        }
                    }
                }
            }

        }
    }
}
