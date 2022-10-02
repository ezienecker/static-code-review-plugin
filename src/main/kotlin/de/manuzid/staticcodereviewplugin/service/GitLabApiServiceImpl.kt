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
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.ProxyClientConfig
import org.gitlab4j.api.models.DiffRef
import org.gitlab4j.api.models.MergeRequest
import org.gitlab4j.api.models.Position
import java.time.Instant
import java.util.*

class GitLabApiServiceImpl(gitConfiguration: GitConfiguration) : GitApiService {

    private val gitLabApi: GitLabApi
    private val projectId: String = gitConfiguration.projectId
    private val mergeRequestIid: Long = gitConfiguration.mergeRequestIid

    init {
        gitLabApi = if (gitConfiguration.authentication.token.isNullOrBlank()) {
            require(!gitConfiguration.authentication.username.isNullOrBlank()) { "The GitLab username must be present." }
            require(!gitConfiguration.authentication.password.isNullOrBlank()) { "The GitLab password must be present." }

            GitLabApi(
                gitConfiguration.gitUrl, gitConfiguration.authentication.username,
                gitConfiguration.authentication.password
            )
        } else {
            if (gitConfiguration.proxyConfiguration == null) {
                GitLabApi(gitConfiguration.gitUrl, gitConfiguration.authentication.token)
            } else {
                GitLabApi(
                    gitConfiguration.gitUrl, gitConfiguration.authentication.token, null,
                    ProxyClientConfig.createProxyClientConfig(
                        gitConfiguration.proxyConfiguration.serverAddress,
                        gitConfiguration.proxyConfiguration.userName,
                        gitConfiguration.proxyConfiguration.password
                    )
                )
            }
        }
    }

    override fun getAffectedFilePaths(): List<String> {
        val mergeRequestChanges = gitLabApi.mergeRequestApi.getMergeRequestChanges(projectId, mergeRequestIid)
        return extractFilePathsFromMergeRequest(mergeRequestChanges)
    }

    override fun getMergeRequestMetaData(): Any =
            gitLabApi.mergeRequestApi.getMergeRequest(projectId, mergeRequestIid).diffRefs

    override fun commentMergeRequest(issues: List<Issue>, mergeRequestMetaData: Any?) {
        mergeRequestMetaData as DiffRef

        issues.forEach {
            gitLabApi.discussionsApi.createMergeRequestDiscussion(projectId, mergeRequestIid, it.message,
                Date.from(Instant.now()), null, Position().apply {
                positionType = Position.PositionType.TEXT
                newLine = it.line
                baseSha = mergeRequestMetaData.baseSha
                headSha = mergeRequestMetaData.headSha
                startSha = mergeRequestMetaData.startSha
                oldPath = it.sourceFilePath
                newPath = it.sourceFilePath
            })
        }
    }

    private fun extractFilePathsFromMergeRequest(mergeRequestChanges: MergeRequest): List<String> =
            mergeRequestChanges.changes.map { it.newPath }.toList()

}
