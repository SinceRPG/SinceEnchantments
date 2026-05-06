pipeline {
    agent any

    environment {
        // --- DISCORD CONFIGURATION ---
        // Using the Webhook and Thread ID provided in your .gitlab-ci.yml
        DISCORD_WEBHOOK_URL = "https://discord.com/api/webhooks/1471469221614583810/pfMtLyRbTDKiUGMJyVBjhhJ3RDgQelOX71iMGqWg3HdrlokqBJSt1Ox3aC4yTkkGtZ-_"
        THREAD_ID = "1492096066340786287"

        // --- ASSETS ---
        ICON_URL = "https://gitlab.com/uploads/-/system/group/avatar/121690756/SinceRPG.png?width=48"
        THUMBNAIL_URL = "https://gitlab.com/uploads/-/system/group/avatar/121690756/SinceRPG.png?width=48"
        BOT_NAME = "SinceEnchantments Build"

        // --- COLORS (Decimal) ---
        COLOR_PENDING = "16766720" // Yellow
        COLOR_SUCCESS = "5763719"  // Green
        COLOR_FAIL = "15548997"    // Red

        // --- TEXTS ---
        NO_CHANGELOG_TEXT = "No specific changelog provided."
        FAIL_DESC_TEXT = "A syntax or compilation error occurred. Please check the Jenkins Console logs."
    }

    stages {
        stage('Prepare & Build') {
            steps {
                script {
                    // Ensure the gradlew wrapper is executable in the Docker/Linux environment
                    sh 'chmod +x gradlew'

                    // 1. Notify Discord that the build has started
                    sh 'python3 build.py --start || echo "Discord Start Notify Failed"'

                    try {
                        // 2. Execute the Gradle build process
                        sh './gradlew clean build'
                    } catch (Exception e) {
                        // 3. If build fails, update Discord message to RED
                        sh 'python3 build.py --fail'
                        error "Build failed: ${e.message}"
                    }
                }
            }
        }

        stage('Finalize') {
            steps {
                script {
                    // 4. If successful, update Discord message to GREEN and attach the JAR
                    sh 'python3 build.py'
                }
            }
        }
    }

    post {
        always {
            // Clean the workspace to save disk space
            cleanWs()
        }
    }
}