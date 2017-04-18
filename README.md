A [Gradle plugin](https://plugins.gradle.org/plugin/io.github.ddimtirov.codacy), 
uploading code coverage results to [Codacy.com](https://www.codacy.com/).

I am creating it only because [jpm4j](http://jpm4j.org/) broke recently, and I dislike 
[proposed](https://github.com/codacy/codacy-coverage-reporter)
[workarounds](https://github.com/mountain-pass/hyperstate/commit/857ca93e1c8484c14a5e2da9f0434d3daf3328ce),
involving copy/paste of lumps of code or jitpacking something from a github repo.
When/if Codacy provide official plugin, this plugin will be immediately deprecated and unsupported.
Meanwhile feel free to create issues int he Github tracker.

# Migrating from working TravisCI setup
Open your `build.gradle` file and add this line to the `plugins` section (you may also remove 
`jacoco` while you are at it):

```
id "io.github.ddimtirov.codacy" version "0.1.0"
```

Then add this line after the `plugins` section:

```
repositories.maven { url "http://dl.bintray.com/typesafe/maven-releases" } // FIXME: codacy uploader needs this
```

Done!

# Usage
Assuming your environment is set up, you can use the aggregating task:

```
gradlew codacyUpload
```

If not, you need to upload each coverage result individually and specify 
the token on the command line:

```
gradlew jacocoTestReportCodacyUpload --codacy-token fe12d3a13ce28cb7b3c2873b46
```

# Configuration
You can specify params [only when calling the individual publishing tasks](https://discuss.gradle.org/t/options-for-a-dependent-task/22275).

To find the available parameters and documentation:

```
gradlew help --task jacocoTestReportCodacyUpload
```

You can also set these from the build script through the plugin extension as follows:

```
codacy {
    toolVersion = '1.6' // override the version for com.codacy:codacy-coverage-reporter
    commitUuid = '2ca92c64bbcb7f97b204d3ee11ebd1e7075ea35f'
    projectToken = 'fe12d3a13ce28cb7b3c2873b46'
}
```

# Credits
* @tompahoward for creating the [initial Gradle integration recipe](https://github.com/mountain-pass/hyperstate/commit/857ca93e1c8484c14a5e2da9f0434d3daf3328ce)  from which I have borrowed.
* @MrRamych for creating the second recipe, leveraging jitpack.io to get the latest and greatest code.
