package ru.inie.tool

import spock.lang.Specification
import spock.lang.Unroll

class PipelineWrapperTest extends Specification {
    
    def script
    def changeDetector
    def stageExecutor
    PipelineWrapper wrapper
    
    def setup() {
        script = Mock(Script)
        changeDetector = Mock(ChangeDetector)
        stageExecutor = Mock(PipelineStageExecutor)
        
        // Базовые моки для env
        script.getEnv() >> [:]
        
        wrapper = new PipelineWrapper(script, changeDetector, stageExecutor)
    }
    
    def "should execute commit pipeline when change type is COMMIT"() {
        given:
        script.getEnv() >> [
            'GIT_COMMIT': 'abc123',
            'BRANCH_NAME': 'feature/test'
        ]
        
        when:
        wrapper.execute()
        
        then:
        1 * changeDetector.detect(_) >> ChangeDetector.ChangeType.COMMIT
        1 * stageExecutor.executeCommitStages()
        0 * stageExecutor.executePRStages(_)
        0 * stageExecutor.executeDeployToDev()
    }
    
    def "should execute PR pipeline when change type is PULL_REQUEST"() {
        given:
        def env = [
            'CHANGE_ID': '42',
            'CHANGE_BRANCH': 'feature/test',
            'CHANGE_TARGET': 'main'
        ]
        script.getEnv() >> env
        
        and:
        def prInfo = [
            'id': '42',
            'sourceBranch': 'feature/test',
            'targetBranch': 'main'
        ]
        
        when:
        wrapper.execute()
        
        then:
        1 * changeDetector.detect(_) >> ChangeDetector.ChangeType.PULL_REQUEST
        1 * changeDetector.getPullRequestInfo(env) >> prInfo
        1 * stageExecutor.executePRStages(prInfo)
        0 * stageExecutor.executeCommitStages()
    }
    
    def "should handle TAG change type"() {
        given:
        script.getEnv() >> [
            'TAG_NAME': 'v1.0.0',
            'GIT_COMMIT': 'abc123'
        ]
        
        when:
        wrapper.execute()
        
        then:
        1 * changeDetector.detect(_) >> ChangeDetector.ChangeType.TAG
        1 * stageExecutor.executeTagStages()
        0 * stageExecutor.executeCommitStages()
        0 * stageExecutor.executePRStages(_)
    }
    
    @Unroll
    def "should deploy to DEV for main branches: #branch"() {
        given:
        script.getEnv() >> [
            'GIT_COMMIT': 'abc123',
            'BRANCH_NAME': branch
        ]
        
        when:
        wrapper.execute()
        
        then:
        1 * changeDetector.detect(_) >> ChangeDetector.ChangeType.COMMIT
        1 * stageExecutor.executeCommitStages()
        1 * stageExecutor.executeDeployToDev()
        0 * stageExecutor.executePRStages(_)
        
        where:
        branch << ['main', 'master', 'develop']
    }
    
    def "should throw error for UNKNOWN change type"() {
        given:
        script.getEnv() >> [:]
        
        when:
        wrapper.execute()
        
        then:
        1 * changeDetector.detect(_) >> ChangeDetector.ChangeType.UNKNOWN
        1 * script.error(_)
        0 * stageExecutor.executeCommitStages()
        0 * stageExecutor.executePRStages(_)
    }
}