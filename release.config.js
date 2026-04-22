let publishCmd = `
IMAGE_NAME_LOWER=$(echo "$IMAGE_NAME" | tr '[:upper:]' '[:lower:]')
docker build --build-arg="VERSION=\${nextRelease.version}" -t "$IMAGE_NAME_LOWER:\${nextRelease.version}" -t "$IMAGE_NAME_LOWER:latest" .
docker push --all-tags "$IMAGE_NAME_LOWER"
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
        {"path": "target/nanopub-*-jar-with-dependencies.jar", "label": "JAR for CLI usage"},
      ]
    }
  ],
  "@semantic-release/git"
)
module.exports = config