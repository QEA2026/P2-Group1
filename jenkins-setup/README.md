# STEPS
1. Open jenkins-setup folder
2. docker compose up -d to start jenkins
3. Get Initial Admin Password:  docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
    Save this password! You'll need it in the next step.
4. Open browser: http://localhost:8080
5. Paste the initial admin password. Click "Continue"
6. Install Plugins
    Select "Install suggested plugins"
7. Create Admin User
    Username: admin
    Password: admin123
    Confirm password: admin123
    Full name: Jenkins Admin
    Email: admin@example.com
8. Instance Configuration
    Jenkins URL: http://localhost:8080/
    Click "Save and Finish"
9. Start Using Jenkins
10. Start Using Jenkins
    Click "Start using Jenkins"
    You're now on the Jenkins Dashboard!

# INSTALL ADDITIONAL PLUGINS
1. Navigate to Plugin Manager
    Manage Jenkins (Cogwheel) → Plugins → Available plugins
2. Search and Install These Plugins. If the exact name does not appear, they must be already installed. Check installed plugins
    Docker (if not installed)
    Docker Pipeline
    Pipeline Stage View
    Timestamper
    Workspace Cleanup
3. Install Without Restart
    Check desired plugins
    Click "Install without restart"
4. Verify Installation
    Go to "Installed plugins"
    Confirm plugins are listed