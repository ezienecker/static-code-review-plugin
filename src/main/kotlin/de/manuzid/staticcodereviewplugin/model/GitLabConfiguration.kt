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

package de.manuzid.staticcodereviewplugin.model

data class GitLabConfiguration(val gitLabUrl: String, val authentication: GitLabAuthenticationConfiguration,
                               val projectId: String, val mergeRequestIid: Int, val proxyConfiguration: ProxyConfiguration? = null)

data class GitLabAuthenticationConfiguration(val token: String?, val username: String?, val password: String?)

data class ProxyConfiguration(val serverAddress: String?, val userName: String?, val password: String?)
