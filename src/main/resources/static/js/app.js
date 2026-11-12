import { clearSession, getSession, startSession } from "./auth.js";
import { mountResources } from "./recursos.js";
import { mountReservations } from "./reservas.js";

const loginView = document.querySelector("#login-view");
const dashboardView = document.querySelector("#dashboard-view");
const loginForm = document.querySelector("#login-form");
const tokenField = document.querySelector("#jwt-token");
const appMessage = document.querySelector("#app-message");
const logoutButton = document.querySelector("#logout-button");
const sessionIdentity = document.querySelector("#session-identity");
const resourceView = document.querySelector("#resources-view");
const reservationsView = document.querySelector("#reservations-view");
const tabButtons = [...document.querySelectorAll(".tab-button")];
let selectedView = "resources-view";

function showMessage(message, kind = "info") {
  appMessage.textContent = message;
  appMessage.className = `app-message${kind === "error" ? " feedback-error" : ""}`;
  appMessage.hidden = false;
}

function hideMessage() {
  appMessage.textContent = "";
  appMessage.hidden = true;
}

function showView(viewId) {
  selectedView = viewId;
  resourceView.hidden = viewId !== "resources-view";
  reservationsView.hidden = viewId !== "reservations-view";
  tabButtons.forEach((button) => {
    if (button.dataset.view === viewId) {
      button.setAttribute("aria-current", "page");
    } else {
      button.removeAttribute("aria-current");
    }
  });
}

async function showPanel(session) {
  loginView.hidden = true;
  dashboardView.hidden = false;
  logoutButton.hidden = false;
  sessionIdentity.hidden = false;
  sessionIdentity.textContent = `${session.subject} · ${session.profile}`;
  showView(selectedView);

  await Promise.all([
    mountResources(document.querySelector("#resources-content"), session.profile),
    mountReservations(document.querySelector("#reservations-content"), session.profile),
  ]);
}

async function renderSession() {
  const session = getSession();
  if (!session) {
    loginView.hidden = false;
    dashboardView.hidden = true;
    logoutButton.hidden = true;
    sessionIdentity.hidden = true;
    tokenField.value = "";
    return;
  }
  await showPanel(session);
}

loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  hideMessage();
  const token = tokenField.value.trim();
  tokenField.value = "";
  if (!startSession(token)) {
    showMessage("Token inválido ou armazenamento de sessão indisponível. Confira o JWT e tente novamente.", "error");
    return;
  }
  hideMessage();
  await renderSession();
});

logoutButton.addEventListener("click", () => {
  clearSession();
  selectedView = "resources-view";
  hideMessage();
  void renderSession();
});

tabButtons.forEach((button) => {
  button.addEventListener("click", () => showView(button.dataset.view));
});

window.addEventListener("session-expired", () => {
  void renderSession();
  showMessage("Sessão inválida ou expirada. Entre novamente.", "error");
});

void renderSession();
