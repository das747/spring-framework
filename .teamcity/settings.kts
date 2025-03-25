import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.triggers.schedule
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2024.12"

object SpringFrameworkVcs : GitVcsRoot({
    name = "Spring Framework"
    url = "https://github.com/spring-projects/spring-framework.git"
    branch = "refs/heads/main"
    branchSpec = "+:*"
    checkoutMode = CheckoutMode.AUTO
})

object MainCIBuild : BuildType({
    name = "Main CI Build"
    description = "Main CI build running on multiple Java versions"
    
    vcs {
        root(SpringFrameworkVcs)
    }

    params {
        param("env.JAVA_HOME", "%env.JDK_17%")
        param("kotlinVersion", "2.1.20")
        param("version", "7.0.0-SNAPSHOT")
    }

    steps {
        gradle {
            name = "Build and Test"
            tasks = "check antora"
            gradleWrapperPath = "gradlew"
            buildFile = "build.gradle"
            gradleParams = "-Xmx2048m --parallel"
        }
    }

    triggers {
        vcs {
            branchFilter = "+:*"
        }
        schedule {
            schedulingPolicy = daily {
                hour = 9
                minute = 30
            }
            branchFilter = "+:*"
        }
    }

    requirements {
        contains("teamcity.agent.jvm.os.name", "Linux")
        contains("teamcity.agent.jvm.version", "17")
    }

    artifactRules = """
        +:build/reports/** => build-reports
        +:build/test-results/** => test-results
        +:build/libs/** => artifacts
    """.trimIndent()
})

project {
    vcsRoot(SpringFrameworkVcs)
    buildType(MainCIBuild)
}
