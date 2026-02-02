pipeline {
    agent any

    environment {
        // Test execution environment
        ENVIRONMENT = "${params.ENVIRONMENT ?: 'default'}"
        TAGS = "${params.TAGS ?: ''}"

        // Report paths
        SERENITY_REPORT_DIR = "${WORKSPACE}/target/site/serenity"
        AI_FAILURE_JSON = "${WORKSPACE}/target/ai-failures.json"
        AI_ENHANCED_JSON = "${WORKSPACE}/target/ai-failures-enhanced.json"
        AI_HTML_REPORT = "${WORKSPACE}/target/ai-failure-report.html"

        // Gradle settings
        GRADLE_OPTS = "-Dorg.gradle.daemon=false"
    }

    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['default', 'staging', 'production'],
            description: 'Target environment for test execution'
        )
        string(
            name: 'TAGS',
            defaultValue: '',
            description: 'Cucumber tags to filter tests (e.g., @Booking, @CreateBooking)'
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
//                     echo "Tags: ${TAGS ?: 'All tests'}"
                    echo "Build: ${BUILD_NUMBER}"
                    echo "================================================"

                    // Clean previous reports
                    sh '''
                        rm -rf target/site/serenity
                        rm -rf target/cucumber-reports
                        rm -f target/ai-failures*.json
                        rm -f target/ai-failure-report.html
                    '''
                }
            }
        }

        stage('Run Tests') {
            steps {
                script {
                    def gradleCommand = "./gradlew clean test aggregate"

                    // Add tags if specified
                    if (TAGS) {
                        gradleCommand += " -Dcucumber.filter.tags=\"${TAGS}\""
                    }

                    // Add environment property
                    gradleCommand += " -Denvironment=${ENVIRONMENT}"

                    // Continue on test failure to ensure reports are generated
                    gradleCommand += " --continue"

                    echo "Executing: ${gradleCommand}"

                    // Run tests and capture exit code
                    def testResult = sh(
                        script: gradleCommand,
                        returnStatus: true
                    )

                    // Store test result for later use
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
                expression { params.GENERATE_AI_REPORT == true }
            }
            steps {
                script {
                    echo "Checking for AI failure report..."

                    def aiReportExists = fileExists(AI_FAILURE_JSON)

                    if (aiReportExists) {
                        echo "✅ AI Failure Report generated successfully"

                        // Display report summary
                        def reportContent = readFile(AI_FAILURE_JSON)
                        def report = readJSON text: reportContent

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
                            echo "⚠️ Failed scenarios detected:"
                            report.features.each { feature ->
                                if (feature.failures.size() > 0) {
                                    echo "  Feature: ${feature.featureName}"
                                    feature.failures.each { failure ->
                                        echo "    - ${failure.scenarioName} (Line: ${failure.line})"
                                        if (failure.failingStep) {
                                            echo "      Failed Step: ${failure.failingStep.keyword}${failure.failingStep.text}"
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        echo "⚠️ AI Failure Report not found - plugin may not have executed"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }

        stage('Enhance Failure Report') {
            when {
                expression {
                    params.GENERATE_AI_REPORT == true &&
                    fileExists(AI_FAILURE_JSON)
                }
            }
            steps {
                script {
                    echo "Enhancing AI failure report with additional context..."

                    // Run enhancement script
                    sh """
                        python3 ${WORKSPACE}/scripts/enhance-failure-report.py \
                            --input ${AI_FAILURE_JSON} \
                            --output ${AI_ENHANCED_JSON} \
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
                    params.GENERATE_HTML_SUMMARY == true &&
                    fileExists(env.AI_ENHANCED_JSON ?: AI_FAILURE_JSON)
                }
            }
            steps {
                script {
                    echo "Generating lightweight HTML summary..."

                    def inputReport = fileExists(AI_ENHANCED_JSON) ? AI_ENHANCED_JSON : AI_FAILURE_JSON

                    sh """
                        python3 ${WORKSPACE}/scripts/generate-html-summary.py \
                            --input ${inputReport} \
                            --output ${AI_HTML_REPORT}
                    """

                    if (fileExists(AI_HTML_REPORT)) {
                        echo "✅ HTML summary generated at: ${AI_HTML_REPORT}"
                    }
                }
            }
        }

        stage('Publish Reports') {
            steps {
                script {
                    echo "Publishing test reports..."

                    // Publish Serenity report
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/site/serenity',
                        reportFiles: 'index.html',
                        reportName: 'Serenity Report',
                        reportTitles: ''
                    ])

                    // Publish AI HTML summary if generated
                    if (fileExists(AI_HTML_REPORT)) {
                        publishHTML([
                            allowMissing: true,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'target',
                            reportFiles: 'ai-failure-report.html',
                            reportName: 'AI Failure Summary',
                            reportTitles: ''
                        ])
                    }

                    // Archive JSON reports
                    archiveArtifacts artifacts: 'target/ai-failures*.json', allowEmptyArchive: true
                    archiveArtifacts artifacts: 'target/cucumber-reports/**/*', allowEmptyArchive: true
                }
            }
        }

        stage('AI Analysis Trigger') {
            when {
                expression {
                    env.TEST_EXIT_CODE != '0' &&
                    fileExists(env.AI_ENHANCED_JSON ?: AI_FAILURE_JSON)
                }
            }
            steps {
                script {
                    echo "Triggering AI Copilot analysis..."

                    def inputReport = fileExists(AI_ENHANCED_JSON) ? AI_ENHANCED_JSON : AI_FAILURE_JSON

                    // This can be customized based on your AI Copilot integration
                    // Options:
                    // 1. Call external API
                    // 2. Trigger downstream Jenkins job
                    // 3. Send to message queue
                    // 4. Upload to S3/cloud storage for async processing

                    // Example: Trigger downstream job
                    build job: 'AI-Copilot-Analysis',
                          parameters: [
                              string(name: 'FAILURE_REPORT_PATH', value: inputReport),
                              string(name: 'BUILD_NUMBER', value: BUILD_NUMBER),
                              string(name: 'BUILD_URL', value: BUILD_URL)
                          ],
                          wait: false

                    echo "✅ AI analysis triggered"
                }
            }
        }
    }

    post {
        always {
            script {
                echo "================================================"
                echo "Pipeline Execution Complete"
                echo "Build Result: ${currentBuild.result ?: 'SUCCESS'}"
                echo "================================================"

                // Cleanup
                cleanWs(
                    deleteDirs: true,
                    patterns: [
                        [pattern: '.gradle', type: 'INCLUDE'],
                        [pattern: 'build', type: 'INCLUDE']
                    ]
                )
            }
        }

        success {
            script {
                if (env.TEST_EXIT_CODE == '0') {
                    echo "✅ All tests passed - No failures to analyze"
                }
            }
        }

        unstable {
            script {
                echo "⚠️ Pipeline marked as UNSTABLE due to test failures"

                if (params.FAIL_PIPELINE_ON_TEST_FAILURE) {
                    error("Marking pipeline as FAILED due to test failures")
                }
            }
        }

        failure {
            script {
                echo "❌ Pipeline execution failed"

                // Send notification (customize as needed)
                emailext(
                    subject: "Test Execution Failed - Build #${BUILD_NUMBER}",
                    body: """
                        Build #${BUILD_NUMBER} has failed.

                        Environment: ${ENVIRONMENT}
                        Tags: ${TAGS ?: 'All tests'}

                        Check the build at: ${BUILD_URL}

                        AI Failure Report: ${BUILD_URL}artifact/target/ai-failures.json
                    """,
                    to: '${DEFAULT_RECIPIENTS}',
                    attachLog: true
                )
            }
        }
    }
}