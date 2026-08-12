FROM jenkins/jenkins:lts

USER root

RUN apt-get update && \
    apt-get install -y \
        ca-certificates \
        curl && \
    rm -rf /var/lib/apt/lists/*

# Add Docker's official Debian repository
RUN install -m 0755 -d /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/debian/gpg \
        -o /etc/apt/keyrings/docker.asc && \
    chmod a+r /etc/apt/keyrings/docker.asc && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian \
        $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
        > /etc/apt/sources.list.d/docker.list

# Install Docker CLI, Compose, and Buildx
RUN apt-get update && \
    apt-get install -y \
        docker-ce-cli \
        docker-compose-plugin \
        docker-buildx-plugin && \
    dpkg --configure -a && \
    apt-get -f install -y && \
    rm -rf /var/lib/apt/lists/*
    
EXPOSE 5000

USER jenkins