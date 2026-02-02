pipeline {
    agent any

    environment {
        // Test execution environment
        ENVIRONMENT = "${params.ENVIRONMENT ?: 'default'}"

        // Report paths
        SERENITY_REPORT_DIR = "${WORKSPACE}\\target\\site\\serenity"
        AI_FAILURE_JSON    = "${WORKSPACE}\\target\\ai-failures.json"
        AI_ENHANCED_JSON   = "${WORKSPACE}\\target\\ai-failures-enhanced.json"
        AI_HTML_REPORT     = "${WORKSPACE}\\target\\ai-failure-report.html"

        // Gradle settings
        GRADLE_OPTS = "-Dorg.gradle.daemon=false"
    }

    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['default', 'staging', 'production'],
            description: 'Target environment for test execution'
        )
        booleanParam(
            name: 'GENERATE_AI_REPORT',
            defaultValue: true,
            description: 'Generate AI-consumable failure report'
        )
        booleanParam(
            name: 'GENERATE_HTML_SUMMARY',
            defaultValue: true,
            description: 'Generate lightweight HTML summary from failures'
        )
        booleanParam(
            name: 'FAIL_PIPELINE_ON_TEST_FAILURE',
            defaultValue: true,
            description: 'Mark pipeline as failed if any test fails'
        )
    }

    stages {

        stage('Environment Setup') {
            steps {
                script {
                    echo "================================================"
                    echo "Starting Test Execution Pipeline"
                    echo "Environment: ${ENVIRONMENT}"
                    echo "Build: ${BUILD_NUMBER}"
                    echo "================================================"

                    // Clean previous reports (Windows)
                    bat '''
                        if exist target\\site\\serenity rmdir /s /q target\\site\\serenity
                        if exist target\\cucumber-reports rmdir /s /q target\\cucumber-reports
                        del /q target\\ai-failures*.json 2>nul
                        del /q target\\ai-failure-report.html 2>nul
                    '''
                }
            }
        }

        stage('Run Tests') {
            steps {
                script {
                    def gradleCommand = "gradlew.bat clean test aggregate"
                    gradleCommand += " -Denvironment=${ENVIRONMENT}"
                    gradleCommand += " --continue"

                    echo "Executing: ${gradleCommand}"

                    def testResult = bat(
                        script: gradleCommand,
                        returnStatus: true
                    )

                    env.TEST_EXIT_CODE = testResult.toString()

                    if (testResult != 0) {
                        echo "⚠️ Tests completed with failures (exit code: ${testResult})"
                        currentBuild.result = 'UNSTABLE'
                    } else {
                        echo "✅ All tests passed successfully"
                    }
                }
            }
        }

        stage('Verify AI Failure Report') {
            when {
                expression { params.GENERATE_AI_REPORT }
            }
            steps {
                script {
                    echo "Checking for AI failure report..."

                    if (fileExists(AI_FAILURE_JSON)) {
                        echo "✅ AI Failure Report generated successfully"

                        def report = readJSON file: AI_FAILURE_JSON

                        echo """
================================================
AI FAILURE REPORT SUMMARY
================================================
Total Features: ${report.summary.totalFeatures}
Total Scenarios: ${report.summary.totalScenarios}
Passed: ${report.summary.passedScenarios}
Failed: ${report.summary.failedScenarios}
Skipped: ${report.summary.skippedScenarios}
Undefined: ${report.summary.undefinedScenarios}
Pass Rate: ${report.summary.passRate}%
================================================
"""

                        if (report.summary.failedScenarios > 0) {
                            report.features.each { feature ->
                                if (feature.failures && feature.failures.size() > 0) {
                                    echo "Feature: ${feature.featureName}"
                                    feature.failures.each { failure ->
                                        echo "  - ${failure.scenarioName} (Line: ${failure.line})"
                                    }
                                }
                            }
                        }
                    } else {
                        echo "⚠️ AI Failure Report not found"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }

        stage('Enhance Failure Report') {
            when {
                expression {
                    params.GENERATE_AI_REPORT &&
                    fileExists(AI_FAILURE_JSON)
                }
            }
            steps {
                script {
                    echo "Enhancing AI failure report..."

                    bat """
                        python scripts\\enhance-failure-report.py ^
                            --input ${AI_FAILURE_JSON} ^
                            --output ${AI_ENHANCED_JSON} ^
                            --serenity-report ${SERENITY_REPORT_DIR}
                    """

                    if (fileExists(AI_ENHANCED_JSON)) {
                        echo "✅ Enhanced report generated"
                    }
                }
            }
        }

        stage('Generate HTML Summary') {
            when {
                expression {
                    params.GENERATE_HTML_SUMMARY &&
                    (fileExists(AI_ENHANCED_JSON) || fileExists(AI_FAILURE_JSON))
                }
            }
            steps {
                script {
                    def inputReport = fileExists(AI_ENHANCED_JSON)
                        ? AI_ENHANCED_JSON
                        : AI_FAILURE_JSON

                    echo "Generating HTML summary from ${inputReport}"

                    bat """
                        python scripts\\generate-html-summary.py ^
                            --input ${inputReport} ^
                            --output ${AI_HTML_REPORT}
                    """

                    if (fileExists(AI_HTML_REPORT)) {
                        echo "✅ HTML summary generated"
                    }
                }
            }
        }

        stage('Publish Reports') {
            steps {
                script {
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/site/serenity',
                        reportFiles: 'index.html',
                        reportName: 'Serenity Report'
                    ])

                    if (fileExists(AI_HTML_REPORT)) {
                        publishHTML([
                            allowMissing: true,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'target',
                            reportFiles: 'ai-failure-report.html',
                            reportName: 'AI Failure Summary'
                        ])
                    }

                    archiveArtifacts artifacts: 'target\\ai-failures*.json', allowEmptyArchive: true
                    archiveArtifacts artifacts: 'target\\cucumber-reports\\**\\*', allowEmptyArchive: true
                }
            }
        }

        stage('AI Analysis Trigger') {
            when {
                expression {
                    env.TEST_EXIT_CODE != '0' &&
                    (fileExists(AI_ENHANCED_JSON) || fileExists(AI_FAILURE_JSON))
                }
            }
            steps {
                script {
                    def inputReport = fileExists(AI_ENHANCED_JSON)
                        ? AI_ENHANCED_JSON
                        : AI_FAILURE_JSON

                    echo "Triggering AI Copilot analysis"

                    build job: 'AI-Copilot-Analysis',
                          parameters: [
                              string(name: 'FAILURE_REPORT_PATH', value: inputReport),
                              string(name: 'BUILD_NUMBER', value: BUILD_NUMBER),
                              string(name: 'BUILD_URL', value: BUILD_URL)
                          ],
                          wait: false
                }
            }
        }
    }

    post {
        always {
            echo "================================================"
            echo "Pipeline Execution Complete"
            echo "Build Result: ${currentBuild.result ?: 'SUCCESS'}"
            echo "================================================"
            cleanWs()
        }

        success {
            script {
                if (env.TEST_EXIT_CODE == '0') {
                    echo "✅ All tests passed"
                }
            }
        }

        unstable {
            script {
                echo "⚠️ Pipeline marked UNSTABLE"
                if (params.FAIL_PIPELINE_ON_TEST_FAILURE) {
                    error("Failing pipeline due to test failures")
                }
            }
        }

        failure {
            script {
                echo "❌ Pipeline execution failed"
            }
        }
    }
}
