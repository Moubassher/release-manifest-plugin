package com.moubassher.manifestplugin;
import groovy.json.JsonOutput
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.security.MessageDigest
import java.time.LocalDateTime

class ReleaseManifestPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        // 1️⃣ Create extension for user configuration
        def ext = project.extensions.create('releaseManifest', ReleaseManifestExtension)

        // 2️⃣ Register the task
        project.tasks.register('generateReleaseManifest') {
            group = 'build'
            description = 'Generates a JSON manifest of the packaged program which can be used to determine whether an update is available on a remote url.'
            dependsOn project.tasks.named('jpackage')

            doLast {
                def appName = project.name
                def appVersion = ext.appVersion ?: (project.version ?: "1.0.0")

                // Default outputDir if not set
                def buildDirFile = project.layout.buildDirectory.get().asFile
                def appDir = ext.outputDir ?: new File(buildDirFile, "jpackage/${appName}/app")
                if (!appDir.exists()) appDir.mkdirs()

                // Default remote URL if not set
                def remoteFile = ext.remoteFile ?:
                        new File(System.getProperty("user.home"),
                                "AppData/Local/${ext.companyName}/${appName}/app/manifest.json")

                // Gather files
                def filesInfo = []
                appDir.eachFileRecurse { f ->
                    if (f.isFile()) {
                        // Apply include/exclude patterns
                        def relativePath = appDir.toPath().relativize(f.toPath()).toString().replace("\\","/")
                        def included = ext.includePatterns.any { relativePath.matches(it.replace("**", ".*")) } &&
                                !ext.excludePatterns.any { relativePath.matches(it.replace("**", ".*")) }
                        if (!included) return

                        def sha = MessageDigest.getInstance(ext.hashAlgorithm)
                        f.withInputStream { is ->
                            byte[] buf = new byte[8192]
                            int len
                            while ((len = is.read(buf)) != -1) {
                                sha.update(buf, 0, len)
                            }
                        }
                        def hash = sha.digest().collect { String.format("%02x", it) }.join()
                        filesInfo << [ path: relativePath, sha256: hash, size: f.length() ]
                    }
                }

                // Build manifest
                def manifest = [
                        appName     : appName,
                        version     : appVersion,
                        companyName : ext.companyName,
                        remoteUrl   : "file:///${remoteFile}".replace("\\","/"),
                        releaseTime : LocalDateTime.now().toString(),
                        files       : filesInfo
                ]

                // Write manifest file
                def manifestFile = new File(appDir, "manifest.json")
                manifestFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(manifest))

                project.logger.lifecycle("✅ Manifest generated at: ${manifestFile.absolutePath}")
            }
        }
    }
}
