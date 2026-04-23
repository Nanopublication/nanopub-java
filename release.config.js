let publishCmd = `
docker build -t "$IMAGE_NAME:\${nextRelease.version}" -t "$IMAGE_NAME:latest" .
docker push --all-tags "$IMAGE_NAME"
`
let config = require("semantic-release-preconfigured-conventional-commits");
config.tagFormat = "nanopub-${version}"
config.branches = ["release"]
config.plugins.push(
  [
    "@terrestris/maven-semantic-release",
    {
      "mavenTarget": "deploy",
      "settingsPath": "./settings.xml",
      "updateSnapshotVersion": true,
      "mvnw": true
    }
  ],
  [
    "@semantic-release/exec",
    {
      "publishCmd": publishCmd
    }
  ],
  [
    "@semantic-release/github",
    {
      "assets": [
        {
          "path": "target/nanopub-*-jar-with-dependencies.jar",
          "label": "JAR for CLI usage (v${nextRelease.version})"
        },
      ]
    }
  ],
  "@semantic-release/git"
)
module.exports = config