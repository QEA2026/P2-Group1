# Project Requirements Document: Revature Expense Manager – Phase 3

## Overview
In Phase 3, **Revature Expense Manager** transitions into a fully containerized and automated deployment pipeline. The focus is on integrating the existing Python and Java web applications, along with their testing suites, into a CI/CD pipeline using Docker and Jenkins. The pipeline will designed for hosting on AWS and designed to build, test, and deploy both applications automatically. Due to cost constraints the deployment on AWS will not occur, however, but architecture and design will be complete.

---

## Technologies Used
- **Docker** – Containerization of both Python and Java applications
- **Jenkins** – CI/CD orchestration
- **AWS** – Cloud hosting for Jenkins and deployment targets
- **Git** – Source control and trigger for pipeline execution

---

## CI/CD MVP Requirements

### Containerization
- Each application (Python and Java) must be containerized using Docker
- Dockerfiles must:
  - Install dependencies
  - Expose necessary ports
  - Run the application in a production-ready state
- Docker Compose may be used to orchestrate multi-container setups (e.g., app + database)

### CI/CD Pipeline (Jenkins)
#### (in design only, associates can implement but no need to keep running for an implentation for entire project)
- Jenkins must be configured to:
  - Pull code from Git repository
  - Build Docker images for both applications
  - Run unit and API tests
  - Deploy containers to a target environment
- Pipelines should be triggered by Git events

### Testing Integration
- Existing unit, API, performance, and E2E tests must be executed as part of the pipeline
- Test results should be visible in Jenkins (e.g., via plugins or console output)

### AWS Hosting
#### (in design only, associates can implement but no need to keep running for an implentation for entire project)
- Jenkins must be hosted on AWS 
- Credentials and secrets must be managed securely 

---

## Deliverables
- Dockerfiles for both applications
- Jenkinsfile or Jenkins pipeline configuration
- AWS-hosted Jenkins instance (design and architectural details, but does not need to be implemented)
- CI/CD pipeline that builds, tests, and deploys both apps (discussion and details but not actually deployed)
- Documentation outlining setup, pipeline flow, and deployment steps
