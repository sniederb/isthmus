// Declarative pipeline, see https://jenkins.io/doc/book/pipeline/syntax/#declarative-pipeline
pipeline {
    agent any
    triggers { 
        pollSCM('H/30 * * * *') 
    }
    
    tools {
        maven 'maven-3.5.0'
        jdk 'jdk-8'
    }
    
    options {
        timestamps()
        timeout(time: 1, unit: 'HOURS')
        buildDiscarder(logRotator(numToKeepStr:'10'))
        disableConcurrentBuilds()
    }
    
    stages {
    	stage('Build and test') {
            steps {
            	script {
                    def exitCode = sh(script: "mvn clean verify -U", returnStatus: true)
                    echo "Exit code: ${exitCode}"
                    if (exitCode != 0) {
                        currentBuild.result = "UNSTABLE"
                    }
                }
                junit allowEmptyResults: true, testResults: 'target/surefire-reports/**/*.xml'
            }
        }
    }
    
    post {
        unstable {
            emailext body: '''${SCRIPT, template="junit-standard.html.template"}''', 
            subject: '$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!', 
            to: 'simon.niederberger@want.ch'
        }
        failure {
            emailext body: '''${SCRIPT, template="junit-standard.html.template"}''', 
            subject: '$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!', 
            to: 'simon.niederberger@want.ch'
        }
    }
}
