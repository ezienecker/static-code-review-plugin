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

package de.manuzid.staticcodereviewplugin

import de.manuzid.staticcodereviewplugin.model.GitLabAuthenticationConfiguration
import de.manuzid.staticcodereviewplugin.model.GitLabConfiguration
import de.manuzid.staticcodereviewplugin.model.ProxyConfiguration
import de.manuzid.staticcodereviewplugin.model.SpotbugsConfiguration
import de.manuzid.staticcodereviewplugin.service.AnalyseService
import de.manuzid.staticcodereviewplugin.service.GitApiService
import de.manuzid.staticcodereviewplugin.service.GitLabApiServiceImpl
import de.manuzid.staticcodereviewplugin.service.SpotbugsAnalyseServiceImpl
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

@Mojo(name = "report", defaultPhase = LifecyclePhase.VERIFY)
class ReportSpotBugsMojo : AbstractReportMojo() {

    @Parameter(property = "applicationSources", defaultValue = "src/main/java")
    private var applicationSourcePath: String = "src/main/java"

    @Parameter(property = "compiledClasses", defaultValue = "classes")
    private var compiledClassPath: String = "classes"

    @Parameter(property = "priorityThresholdLevel", defaultValue = "3")
    private var priorityThresholdLevel: Int = 3

    override fun execute() {
        if (isAnalyzerActive()) {
            log.info("Static Code Review has been skipped")
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

        val analyseService = spotbugsAnalyseServiceImpl(affectedFilePaths)
        analyseService.analyse()
        val reportedIssues = analyseService.getReportedIssues()

        if (reportedIssues.isEmpty()) {
            log.info("No bugs found.")
            return
        }

        log.info("Post ${reportedIssues.size.bottles("comment", "comments")} to merge request.")
        gitApiService.commentMergeRequest(reportedIssues, mergeRequestMetaData)
    }

    override fun getAnalyzer(): String = "spotbugs"

    private fun gitLabApiServiceImpl(): GitApiService {
        val authConfiguration = GitLabAuthenticationConfiguration(authToken, authUsername, authPassword)
        val proxyConfiguration = ProxyConfiguration(proxyServerAddress, proxyUsername, proxyPassword)
        val gitLabConfiguration = GitLabConfiguration(gitLabUrl, authConfiguration, projectId, mergeRequestIid.toInt(),
                proxyConfiguration)

        return GitLabApiServiceImpl(gitLabConfiguration)
    }

    private fun spotbugsAnalyseServiceImpl(filePaths: List<String>): AnalyseService {
        val spotbugsConfiguration = SpotbugsConfiguration(project.artifactId, filePaths, priorityThresholdLevel,
                project.build.directory, applicationSourcePath.removeSurrounding("/"),
                compiledClassPath.removeSurrounding("/"))
        return SpotbugsAnalyseServiceImpl(spotbugsConfiguration)
    }

    private fun Int.bottles(singular: String, plural: String): String = when (this) {
        0 -> "no $singular"
        1 -> "1 $singular"
        else -> "$this $plural"
    }

    private fun String.removeSurrounding(sign: String): String = this.removePrefix(sign).removeSuffix(sign)
}
