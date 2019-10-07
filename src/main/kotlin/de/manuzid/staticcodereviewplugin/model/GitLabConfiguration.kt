package de.manuzid.staticcodereviewplugin.model

data class GitLabConfiguration(val gitLabUrl: String, val authentication: GitLabAuthenticationConfiguration,
                               val projectId: String, val mergeRequestIid: Int, val proxyConfiguration: ProxyConfiguration? = null)

data class GitLabAuthenticationConfiguration(val token: String?, val username: String?, val password: String?)

data class ProxyConfiguration(val serverAddress: String?, val userName: String?, val password: String?)
