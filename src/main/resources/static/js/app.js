import { clearSession, getSession, startSession } from "./auth.js";
import { apiPost } from "./api.js";
import { mountMaintenance } from "./manutencoes.js";
import { mountReports } from "./relatorios.js";
import { mountResources } from "./recursos.js";
import { mountReservations } from "./reservas.js";
import { mountUsers } from "./usuarios.js";

const loginView = document.querySelector("#login-view");
const dashboardView = document.querySelector("#dashboard-view");
const loginForm = document.querySelector("#login-form");
const manualTokenForm = document.querySelector("#manual-token-form");
const loginIdentifier = document.querySelector("#login-identifier");
const tokenField = document.querySelector("#jwt-token");
const appMessage = document.querySelector("#app-message");
const logoutButton = document.querySelector("#logout-button");
const sessionIdentity = document.querySelector("#session-identity");
const tabButtons = [...document.querySelectorAll(".tab-button")];
const contentViews = [...document.querySelectorAll(".content-view")];
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
  const selectedTab = tabButtons.find((button) => (
    button.dataset.view === viewId && !button.hidden
  ));
  if (!selectedTab) {
    viewId = "resources-view";
  }
  selectedView = viewId;
  contentViews.forEach((view) => {
    view.hidden = view.id !== viewId;
  });
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
  sessionIdentity.textContent = `${session.name} · ${session.profile}`;
  const administrator = session.profile === "ADMINISTRADOR";
  const managementProfile = administrator || session.profile === "RESPONSAVEL";
  const usersTab = tabButtons.find((button) => button.dataset.view === "users-view");
  usersTab.textContent = administrator ? "Usuários" : "Meu cadastro";
  tabButtons.find((button) => button.dataset.view === "maintenance-view").hidden = !managementProfile;
  tabButtons.find((button) => button.dataset.view === "reports-view").hidden = !managementProfile;
  showView(selectedView);

  const modules = [
    ["#resources-content", mountResources],
    ["#reservations-content", mountReservations],
    ["#users-content", mountUsers],
  ];
  if (managementProfile) {
    modules.push(["#maintenance-content", mountMaintenance], ["#reports-content", mountReports]);
  }
  await Promise.all(modules.map(async ([selector, mount]) => {
    const container = document.querySelector(selector);
    try {
      await mount(container, session);
    } catch (error) {
      console.error(`Falha ao carregar ${selector}:`, error);
      if (container) {
        container.textContent = "Não foi possível carregar esta seção. Tente novamente.";
      } else {
        showMessage(`Não foi possível carregar a seção ${selector}.`, "error");
      }
    }
  }));
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
  const submit = loginForm.querySelector("button[type='submit']");
  submit.disabled = true;
  try {
    const response = await apiPost("/auth/login", {
      identificador: loginIdentifier.value.trim(),
    });
    if (!startSession(response?.token || "")) {
      throw new Error("O servidor retornou um token inválido ou expirado.");
    }
    loginIdentifier.value = "";
    await renderSession();
  } catch (error) {
    showMessage(error.message, "error");
  } finally {
    submit.disabled = false;
  }
});

manualTokenForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  hideMessage();
  const token = tokenField.value.trim();
  tokenField.value = "";
  if (!startSession(token)) {
    showMessage("Token inválido, expirado ou armazenamento de sessão indisponível.", "error");
    return;
  }
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
