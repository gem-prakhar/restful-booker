#!/usr/bin/env python3
"""
AI Failure Report Enhancement Script

This script enhances the basic AI failure report with additional context:
- Aggregates failure patterns
- Identifies flaky tests (requires historical data)
- Adds screenshot references from Serenity
- Categorizes errors by type
- Provides AI-friendly insights
"""

import json
import argparse
import os
import re
from pathlib import Path
from datetime import datetime
from collections import defaultdict, Counter


class FailureReportEnhancer:
    def __init__(self, input_path, output_path, serenity_report_path=None):
        self.input_path = input_path
        self.output_path = output_path
        self.serenity_report_path = serenity_report_path

    def enhance(self):
        """Main enhancement process"""
        print(f"Loading report from: {self.input_path}")

        with open(self.input_path, 'r') as f:
            report = json.load(f)

        print("Enhancing report with additional context...")

        # Add enhancement metadata
        report['enhancement'] = {
            'enhancedAt': datetime.utcnow().isoformat(),
            'version': '1.0.0'
        }

        # Perform enhancements
        self._add_failure_patterns(report)
        self._categorize_errors(report)
        self._add_actionable_insights(report)
        self._add_retry_suggestions(report)

        if self.serenity_report_path and os.path.exists(self.serenity_report_path):
            self._link_serenity_artifacts(report)

        # Save enhanced report
        print(f"Saving enhanced report to: {self.output_path}")
        with open(self.output_path, 'w') as f:
            json.dump(report, f, indent=2)

        print("✅ Enhancement complete")
        return report

    def _add_failure_patterns(self, report):
        """Identify common failure patterns across scenarios"""
        patterns = {
            'timeout_failures': [],
            'assertion_failures': [],
            'connection_failures': [],
            'authentication_failures': [],
            'data_validation_failures': [],
            'null_pointer_failures': [],
            'other_failures': []
        }

        for feature in report.get('features', []):
            for failure in feature.get('failures', []):
                error_msg = (failure.get('errorMessage') or '').lower()
                error_type = (failure.get('errorType') or '').lower()

                failure_info = {
                    'feature': feature['featureName'],
                    'scenario': failure['scenarioName'],
                    'line': failure['line'],
                    'error': failure.get('errorMessage', 'Unknown error')
                }

                # Categorize failures
                if 'timeout' in error_msg or 'timeout' in error_type:
                    patterns['timeout_failures'].append(failure_info)
                elif 'assertion' in error_msg or 'expected' in error_msg:
                    patterns['assertion_failures'].append(failure_info)
                elif 'connection' in error_msg or 'unable to connect' in error_msg:
                    patterns['connection_failures'].append(failure_info)
                elif 'auth' in error_msg or 'unauthorized' in error_msg or '401' in error_msg:
                    patterns['authentication_failures'].append(failure_info)
                elif 'validation' in error_msg or 'invalid' in error_msg:
                    patterns['data_validation_failures'].append(failure_info)
                elif 'nullpointer' in error_type or 'null' in error_msg:
                    patterns['null_pointer_failures'].append(failure_info)
                else:
                    patterns['other_failures'].append(failure_info)

        # Remove empty categories
        patterns = {k: v for k, v in patterns.items() if v}

        report['failurePatterns'] = patterns
        report['failurePatternSummary'] = {
            category: len(failures)
            for category, failures in patterns.items()
        }

    def _categorize_errors(self, report):
        """Categorize errors by type for easier AI analysis"""
        error_categories = defaultdict(list)

        for feature in report.get('features', []):
            for failure in feature.get('failures', []):
                error_type = failure.get('errorType', 'UnknownError')
                error_categories[error_type].append({
                    'feature': feature['featureName'],
                    'scenario': failure['scenarioName'],
                    'message': failure.get('errorMessage', '')
                })

        report['errorCategories'] = dict(error_categories)
        report['errorCategorySummary'] = {
            error_type: len(occurrences)
            for error_type, occurrences in error_categories.items()
        }

    def _add_actionable_insights(self, report):
        """Generate AI-friendly actionable insights"""
        insights = []

        summary = report.get('summary', {})
        failed_count = summary.get('failedScenarios', 0)

        if failed_count == 0:
            insights.append({
                'type': 'SUCCESS',
                'severity': 'INFO',
                'message': 'All tests passed successfully. No action required.',
                'recommendation': 'Monitor for consistency in future runs.'
            })
        else:
            # Check for high failure rate
            total = summary.get('totalScenarios', 0)
            if total > 0:
                failure_rate = (failed_count / total) * 100

                if failure_rate > 50:
                    insights.append({
                        'type': 'HIGH_FAILURE_RATE',
                        'severity': 'CRITICAL',
                        'message': f'{failure_rate:.1f}% of tests failed ({failed_count}/{total})',
                        'recommendation': 'Investigate environment issues or recent code changes. This indicates systemic problems.'
                    })
                elif failure_rate > 20:
                    insights.append({
                        'type': 'MODERATE_FAILURE_RATE',
                        'severity': 'HIGH',
                        'message': f'{failure_rate:.1f}% of tests failed ({failed_count}/{total})',
                        'recommendation': 'Review failed scenarios for common patterns.'
                    })

            # Check for pattern-specific insights
            patterns = report.get('failurePatternSummary', {})

            if patterns.get('timeout_failures', 0) > 2:
                insights.append({
                    'type': 'TIMEOUT_PATTERN',
                    'severity': 'HIGH',
                    'message': f'{patterns["timeout_failures"]} timeout failures detected',
                    'recommendation': 'Check application response times, database performance, or increase timeout thresholds.'
                })

            if patterns.get('authentication_failures', 0) > 0:
                insights.append({
                    'type': 'AUTH_PATTERN',
                    'severity': 'HIGH',
                    'message': f'{patterns["authentication_failures"]} authentication failures detected',
                    'recommendation': 'Verify test credentials and authentication endpoints are working correctly.'
                })

            if patterns.get('connection_failures', 0) > 0:
                insights.append({
                    'type': 'CONNECTION_PATTERN',
                    'severity': 'CRITICAL',
                    'message': f'{patterns["connection_failures"]} connection failures detected',
                    'recommendation': 'Check network connectivity, service availability, and firewall rules.'
                })

            if patterns.get('null_pointer_failures', 0) > 0:
                insights.append({
                    'type': 'NPE_PATTERN',
                    'severity': 'MEDIUM',
                    'message': f'{patterns["null_pointer_failures"]} null pointer exceptions detected',
                    'recommendation': 'Review test data setup and null handling in step definitions.'
                })

        report['actionableInsights'] = insights

    def _add_retry_suggestions(self, report):
        """Suggest which tests should be retried"""
        retry_candidates = []

        for feature in report.get('features', []):
            for failure in feature.get('failures', []):
                error_msg = (failure.get('errorMessage') or '').lower()

                # Determine if failure is likely transient
                is_transient = any(keyword in error_msg for keyword in [
                    'timeout', 'connection', 'temporarily unavailable',
                    'service unavailable', 'network', '503', '504'
                ])

                retry_candidates.append({
                    'feature': feature['featureName'],
                    'scenario': failure['scenarioName'],
                    'line': failure['line'],
                    'tags': failure.get('tags', []),
                    'shouldRetry': is_transient,
                    'reason': 'Transient failure detected' if is_transient else 'Deterministic failure - requires fix'
                })

        report['retrySuggestions'] = retry_candidates

    def _link_serenity_artifacts(self, report):
        """Link screenshots and other Serenity artifacts to failures"""
        print(f"Scanning Serenity report at: {self.serenity_report_path}")

        # This is a placeholder - actual implementation would parse Serenity JSON
        # and match screenshots to specific test steps

        artifacts = {
            'screenshotsAvailable': True,
            'serenityReportPath': self.serenity_report_path,
            'note': 'Screenshot linking requires Serenity JSON parsing'
        }

        report['serenityArtifacts'] = artifacts


def main():
    parser = argparse.ArgumentParser(description='Enhance AI failure report with additional context')
    parser.add_argument('--input', required=True, help='Input AI failure report JSON')
    parser.add_argument('--output', required=True, help='Output enhanced report JSON')
    parser.add_argument('--serenity-report', help='Path to Serenity report directory')

    args = parser.parse_args()

    enhancer = FailureReportEnhancer(
        args.input,
        args.output,
        args.serenity_report
    )

    enhancer.enhance()


if __name__ == '__main__':
    main()