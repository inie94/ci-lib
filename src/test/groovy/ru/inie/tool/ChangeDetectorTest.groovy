package ru.inie.tool

import spock.lang.Specification
import spock.lang.Unroll

class ChangeDetectorTest extends Specification {
    
    ChangeDetector detector
    
    def setup() {
        detector = new ChangeDetector()
    }
    
    @Unroll
    def "should detect #expectedType when environment has #description"() {
        expect:
        detector.detect(env) == expectedType
        
        where:
        env                                              | expectedType                        | description
        // Pull Request cases
        ['CHANGE_ID': '123']                             | ChangeDetector.ChangeType.PULL_REQUEST | 'CHANGE_ID set'
        ['ghprbPullId': '456']                           | ChangeDetector.ChangeType.PULL_REQUEST | 'GitHub PR Builder'
        ['gitlabMergeRequestId': '789']                  | ChangeDetector.ChangeType.PULL_REQUEST | 'GitLab MR'
        ['BITBUCKET_PULL_REQUEST_ID': '101']             | ChangeDetector.ChangeType.PULL_REQUEST | 'Bitbucket PR'
        ['CHANGE_BRANCH': 'feature/test']                | ChangeDetector.ChangeType.PULL_REQUEST | 'CHANGE_BRANCH set'
        
        // Commit cases
        ['GIT_COMMIT': 'abc123', 'BRANCH_NAME': 'develop'] | ChangeDetector.ChangeType.COMMIT     | 'regular commit'
        ['GIT_COMMIT': 'def456']                         | ChangeDetector.ChangeType.COMMIT     | 'commit without branch'
        
        // Tag cases
        ['TAG_NAME': 'v1.0.0', 'GIT_COMMIT': 'abc123']   | ChangeDetector.ChangeType.TAG         | 'git tag'
        
        // Unknown cases
        [:]                                              | ChangeDetector.ChangeType.UNKNOWN     | 'empty environment'
        ['SOME_OTHER_VAR': 'value']                      | ChangeDetector.ChangeType.UNKNOWN     | 'unknown variables only'
    }
    
    def "should return empty strings for missing PR info"() {
        when:
        def prInfo = detector.getPullRequestInfo([:])
        
        then:
        prInfo.id == 'unknown'
        prInfo.sourceBranch == 'unknown'
        prInfo.targetBranch == 'unknown'
        prInfo.title == ''
        prInfo.url == ''
    }
    
    def "should extract full PR info from GitHub multibranch"() {
        given:
        def env = [
            'CHANGE_ID': '42',
            'CHANGE_BRANCH': 'feature/new-feature',
            'CHANGE_TARGET': 'main',
            'CHANGE_TITLE': 'Add new feature',
            'CHANGE_URL': 'https://github.com/example/pr/42'
        ]
        
        when:
        def prInfo = detector.getPullRequestInfo(env)
        
        then:
        prInfo.id == '42'
        prInfo.sourceBranch == 'feature/new-feature'
        prInfo.targetBranch == 'main'
        prInfo.title == 'Add new feature'
        prInfo.url == 'https://github.com/example/pr/42'
    }
    
    def "should extract PR info from Bitbucket"() {
        given:
        def env = [
            'BITBUCKET_PULL_REQUEST_ID': '55',
            'BITBUCKET_SOURCE_BRANCH': 'bugfix/fix-login',
            'BITBUCKET_TARGET_BRANCH': 'develop'
        ]
        
        when:
        def prInfo = detector.getPullRequestInfo(env)
        
        then:
        prInfo.id == '55'
        prInfo.sourceBranch == 'bugfix/fix-login'
        prInfo.targetBranch == 'develop'
    }
    
    def "should extract PR info from GitLab"() {
        given:
        def env = [
            'gitlabMergeRequestId': '100',
            'gitlabSourceBranch': 'feature/gitlab-ci',
            'gitlabTargetBranch': 'master'
        ]
        
        when:
        def prInfo = detector.getPullRequestInfo(env)
        
        then:
        prInfo.id == '100'
        prInfo.sourceBranch == 'feature/gitlab-ci'
        prInfo.targetBranch == 'master'
    }
    
    def "should handle empty and null values gracefully"() {
        when:
        def result = detector.detect(null)
        
        then:
        thrown(NullPointerException)
    }
}