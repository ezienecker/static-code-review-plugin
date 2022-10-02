/*
 * Copyright 2022 the original author or authors.
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

package de.manuzid.staticcodereviewplugin.service

import de.manuzid.staticcodereviewplugin.model.GitConfiguration
import de.manuzid.staticcodereviewplugin.model.Issue
import org.kohsuke.github.*

class GitHubApiServiceImpl(gitConfiguration: GitConfiguration) : GitApiService {

    private val gitHub: GitHub
    private val pullRequestChanges: GHPullRequest
    private val repository: String = gitConfiguration.repository!!
    private val mergeRequestIid: Long = gitConfiguration.mergeRequestIid

    init {
        gitHub = if (gitConfiguration.authentication.token.isNullOrBlank()) {
            require(!gitConfiguration.authentication.username.isNullOrBlank()) { "The GitLab username must be present." }
            require(!gitConfiguration.authentication.password.isNullOrBlank()) { "The GitLab password must be present." }

            GitHubBuilder()
                .withEndpoint(gitConfiguration.gitUrl)
                .withPassword(
                    gitConfiguration.authentication.username,
                    gitConfiguration.authentication.password
                ).build()
        } else {
            if (gitConfiguration.proxyConfiguration == null) {
                GitHubBuilder()
                    .withEndpoint(gitConfiguration.gitUrl)
                    .withOAuthToken(gitConfiguration.authentication.token)
                    .build()
            } else {
                GitHubBuilder()
                    .withEndpoint(gitConfiguration.gitUrl)
                    .withOAuthToken(gitConfiguration.authentication.token)
                    .build()
            }
        }

        pullRequestChanges = gitHub.getRepository(repository).getPullRequest(mergeRequestIid.toInt())
    }

    override fun getAffectedFilePaths(): List<String> =
        extractFilePathsFromPullRequest(pullRequestChanges.listFiles())

    override fun getMergeRequestMetaData(): Any =
        pullRequestChanges.listCommits().toList().first().sha

    override fun commentMergeRequest(issues: List<Issue>, mergeRequestMetaData: Any?) {
        mergeRequestMetaData as String

        issues.forEach {
            gitHub
                .getRepository(repository)
                .getPullRequest(mergeRequestIid.toInt())
                .createReviewComment(it.message, mergeRequestMetaData, it.sourceFilePath, it.line)
        }
    }

    private fun extractFilePathsFromPullRequest(pullRequestFileDetails: PagedIterable<GHPullRequestFileDetail>): List<String> =
        pullRequestFileDetails.toList().map { it.filename }.toList()
}
