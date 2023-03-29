#!groovy

def applicationPipeline(Map configMap){
    application = configMap.get("application", "")
    switch(application) {
        case 'java':
            javaMaven(configMap)
            break
        case 'node':
            nodeNPM(configMap)
            break
        case 'terraform':
            terraform(configMap)
            break
        case 'eks':
            eks(configMap)
            break
        case 'ansible':
            ansibleDeploy(configMap)
            break
        default:
            error "Un recognised Pipeline"
            break
    }
}