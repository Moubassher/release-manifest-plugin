package com.moubassher.manifestplugin

class ReleaseManifestExtension {
    String companyName = "YourCompany"           // for remote path or display
    String appVersion = "1.0.0"                  // override project.version
    File outputDir = null                         // where to write manifest (defaults to jpackage/app)
    File remoteFile = null                        // optional: remote manifest path
    List<String> includePatterns = ["**/*"]      // files to include in manifest
    List<String> excludePatterns = []            // files to ignore
    String hashAlgorithm = "SHA-256"             // default hashing algorithm
}
