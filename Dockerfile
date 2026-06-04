FROM maven:3.9.6-eclipse-temurin-21

LABEL maintainer="CRD QA Assessment"
LABEL description="Portfolio Rebalancing Calculator - Selenium TestNG test suite"
LABEL version="1.0.0"

# Install Google Chrome (amd64)
RUN apt-get update && apt-get install -y \
    wget \
    gnupg \
    fonts-liberation \
    libasound2 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libcups2 \
    libdbus-1-3 \
    libgdk-pixbuf2.0-0 \
    libnspr4 \
    libnss3 \
    libx11-xcb1 \
    libxcomposite1 \
    libxdamage1 \
    libxrandr2 \
    xdg-utils \
    --no-install-recommends \
    && wget -q -O - https://dl.google.com/linux/linux_signing_key.pub \
       | gpg --dearmor > /usr/share/keyrings/google-chrome.gpg \
    && echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-chrome.gpg] \
       http://dl.google.com/linux/chrome/deb/ stable main" \
       > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable --no-install-recommends \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Cache Maven dependencies — only re-downloads when pom.xml changes
COPY pom.xml .
RUN mvn dependency:go-offline --no-transfer-progress

# Copy the rest of the project
COPY . .

# Run all tests and generate Allure report
CMD ["mvn", "test", "allure:report", "--no-transfer-progress"]
