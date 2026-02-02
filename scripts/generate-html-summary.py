#!/usr/bin/env python3
"""
AI Failure HTML Report Generator

Generates a lightweight, focused HTML report from the AI failure JSON,
highlighting only meaningful failures and actionable information.
"""

import json
import argparse
from datetime import datetime


HTML_TEMPLATE = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Test Failure Analysis - Build {build_number}</title>
    <style>
        * {{
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }}
        
        body {{
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            background: #f5f5f5;
            color: #333;
            line-height: 1.6;
        }}
        
        .container {{
            max-width: 1400px;
            margin: 0 auto;
            padding: 20px;
        }}
        
        header {{
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            border-radius: 10px;
            margin-bottom: 30px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }}
        
        header h1 {{
            font-size: 28px;
            margin-bottom: 10px;
        }}
        
        .metadata {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 10px;
            font-size: 14px;
            opacity: 0.9;
        }}
        
        .metadata-item {{
            display: flex;
            gap: 8px;
        }}
        
        .metadata-label {{
            font-weight: 600;
        }}
        
        .summary {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }}
        
        .summary-card {{
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            border-left: 4px solid #667eea;
        }}
        
        .summary-card.failed {{
            border-left-color: #ef4444;
        }}
        
        .summary-card.passed {{
            border-left-color: #10b981;
        }}
        
        .summary-card.skipped {{
            border-left-color: #f59e0b;
        }}
        
        .summary-card h3 {{
            font-size: 14px;
            color: #666;
            margin-bottom: 8px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }}
        
        .summary-card .value {{
            font-size: 36px;
            font-weight: 700;
            color: #333;
        }}
        
        .insights {{
            background: white;
            padding: 25px;
            border-radius: 8px;
            margin-bottom: 30px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }}
        
        .insights h2 {{
            font-size: 20px;
            margin-bottom: 20px;
            color: #333;
        }}
        
        .insight {{
            padding: 15px;
            margin-bottom: 15px;
            border-radius: 6px;
            border-left: 4px solid;
        }}
        
        .insight.critical {{
            background: #fee;
            border-left-color: #ef4444;
        }}
        
        .insight.high {{
            background: #fef3c7;
            border-left-color: #f59e0b;
        }}
        
        .insight.medium {{
            background: #dbeafe;
            border-left-color: #3b82f6;
        }}
        
        .insight.info {{
            background: #d1fae5;
            border-left-color: #10b981;
        }}
        
        .insight-header {{
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 8px;
        }}
        
        .insight-type {{
            font-weight: 600;
            font-size: 14px;
        }}
        
        .insight-severity {{
            display: inline-block;
            padding: 2px 8px;
            border-radius: 3px;
            font-size: 11px;
            font-weight: 600;
            text-transform: uppercase;
        }}
        
        .insight-severity.critical {{
            background: #ef4444;
            color: white;
        }}
        
        .insight-severity.high {{
            background: #f59e0b;
            color: white;
        }}
        
        .insight-severity.medium {{
            background: #3b82f6;
            color: white;
        }}
        
        .insight-severity.info {{
            background: #10b981;
            color: white;
        }}
        
        .failures {{
            background: white;
            padding: 25px;
            border-radius: 8px;
            margin-bottom: 30px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }}
        
        .failures h2 {{
            font-size: 20px;
            margin-bottom: 20px;
            color: #333;
        }}
        
        .failure-item {{
            border: 1px solid #e5e7eb;
            border-radius: 6px;
            margin-bottom: 20px;
            overflow: hidden;
        }}
        
        .failure-header {{
            background: #f9fafb;
            padding: 15px;
            cursor: pointer;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }}
        
        .failure-header:hover {{
            background: #f3f4f6;
        }}
        
        .failure-title {{
            font-weight: 600;
            color: #333;
        }}
        
        .failure-meta {{
            display: flex;
            gap: 15px;
            font-size: 12px;
            color: #666;
            margin-top: 5px;
        }}
        
        .tag {{
            display: inline-block;
            background: #e0e7ff;
            color: #4338ca;
            padding: 2px 8px;
            border-radius: 3px;
            font-size: 11px;
            margin-right: 5px;
        }}
        
        .failure-details {{
            padding: 20px;
            border-top: 1px solid #e5e7eb;
            background: #fafafa;
        }}
        
        .error-message {{
            background: #fff;
            border-left: 4px solid #ef4444;
            padding: 15px;
            border-radius: 4px;
            margin-bottom: 15px;
            font-family: 'Monaco', 'Courier New', monospace;
            font-size: 13px;
            color: #dc2626;
        }}
        
        .steps {{
            margin-top: 15px;
        }}
        
        .step {{
            padding: 10px;
            margin-bottom: 8px;
            border-radius: 4px;
            font-size: 13px;
        }}
        
        .step.passed {{
            background: #f0fdf4;
            border-left: 3px solid #10b981;
        }}
        
        .step.failed {{
            background: #fef2f2;
            border-left: 3px solid #ef4444;
        }}
        
        .step.skipped {{
            background: #fef9f3;
            border-left: 3px solid #f59e0b;
        }}
        
        .step-keyword {{
            font-weight: 600;
            margin-right: 8px;
        }}
        
        .stacktrace {{
            background: #1f2937;
            color: #e5e7eb;
            padding: 15px;
            border-radius: 4px;
            font-family: 'Monaco', 'Courier New', monospace;
            font-size: 12px;
            overflow-x: auto;
            margin-top: 10px;
            max-height: 300px;
            overflow-y: auto;
        }}
        
        .patterns {{
            background: white;
            padding: 25px;
            border-radius: 8px;
            margin-bottom: 30px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }}
        
        .pattern-grid {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 15px;
            margin-top: 15px;
        }}
        
        .pattern-card {{
            border: 1px solid #e5e7eb;
            border-radius: 6px;
            padding: 15px;
        }}
        
        .pattern-card h4 {{
            font-size: 14px;
            margin-bottom: 10px;
            color: #666;
        }}
        
        .pattern-count {{
            font-size: 24px;
            font-weight: 700;
            color: #ef4444;
            margin-bottom: 10px;
        }}
        
        footer {{
            text-align: center;
            padding: 20px;
            color: #666;
            font-size: 12px;
        }}
        
        .toggle-icon {{
            transition: transform 0.3s;
        }}
        
        .toggle-icon.expanded {{
            transform: rotate(180deg);
        }}
    </style>
