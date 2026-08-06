import os
from pathlib import Path

from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.support.ui import WebDriverWait


def before_scenario(context, scenario):
    """Create a fresh browser before every scenario."""

    chrome_options = Options()

    selenium_url = os.getenv("SELENIUM_URL")

    if selenium_url:
        # Docker / Jenkins
        chrome_options.add_argument("--headless")
        chrome_options.add_argument("--no-sandbox")
        chrome_options.add_argument("--disable-dev-shm-usage")

        context.driver = webdriver.Remote(
            command_executor=selenium_url,
            options=chrome_options
        )

        context.base_url = os.getenv(
            "APP_URL",
            "http://employee-app:5000/app"
        )

    else:
        # Local VS Code
        chrome_options.add_argument("--start-maximized")

        context.driver = webdriver.Chrome(
            options=chrome_options
        )

        context.base_url = "http://127.0.0.1:5000/app"

    context.wait = WebDriverWait(
        context.driver,
        10
    )

    # Replace these with a valid employee login.
    context.employee_username = "Bob"
    context.employee_password = "bob_22"

    # Use recognizable values so the test can find its own expense.
    context.original_description = (
        "E2E Original Meals Expense"
    )

    context.updated_description = (
        "E2E Modified Travel Expense"
    )


def after_scenario(context, scenario):
    """Take a screenshot after failure and always close Chrome."""

    if hasattr(context, "driver"):
        if scenario.status == "failed":
            screenshot_directory = Path(
                "test-results/screenshots"
            )

            screenshot_directory.mkdir(
                parents=True,
                exist_ok=True
            )

            safe_name = scenario.name.replace(
                " ",
                "_"
            )

            screenshot_path = (
                screenshot_directory
                / f"{safe_name}.png"
            )

            context.driver.save_screenshot(
                str(screenshot_path)
            )

        context.driver.quit()