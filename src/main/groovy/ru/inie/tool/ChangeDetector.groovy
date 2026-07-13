package ru.inie.tool

class ChangeDetector {
    
    enum ChangeType {
        COMMIT,
        PULL_REQUEST,
        TAG,
        UNKNOWN
    }
    
    /**
     * Определяет тип изменения на основе переменных окружения Jenkins
     */
    ChangeType detect(Map env) {
        if (isPullRequest(env)) {
            return ChangeType.PULL_REQUEST
        }
        
        if (isTag(env)) {
            return ChangeType.TAG
        }
        
        if (isCommit(env)) {
            return ChangeType.COMMIT
        }
        
        return ChangeType.UNKNOWN
    }
    
    private boolean isPullRequest(Map env) {
        // GitHub Multibranch Pipeline
        if (env['CHANGE_ID'] != null && env['CHANGE_ID'] != '') {
            return true
        }
        
        // GitHub Pull Request Builder
        if (env['ghprbPullId'] != null && env['ghprbPullId'] != '') {
            return true
        }
        
        // GitLab Merge Request
        if (env['gitlabMergeRequestId'] != null && env['gitlabMergeRequestId'] != '') {
            return true
        }
        
        // Bitbucket Pull Request
        if (env['BITBUCKET_PULL_REQUEST_ID'] != null && env['BITBUCKET_PULL_REQUEST_ID'] != '') {
            return true
        }
        
        // Generic PR check
        if (env['CHANGE_BRANCH'] != null && env['CHANGE_BRANCH'] != '') {
            return true
        }
        
        return false
    }
    
    private boolean isTag(Map env) {
        return env['TAG_NAME'] != null && env['TAG_NAME'] != ''
    }
    
    private boolean isCommit(Map env) {
        return env['GIT_COMMIT'] != null && env['GIT_COMMIT'] != ''
    }
    
    /**
     * Извлекает информацию о Pull Request
     */
    Map<String, String> getPullRequestInfo(Map env) {
        [
            'id': getPullRequestId(env),
            'sourceBranch': getSourceBranch(env),
            'targetBranch': getTargetBranch(env),
            'title': env['CHANGE_TITLE'] ?: '',
            'url': env['CHANGE_URL'] ?: ''
        ]
    }
    
    private String getPullRequestId(Map env) {
        return env['CHANGE_ID'] ?: 
               env['ghprbPullId'] ?: 
               env['gitlabMergeRequestId'] ?: 
               env['BITBUCKET_PULL_REQUEST_ID'] ?: 
               'unknown'
    }
    
    private String getSourceBranch(Map env) {
        return env['CHANGE_BRANCH'] ?:
               env['ghprbSourceBranch'] ?:
               env['gitlabSourceBranch'] ?:
               env['BITBUCKET_SOURCE_BRANCH'] ?:
               env['GIT_BRANCH'] ?:
               'unknown'
    }
    
    private String getTargetBranch(Map env) {
        return env['CHANGE_TARGET'] ?:
               env['ghprbTargetBranch'] ?:
               env['gitlabTargetBranch'] ?:
               env['BITBUCKET_TARGET_BRANCH'] ?:
               env['GIT_BRANCH'] ?:
               'unknown'
    }
}