</head>
<body>
    <div class="container">
        <header>
            <h1>🔍 Test Failure Analysis</h1>
            <div class="metadata">
                <div class="metadata-item">
                    <span class="metadata-label">Build:</span>
                    <span>{build_number}</span>
                </div>
                <div class="metadata-item">
                    <span class="metadata-label">Environment:</span>
                    <span>{environment}</span>
                </div>
                <div class="metadata-item">
                    <span class="metadata-label">Duration:</span>
                    <span>{duration}</span>
                </div>
                <div class="metadata-item">
                    <span class="metadata-label">Generated:</span>
                    <span>{timestamp}</span>
                </div>
            </div>
        </header>
        
        <div class="summary">
            <div class="summary-card">
                <h3>Total Scenarios</h3>
                <div class="value">{total_scenarios}</div>
            </div>
            <div class="summary-card passed">
                <h3>Passed</h3>
                <div class="value">{passed_scenarios}</div>
            </div>
            <div class="summary-card failed">
                <h3>Failed</h3>
                <div class="value">{failed_scenarios}</div>
            </div>
            <div class="summary-card skipped">
                <h3>Skipped</h3>
                <div class="value">{skipped_scenarios}</div>
            </div>
        </div>
        
        {insights_section}
        
        {patterns_section}
        
        {failures_section}
        
        <footer>
            Generated by AI Failure Analyzer | {timestamp}
        </footer>
    </div>
    
    <script>
        // Toggle failure details
        document.querySelectorAll('.failure-header').forEach(header => {{
            header.addEventListener('click', () => {{
                const details = header.nextElementSibling;
                const icon = header.querySelector('.toggle-icon');
                
                if (details.style.display === 'none' || !details.style.display) {{
                    details.style.display = 'block';
                    icon.classList.add('expanded');
                }} else {{
                    details.style.display = 'none';
                    icon.classList.remove('expanded');
                }}
            }});
        }});
        
        // Initially hide all details
        document.querySelectorAll('.failure-details').forEach(details => {{
            details.style.display = 'none';
        }});
    </script>
