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

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject

abstract class AbstractReportMojo : AbstractMojo() {

    @Parameter(property = "gitLabUrl", required = false)
    protected lateinit var gitLabUrl: String

    @Parameter(property = "projectId", required = false)
    protected lateinit var projectId: String

    @Parameter(property = "mergeRequestIid", required = false)
    protected lateinit var mergeRequestIid: Integer

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

    @Parameter(property = "exclusions", required = false)
    private val exclusions: List<String> = ArrayList()

    fun isAnalyzerActive(): Boolean = getAnalyzer() in exclusions

    protected abstract fun getAnalyzer(): String

}
