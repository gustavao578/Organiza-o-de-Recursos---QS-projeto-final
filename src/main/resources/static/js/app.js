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
const demoChipButtons = [...document.querySelectorAll(".chip-button[data-login]")];

let selectedView = "resources-view";

function showMessage(message, kind = "info") {
  appMessage.textContent = message;
  appMessage.className = `app-message${kind === "error" ? " feedback-error" : kind === "success" ? " feedback-success" : ""}`;
  appMessage.hidden = false;
}

function hideMessage() {
  appMessage.textContent = "";
  appMessage.hidden = true;
}

function defaultViewForProfile(profile) {
  if (profile === "SOLICITANTE") {
    return "reservations-view";
  }
  if (profile === "RESPONSAVEL") {
    return "reservations-view";
  }
  return "resources-view";
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

async function safeMount(fn) {
  try {
    await fn();
  } catch (error) {
    console.error("Erro ao carregar seção:", error);
  }
}

async function showPanel(session) {
  loginView.hidden = true;
  dashboardView.hidden = false;
  logoutButton.hidden = false;
  sessionIdentity.hidden = false;
  sessionIdentity.textContent = `${session.name || session.subject} · ${session.profile}`;

  const administrator = session.profile === "ADMINISTRADOR";
  const managementProfile = administrator || session.profile === "RESPONSAVEL";
  const usersTab = tabButtons.find((button) => button.dataset.view === "users-view");
  if (usersTab) {
    usersTab.textContent = administrator ? "Usuários" : "Meu cadastro";
  }

  const maintenanceTab = tabButtons.find((button) => button.dataset.view === "maintenance-view");
  if (maintenanceTab) maintenanceTab.hidden = !managementProfile;

  const reportsTab = tabButtons.find((button) => button.dataset.view === "reports-view");
  if (reportsTab) reportsTab.hidden = !managementProfile;

  showView(selectedView);

  await Promise.all([
    safeMount(() => mountResources(document.querySelector("#resources-content"), session)),
    safeMount(() => mountReservations(document.querySelector("#reservations-content"), session)),
    safeMount(() => mountUsers(document.querySelector("#users-content"), session)),
    managementProfile
      ? safeMount(() => mountMaintenance(document.querySelector("#maintenance-content"), session))
      : Promise.resolve(),
    managementProfile
      ? safeMount(() => mountReports(document.querySelector("#reports-content")))
      : Promise.resolve(),
  ]);
}

async function renderSession() {
  const session = getSession();
  if (!session) {
    loginView.hidden = false;
    dashboardView.hidden = true;
    logoutButton.hidden = true;
    sessionIdentity.hidden = true;
    if (tokenField) tokenField.value = "";
    return;
  }
  await showPanel(session);
}

demoChipButtons.forEach((chip) => {
  chip.addEventListener("click", () => {
    loginIdentifier.value = chip.dataset.login || "";
    loginForm.requestSubmit();
  });
});

loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  hideMessage();
  const submit = loginForm.querySelector("button[type='submit']");
  submit.disabled = true;
  try {
    const identificador = loginIdentifier.value.trim();
    const response = await apiPost("/auth/login", { identificador });
    if (!startSession(response?.token || "", response?.usuario || null)) {
      throw new Error("O servidor retornou um token inválido ou expirado.");
    }
    const activeSession = getSession();
    selectedView = defaultViewForProfile(activeSession?.profile);
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
  const activeSession = getSession();
  selectedView = defaultViewForProfile(activeSession?.profile);
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