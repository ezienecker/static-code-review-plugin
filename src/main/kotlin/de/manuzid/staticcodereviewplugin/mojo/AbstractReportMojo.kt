/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.manuzid.staticcodereviewplugin.mojo

import de.manuzid.staticcodereviewplugin.model.GitLabAuthenticationConfiguration
import de.manuzid.staticcodereviewplugin.model.GitLabConfiguration
import de.manuzid.staticcodereviewplugin.model.Issue
import de.manuzid.staticcodereviewplugin.model.ProxyConfiguration
import de.manuzid.staticcodereviewplugin.service.GitApiService
import de.manuzid.staticcodereviewplugin.service.GitLabApiServiceImpl
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject

abstract class AbstractReportMojo : AbstractMojo() {

    @Parameter(property = "gitLabUrl", required = false)
    protected lateinit var gitLabUrl: String

    @Parameter(property = "projectId", required = false)
    protected lateinit var projectId: String

    @Parameter(property = "mergeRequestIid", required = false)
    protected var mergeRequestIid: Long? = null

    @Parameter(property = "auth.token", required = false)
    protected var authToken: String? = null

    @Parameter(property = "auth.username", required = false)
    protected var authUsername: String? = null

    @Parameter(property = "auth.password", required = false)
    protected var authPassword: String? = null

    @Parameter(property = "proxy.serverAddress", required = false)
    protected var proxyServerAddress: String? = null

    @Parameter(property = "proxy.username", required = false)
    protected var proxyUsername: String? = null

    @Parameter(property = "proxy.password", required = false)
    protected var proxyPassword: String? = null

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    protected lateinit var project: MavenProject

    @Parameter(property = "applicationSources", defaultValue = "src/main/java")
    protected var applicationSourcePath: String = "src/main/java"

    @Parameter(property = "compiledClasses", defaultValue = "classes")
    protected var compiledClassPath: String = "classes"

    @Parameter(property = "exclusions", required = false)
    private val exclusions: List<String> = ArrayList()

    @Parameter(property = "static-code-review.skip", defaultValue = "false")
    private var skip: Boolean = false

    override fun execute() {
        if (mergeRequestIid == null) {
            log.error("Merge Request ID must not be null.")
            return
        }

        if (skip) {
            log.info("Static Code Review has been skipped.")
            return
        }

        if (isAnalyzerActive()) {
            log.info("Analyzer ${getAnalyzer()} is inactive.")
            return
        }

        log.info("Execute Static Code Review Plugin.")

        val gitApiService = gitLabApiServiceImpl()
        val affectedFilePaths = gitApiService.getAffectedFilePaths()
        val mergeRequestMetaData = gitApiService.getMergeRequestMetaData()

        if (affectedFilePaths.isEmpty()) {
            log.info("No files found to analyse.")
            return
        }

        log.debug("Obtain following ${affectedFilePaths.size.bottles("path", "paths")} from remote: $affectedFilePaths.")
        log.info("Start analysing ${affectedFilePaths.size.bottles("file", "files")}.")

        val reportedIssues = getIssuesFromAnalyzer(affectedFilePaths)

        if (reportedIssues.isEmpty()) {
            log.info("No bugs found.")
            return
        }

        log.info("Post ${reportedIssues.size.bottles("comment", "comments")} to merge request.")
        gitApiService.commentMergeRequest(reportedIssues, mergeRequestMetaData)
    }

    protected abstract fun getAnalyzer(): String

    protected abstract fun getIssuesFromAnalyzer(affectedFilePaths: List<String>): List<Issue>

    private fun gitLabApiServiceImpl(): GitApiService {
        val authConfiguration = GitLabAuthenticationConfiguration(authToken, authUsername, authPassword)
        val proxyConfiguration = ProxyConfiguration(proxyServerAddress, proxyUsername, proxyPassword)
        val gitLabConfiguration = GitLabConfiguration(gitLabUrl, authConfiguration, projectId, mergeRequestIid!!,
                proxyConfiguration)

        return GitLabApiServiceImpl(gitLabConfiguration)
    }

    private fun isAnalyzerActive(): Boolean = getAnalyzer() in exclusions

}

private fun Int.bottles(singular: String, plural: String): String = when (this) {
    0 -> "no $singular"
    1 -> "1 $singular"
    else -> "$this $plural"
}
