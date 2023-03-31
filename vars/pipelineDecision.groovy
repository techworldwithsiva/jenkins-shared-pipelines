#!groovy

def applicationPipeline(Map configMap){
    application = configMap.get("application", "")
    switch(application) {
        case 'node':
            nodeNPM(configMap)
            break
        default:
            error "Un recognised Pipeline"
            break
    }
}