</body>
</html>
"""


def format_duration(ms):
    """Format duration in milliseconds to human readable"""
    if not ms:
        return "N/A"

    seconds = ms / 1000
    if seconds < 60:
        return f"{seconds:.1f}s"

    minutes = seconds / 60
    if minutes < 60:
        return f"{minutes:.1f}m"

    hours = minutes / 60
    return f"{hours:.1f}h"


def generate_insights_section(report):
    """Generate insights HTML section"""
    insights = report.get('actionableInsights', [])

    if not insights:
        return ""

    insights_html = ['<div class="insights">', '<h2>📊 Actionable Insights</h2>']

    for insight in insights:
        severity = insight.get('severity', 'INFO').lower()
        insights_html.append(f'''
        <div class="insight {severity}">
            <div class="insight-header">
                <span class="insight-type">{insight.get('type', 'Unknown')}</span>
                <span class="insight-severity {severity}">{insight.get('severity', 'INFO')}</span>
            </div>
            <div>{insight.get('message', '')}</div>
            <div style="margin-top: 8px; font-size: 13px; opacity: 0.8;">
                <strong>Recommendation:</strong> {insight.get('recommendation', '')}
            </div>
        </div>
        ''')

    insights_html.append('</div>')
    return '\n'.join(insights_html)


def generate_patterns_section(report):
    """Generate failure patterns HTML section"""
    patterns = report.get('failurePatternSummary', {})

    if not patterns:
        return ""

    patterns_html = ['<div class="patterns">', '<h2>🎯 Failure Patterns</h2>', '<div class="pattern-grid">']

    pattern_labels = {
        'timeout_failures': 'Timeout Failures',
        'assertion_failures': 'Assertion Failures',
        'connection_failures': 'Connection Failures',
        'authentication_failures': 'Authentication Failures',
        'data_validation_failures': 'Data Validation Failures',
        'null_pointer_failures': 'Null Pointer Failures',
        'other_failures': 'Other Failures'
    }

    for pattern_key, count in patterns.items():
        label = pattern_labels.get(pattern_key, pattern_key.replace('_', ' ').title())
        patterns_html.append(f'''
        <div class="pattern-card">
            <h4>{label}</h4>
            <div class="pattern-count">{count}</div>
        </div>
        ''')

    patterns_html.append('</div></div>')
    return '\n'.join(patterns_html)


def generate_failures_section(report):
    """Generate failures HTML section"""
    features = report.get('features', [])

    failures_html = []
    failure_count = 0

    for feature in features:
        for failure in feature.get('failures', []):
            failure_count += 1

            tags_html = ''.join([f'<span class="tag">{tag}</span>' for tag in failure.get('tags', [])])

            failure_html = f'''
            <div class="failure-item">
                <div class="failure-header">
                    <div>
                        <div class="failure-title">{failure.get('scenarioName', 'Unknown Scenario')}</div>
                        <div class="failure-meta">
                            <span>📁 {feature.get('featureName', 'Unknown Feature')}</span>
                            <span>📍 Line {failure.get('line', 'N/A')}</span>
                            <span>⏱️ {format_duration(failure.get('duration'))}</span>
                        </div>
                        <div style="margin-top: 5px;">{tags_html}</div>
                    </div>
                    <span class="toggle-icon">▼</span>
                </div>
                <div class="failure-details">
            '''

            if failure.get('errorMessage'):
                failure_html += f'''
                    <div class="error-message">
                        ❌ {failure.get('errorMessage', 'Unknown error')}
                    </div>
                '''

            steps = failure.get('steps', [])
            if steps:
                failure_html += '<div class="steps"><h4>Test Steps:</h4>'
                for step in steps:
                    status = step.get('status', 'UNKNOWN').lower()
                    failure_html += f'''
                    <div class="step {status}">
                        <span class="step-keyword">{step.get('keyword', '')}</span>
                        <span>{step.get('text', '')}</span>
                        {f'<div style="margin-top: 5px; color: #dc2626; font-size: 12px;">Error: {step.get("errorMessage", "")}</div>' if step.get('errorMessage') else ''}
                    </div>
                    '''
                failure_html += '</div>'

            if failure.get('stackTrace'):
                failure_html += f'''
                    <div class="stacktrace">{failure.get('stackTrace', '')}</div>
                '''

            failure_html += '</div></div>'
            failures_html.append(failure_html)

    if not failures_html:
        return ""

    return f'<div class="failures"><h2>❌ Failed Scenarios ({failure_count})</h2>' + '\n'.join(failures_html) + '</div>'


def generate_html_report(report_data, output_path):
    """Generate HTML report from JSON data"""
    metadata = report_data.get('metadata', {})
    summary = report_data.get('summary', {})

    html_content = HTML_TEMPLATE.format(
        build_number=metadata.get('buildNumber', 'Unknown'),
        environment=metadata.get('environment', 'default'),
        duration=format_duration(metadata.get('duration')),
        timestamp=datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
        total_scenarios=summary.get('totalScenarios', 0),
        passed_scenarios=summary.get('passedScenarios', 0),
        failed_scenarios=summary.get('failedScenarios', 0),
        skipped_scenarios=summary.get('skippedScenarios', 0),
        insights_section=generate_insights_section(report_data),
        patterns_section=generate_patterns_section(report_data),
        failures_section=generate_failures_section(report_data)
    )

    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(html_content)
    print(f"HTML report generated: {output_path}")


def main():
    parser = argparse.ArgumentParser(description='Generate HTML summary from AI failure JSON')
    parser.add_argument('--input', required=True, help='Input AI failure report JSON')
    parser.add_argument('--output', required=True, help='Output HTML report path')

    args = parser.parse_args()

    print(f"Loading report from: {args.input}")
    with open(args.input, 'r', encoding='utf-8') as f:
        report_data = json.load(f)

    generate_html_report(report_data, args.output)


if __name__ == '__main__':
    main()