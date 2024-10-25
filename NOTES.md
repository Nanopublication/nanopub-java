Release new version:

    $ export MAVEN_OPTS="--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.desktop/java.awt.font=ALL-UNNAMED"
    $ mvn release:clean release:prepare
    $ mvn release:perform

Undo failed release attempt:

    $ git pull
    $ git log  # check how many commits
    $ git reset HEAD^^ --hard  # undo two local commits
    $ git push origin -f  # force push to remote
    $ git tag -d nanopub-1.43  # tag name needs to be adjusted
    $ git push --delete origin nanopub-1.43  # delete tag remotely too


Update Dependencies:

    $ mvn versions:use-latest-versions && mvn versions:update-properties
