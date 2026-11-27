import { authApi } from "./api.js";

const form = document.querySelector("#auth-form");
const emailInput = document.querySelector("#auth-email");
const passwordInput = document.querySelector("#auth-password");
const passwordToggle = document.querySelector("#password-toggle");
const submitButton = document.querySelector("#auth-submit");
const message = document.querySelector("#auth-message");
const mode = form.dataset.authMode;

function showMessage(text, type = "error") {
  message.textContent = text;
  message.className = `auth-message auth-message-${type}`;
  message.hidden = !text;
}

passwordToggle.addEventListener("click", () => {
  const isHidden = passwordInput.type === "password";
  passwordInput.type = isHidden ? "text" : "password";
  passwordToggle.textContent = isHidden ? "Hide" : "Show";
  passwordToggle.setAttribute("aria-label", isHidden ? "Hide password" : "Show password");
  passwordToggle.setAttribute("aria-pressed", String(isHidden));
});

form.addEventListener("submit", async event => {
  event.preventDefault();
  if (!form.reportValidity()) return;

  const credentials = {
    email: emailInput.value.trim(),
    password: passwordInput.value
  };
  submitButton.disabled = true;
  submitButton.textContent = mode === "register" ? "Creating account..." : "Signing in...";
  showMessage("");

  try {
    if (mode === "register") {
      await authApi.register(credentials);
      window.location.replace("./login.html?registered=1");
    } else {
      await authApi.login(credentials);
      window.location.replace("./index.html");
    }
  } catch (error) {
    showMessage(error.message || "The pantry service is unavailable. Try again.");
  } finally {
    submitButton.disabled = false;
    submitButton.textContent = mode === "register" ? "Create account" : "Sign in";
  }
});

async function initializeAuthPage() {
  const registered = new URLSearchParams(window.location.search).get("registered") === "1";
  if (mode === "login" && registered) {
    showMessage("Your account is ready. Sign in to open your pantry.", "success");
  }

  try {
    await authApi.csrf();
    await authApi.me();
    window.location.replace("./index.html");
  } catch (error) {
    if (error.status !== 401) {
      showMessage("The pantry service is unavailable. Check that the API is running, then try again.");
    }
  }
}

initializeAuthPage();
