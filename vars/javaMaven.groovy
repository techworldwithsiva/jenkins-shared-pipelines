#!groovy

def call(Map configMap){
    echo "Pipeline started"
    def pomMap = [:]
    def nexusUploadFile
    pipeline{
        agent {label 'master'}
        environment{
            AWS_ACCOUNT_ID="084767242532"
            REGION="ap-southeast-1"
            IMAGE_REPO="student-api"
            IMAGE_TAG="latest"
            REPO_URI="${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${IMAGE_REPO}"
            nexusUrl="13.250.103.76:8081"
            repository="OCBC"
        }
        parameters{
            booleanParam(name: 'deploy', defaultValue: false, description: '')
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
                        try{
                            def pom = readMavenPom file: 'pom.xml'
                            pomMap['group_id'] = pom.groupId
                            pomMap['version'] = pom.version
                            pomMap['artifactId'] = pom.artifactId
                            pomMap['packaging'] = pom.packaging
                            env.version = pom.version
                            echo "$pomMap.group_id"
                        }
                        catch(e){
                            error "Error: unable to read pom.xml, ${e}"
                        }
                    }
                }
            }
            stage('Build'){
                steps{
                    script{
                        sh '''     
                        mvn --version
                        mvn clean install'''
                    }
                }
            }
            

            stage('SonarQube Analysis') {
                steps {
                    withSonarQubeEnv('sonarqube') {
                        // Optionally use a Maven environment you've configured already
                        
                            sh '''
                            mvn --version
                            mvn clean package sonar:sonar

                            '''
                        
                    }
                }
            }
            /* stage('Publish Artifact to Nexus'){
              
                steps{
                    script{
                        def artifactType = pomMap.get("packaging", "jar") ?: "jar"
                        echo "packaging: ${artifactType}"//remove
                        nexusUploadFile = "${pomMap.artifactId}-${pomMap.version}.${artifactType}"
                        echo "nexus file: ${nexusUploadFile}"
                       /*  Map artifactsMap = [
                            gitInfo: gitInfo,
                            artifact: "./target/${nexusUploadFile}",
                            artifactType: artifactType,
                            group_id: pomMap.group_id,
                            module: "OCBC",
                            repoName: "echo-project"
                        ] //*
                        nexusArtifactUploader(
                            nexusVersion: "nexus3",
                            protocol: "HTTP",
                            nexusUrl: "$nexusUrl",
                            groupId: pomMap.group_id,
                            version: pomMap.version,
                            repository: "$repository",
                            credentialsId: "neuxs-creds",
                            artifacts: [
                                [artifactId: pomMap.artifactId,
                                file: "./target/${nexusUploadFile}",
                                type: pomMap.packaging]
                            ]
                        );
                        
                    }
                }
            } */
          
            /* stage("Quality gate") {
                steps {
                    waitForQualityGate abortPipeline: true
                } 
            } */
        stage('Docker build'){
            steps{
                script{
                    sh "docker build -t ${IMAGE_REPO}:$pomMap.version . "
                }
            }
        }
        stage('Image push to ECR'){
            steps{
                script{
                    sh '''
                    aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com
                    docker tag ${IMAGE_REPO}:${version}  ${REPO_URI}:${version}
                    docker push ${REPO_URI}:${version}
                    '''
                    

                }
            }
        }
        stage('Deploy'){
            steps{
                script{
                    sh '''
                    kubectl apply -f manifest.yaml
                    '''
                    

                }
            }
        }
        }
    }
}