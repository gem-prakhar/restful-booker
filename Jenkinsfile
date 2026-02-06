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
        RERUN_FILE         = "${WORKSPACE}\\target\\rerun.txt"
        RERUN_SUMMARY      = "${WORKSPACE}\\target\\rerun-summary.json"

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
            name: 'ENABLE_RETRY',
            defaultValue: true,
            description: 'Enable automatic retry of failed tests'
        )
        string(
            name: 'MAX_RETRIES',
            defaultValue: '3',
            description: 'Maximum number of retry attempts per test (1-5)'
        )
        string(
            name: 'MAX_RETRY_ROUNDS',
            defaultValue: '2',
            description: 'Maximum rounds of retrying all failures (1-3)'
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
                    echo "Retry Enabled: ${params.ENABLE_RETRY}"
                    echo "Max Retries: ${params.MAX_RETRIES}"
                    echo "================================================"

                    // Clean previous reports (Windows)
                    bat '''
                        if exist target\\site\\serenity rmdir /s /q target\\site\\serenity
                        if exist target\\cucumber-reports rmdir /s /q target\\cucumber-reports
                        del /q target\\ai-failures*.json 2>nul
                        del /q target\\ai-failure-report.html 2>nul
                        del /q target\\rerun.txt 2>nul
                        del /q target\\rerun-summary.json 2>nul
                    '''
                }
            }
        }

        stage('Run Tests (With Retry)') {
            when {
                expression { params.ENABLE_RETRY }
            }
            steps {
                script {
                    echo "Running tests with retry mechanism enabled..."

                    def gradleCommand = "gradlew.bat executeTestsWithRetry"
                    gradleCommand += " -Denvironment=${ENVIRONMENT}"
                    gradleCommand += " -DmaxRetries=${params.MAX_RETRIES}"
                    gradleCommand += " -DmaxRetryRounds=${params.MAX_RETRY_ROUNDS}"
                    gradleCommand += " --continue"

                    echo "Executing: ${gradleCommand}"

                    def testResult = bat(
                        script: gradleCommand,
                        returnStatus: true
                    )

                    env.TEST_EXIT_CODE = testResult.toString()

                    if (testResult != 0) {
                        echo "Tests completed with failures (exit code: ${testResult})"
                        currentBuild.result = 'UNSTABLE'
                    } else {
                        echo "All tests passed successfully"
                    }
                }
            }
        }

        stage('Run Tests (No Retry)') {
            when {
                expression { !params.ENABLE_RETRY }
            }
            steps {
                script {
                    echo "Running tests WITHOUT retry mechanism..."

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
                        echo "Tests completed with failures (exit code: ${testResult})"
                        currentBuild.result = 'UNSTABLE'
                    } else {
                        echo "All tests passed successfully"
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
                        echo "AI Failure Report generated successfully"

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

                        // Show retry metadata if available
                        if (report.retryMetadata) {
                            echo """
RETRY STATISTICS
================================================
Retry Enabled: ${report.retryMetadata.retryEnabled}
Scenarios Passed After Retry: ${report.retryMetadata.scenariosPassedAfterRetry}
Scenarios Failed All Retries: ${report.retryMetadata.scenariosFailedAllRetries}
================================================
"""
                        }

                        if (report.summary.failedScenarios > 0) {
                            echo "\nFailed Scenarios:"
                            report.features.each { feature ->
                                if (feature.failures && feature.failures.size() > 0) {
                                    echo "  Feature: ${feature.featureName}"
                                    feature.failures.each { failure ->
                                        echo "    - ${failure.scenarioName} (Line: ${failure.line})"
                                        if (failure.retryInfo) {
                                            echo "      Retries: ${failure.retryInfo.totalAttempts} attempts"
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        echo "AI Failure Report not found"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }

        stage('Display Retry Summary') {
            when {
                expression {
                    params.ENABLE_RETRY && fileExists(RERUN_SUMMARY)
                }
            }
            steps {
                script {
                    echo "Reading retry summary..."

                    def retrySummary = readJSON file: RERUN_SUMMARY

                    echo """
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
RETRY SUMMARY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Total Scenarios: ${retrySummary.totalScenarios}
Passed First Attempt: ${retrySummary.scenariosPassedFirstAttempt}
Passed After Retry: ${retrySummary.scenariosPassedAfterRetry}
Still Failing: ${retrySummary.scenariosStillFailing}
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
"""

                    if (retrySummary.scenariosPassedAfterRetry > 0) {
                        echo "\nTests that passed after retry:"
                        retrySummary.scenarios.each { scenario ->
                            if (scenario.finalStatus == "PASSED" && scenario.passedOnAttempt > 1) {
                                echo "  - ${scenario.name} (passed on attempt ${scenario.passedOnAttempt})"
                            }
                        }
                    }

                    if (retrySummary.scenariosStillFailing > 0) {
                        echo "\nTests that failed all retry attempts:"
                        retrySummary.scenarios.each { scenario ->
                            if (scenario.finalStatus == "FAILED") {
                                echo "  - ${scenario.name} (${scenario.totalAttempts} attempts)"
                            }
                        }
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
                        echo "Enhanced report generated"
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
                        echo "HTML summary generated"
                    }
                }
            }
        }

        stage('Publish Reports') {
            steps {
                script {
                    echo "Publishing reports..."

                    // Publish Serenity Report
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/site/serenity',
                        reportFiles: 'index.html',
                        reportName: 'Serenity Report'
                    ])

                    // Publish AI HTML Summary
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

                    // Archive JSON reports
                    archiveArtifacts artifacts: 'target\\ai-failures*.json', allowEmptyArchive: true
                    archiveArtifacts artifacts: 'target\\rerun*.json', allowEmptyArchive: true
                    archiveArtifacts artifacts: 'target\\rerun.txt', allowEmptyArchive: true
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

                    echo "Triggering AI Copilot analysis with retry-filtered results..."

                    // Optional: Include retry summary for AI analysis
                    def retryData = fileExists(RERUN_SUMMARY)
                        ? readJSON(file: RERUN_SUMMARY)
                        : null

                    build job: 'AI-Copilot-Analysis',
                          parameters: [
                              string(name: 'FAILURE_REPORT_PATH', value: inputReport),
                              string(name: 'RETRY_SUMMARY_PATH', value: RERUN_SUMMARY),
                              string(name: 'BUILD_NUMBER', value: BUILD_NUMBER),
                              string(name: 'BUILD_URL', value: BUILD_URL),
                              string(name: 'RETRY_ENABLED', value: params.ENABLE_RETRY.toString())
                          ],
                          wait: false

                    echo "AI analysis triggered"
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
                echo "Retry Enabled: ${params.ENABLE_RETRY}"

                if (params.ENABLE_RETRY && fileExists(RERUN_SUMMARY)) {
                    def summary = readJSON file: RERUN_SUMMARY
                    echo "Scenarios Passed After Retry: ${summary.scenariosPassedAfterRetry}"
                    echo "Scenarios Failed All Retries: ${summary.scenariosStillFailing}"
                }

                echo "================================================"
            }
            cleanWs()
        }

        success {
            script {
                if (env.TEST_EXIT_CODE == '0') {
                    echo "All tests passed on first attempt"
                } else if (params.ENABLE_RETRY && fileExists(RERUN_SUMMARY)) {
                    def summary = readJSON file: RERUN_SUMMARY
                    if (summary.scenariosStillFailing == 0) {
                        echo "All tests passed (some required retries)"
                    }
                }
            }
        }

        unstable {
            script {
                echo "Pipeline marked UNSTABLE due to test failures"

                if (params.ENABLE_RETRY && fileExists(RERUN_SUMMARY)) {
                    def summary = readJSON file: RERUN_SUMMARY
                    echo "Failed after ${params.MAX_RETRY_ROUNDS} retry rounds: ${summary.scenariosStillFailing} scenarios"
                }

                if (params.FAIL_PIPELINE_ON_TEST_FAILURE) {
                    error("Failing pipeline due to test failures")
                }
            }
        }

        failure {
            script {
                echo "Pipeline execution failed"

                // Send notification with retry info
                if (params.ENABLE_RETRY && fileExists(RERUN_SUMMARY)) {
                    def summary = readJSON file: RERUN_SUMMARY

                    emailext(
                        subject: "Test Execution Failed - Build #${BUILD_NUMBER} (With Retry)",
                        body: """
Build #${BUILD_NUMBER} has failed.

Environment: ${ENVIRONMENT}
Retry Enabled: Yes
Max Retries: ${params.MAX_RETRIES}

Test Results:
- Total Scenarios: ${summary.totalScenarios}
- Passed First Attempt: ${summary.scenariosPassedFirstAttempt}
- Passed After Retry: ${summary.scenariosPassedAfterRetry}
- Failed All Retries: ${summary.scenariosStillFailing}

Check the build at: ${BUILD_URL}

Reports:
- AI Failure Report: ${BUILD_URL}artifact/target/ai-failures.json
- Retry Summary: ${BUILD_URL}artifact/target/rerun-summary.json
- Serenity Report: ${BUILD_URL}Serenity_Report/
                        """,
                        to: '${DEFAULT_RECIPIENTS}',
                        attachLog: true
                    )
                } else {
                    emailext(
                        subject: "Test Execution Failed - Build #${BUILD_NUMBER}",
                        body: """
Build #${BUILD_NUMBER} has failed.

Environment: ${ENVIRONMENT}
Retry Enabled: No

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
}
