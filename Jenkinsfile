def configurations = [
    [platform: 'linux',   jdk: '21'],
    [platform: 'windows', jdk: '21'],
]

def tasks = [:]

for (cfg in configurations) {
    def platform = cfg.platform
    def jdk = cfg.jdk
    def stageName = "${platform}-${jdk}"

    tasks[stageName] = {
        def baseLabel = infra.getBuildAgentLabel([
            platform: platform,
            jdk: jdk
        ])

        node("${baseLabel} && spot") {
            timeout(time: 60, unit: 'MINUTES') {
                deleteDir()

                stage("Checkout vectorcast-execution (${stageName})") {
                    checkout scm
                }

                def m2repo = "${pwd(tmp: true)}/m2repo"

                stage("Build vectorcast-execution (${stageName})") {
                    if (isUnix()) {
                        sh """
                            mvn -B -ntp -U \\
                                -Dmaven.repo.local='${m2repo}' \\
                                -Dmaven.test.failure.ignore \\
                                -Dspotbugs.failOnError=false \\
                                -Dcheckstyle.failOnViolation=false \\
                                -Dcheckstyle.failsOnError=false \\
                                -Dpmd.failOnViolation=false \\
                                -Pjacoco \\
                                clean install
                        """
                    } else {
                        bat """
                            mvn -B -ntp -U ^
                                -Dmaven.repo.local="${m2repo}" ^
                                -Dmaven.test.failure.ignore ^
                                -Dspotbugs.failOnError=false ^
                                -Dcheckstyle.failOnViolation=false ^
                                -Dcheckstyle.failsOnError=false ^
                                -Dpmd.failOnViolation=false ^
                                -Pjacoco ^
                                clean install
                        """
                    }
                }

                stage("Publish test results (${stageName})") {
                    junit(
                        allowEmptyResults: true,
                        testResults: '**/target/surefire-reports/**/*.xml,**/target/failsafe-reports/**/*.xml'
                    )
                }

                stage("Archive plugin artifacts (${stageName})") {
                    archiveArtifacts(
                        artifacts: '**/target/*.hpi,**/target/*.jpi,**/target/*.jar',
                        fingerprint: true,
                        allowEmptyArchive: true
                    )
                }
            }
        }
    }
}

parallel tasks
