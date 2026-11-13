import assert from "node:assert/strict";
import test from "node:test";

class Element {
  constructor(id = "") {
    this.id = id;
    this.children = [];
    this.handlers = {};
    this.attributes = {};
    this.dataset = {};
    this.hidden = false;
    this.value = "";
    this.classList = { add() {} };
  }
  append(...children) { this.children.push(...children); }
  prepend(...children) { this.children.unshift(...children); }
  after() {}
  replaceChildren(...children) {
    if (this.failMount) throw new Error("Falha simulada");
    this.children = children;
  }
  setAttribute(name, value) { this.attributes[name] = value; }
  removeAttribute(name) { delete this.attributes[name]; }
  addEventListener(name, handler) { this.handlers[name] = handler; }
  querySelector(selector) {
    return selector === "button[type='submit']" ? this.submit : null;
  }
}

const ids = ["login-view", "dashboard-view", "login-form", "manual-token-form",
  "login-identifier", "jwt-token", "app-message", "logout-button", "session-identity",
  "resources-content", "reservations-content", "users-content", "maintenance-content", "reports-content"];
const elements = Object.fromEntries(ids.map((id) => [id, new Element(id)]));
elements["login-form"].submit = new Element();
const viewIds = ["resources-view", "reservations-view", "users-view", "maintenance-view", "reports-view"];
const views = viewIds.map((id) => new Element(id));
const tabs = viewIds.map((id) => {
  const tab = new Element();
  tab.dataset.view = id;
  return tab;
});
globalThis.document = {
  querySelector: (selector) => elements[selector.slice(1)] || null,
  querySelectorAll: (selector) => selector === ".tab-button" ? tabs : views,
  createElement: () => new Element(),
  createTextNode: (text) => ({ textContent: text }),
};
const listeners = {};
globalThis.window = {
  addEventListener: (name, handler) => { listeners[name] = handler; },
  dispatchEvent: (event) => listeners[event.type]?.(event),
};
let storedToken = null;
globalThis.sessionStorage = {
  getItem: () => storedToken,
  setItem: (_key, value) => { storedToken = value; },
  removeItem: () => { storedToken = null; },
};

const accounts = {
  "admin@organizacao.edu.br": ["ADMINISTRADOR", "Administrador"],
  "responsavel@organizacao.edu.br": ["RESPONSAVEL", "Responsável Laboratório"],
  "solicitante@organizacao.edu.br": ["SOLICITANTE", "Aluno Solicitante"],
};
const nativeFetch = globalThis.fetch;
globalThis.fetch = async (path, options) => {
  if (process.env.UI_TEST_BASE_URL) return nativeFetch(new URL(path, process.env.UI_TEST_BASE_URL), options);
  let payload = [];
  if (path === "/auth/login") {
    const [profile, name] = accounts[JSON.parse(options.body).identificador];
    const claims = { sub: "id-1", profile, name, exp: Math.floor(Date.now() / 1000) + 3600 };
    payload = { token: `header.${Buffer.from(JSON.stringify(claims)).toString("base64url")}.signature` };
  } else if (path.startsWith("/usuarios/")) {
    payload = { id: "id-1", nome: "Nome", email: "email@example.test", matricula: "M1" };
  }
  return new Response(JSON.stringify(payload), { headers: { "content-type": "application/json" } });
};

await import("../../main/resources/static/js/app.js");

for (const [email, [profile, name]] of Object.entries(accounts)) {
  test(`login displays dashboard and allowed tabs for ${email}`, async () => {
    elements["logout-button"].handlers.click();
    elements["login-identifier"].value = email;
    await elements["login-form"].handlers.submit({ preventDefault() {} });
    assert.equal(elements["login-view"].hidden, true);
    assert.equal(elements["dashboard-view"].hidden, false);
    assert.equal(elements["session-identity"].textContent, `${name} · ${profile}`);
    assert.ok(storedToken);
    assert.notEqual(elements["resources-content"].textContent,
      "Não foi possível carregar esta seção. Tente novamente.");
    assert.equal(views[0].hidden, false);
    assert.equal(tabs[2].textContent, profile === "ADMINISTRADOR" ? "Usuários" : "Meu cadastro");
    assert.equal(tabs[3].hidden, profile === "SOLICITANTE");
    assert.equal(tabs[4].hidden, profile === "SOLICITANTE");
    tabs[1].handlers.click();
    assert.equal(views[1].hidden, false);
    assert.equal(views[0].hidden, true);
  });
}

test("dashboard is visible before initial module requests finish", async () => {
  elements["logout-button"].handlers.click();
  elements["login-identifier"].value = "solicitante@organizacao.edu.br";
  const originalFetch = globalThis.fetch;
  let release;
  const blocked = new Promise((resolve) => { release = resolve; });
  let requestStarted;
  const started = new Promise((resolve) => { requestStarted = resolve; });
  globalThis.fetch = async (path, options) => {
    if (path !== "/auth/login") {
      requestStarted();
      await blocked;
    }
    return originalFetch(path, options);
  };
  try {
    const submission = elements["login-form"].handlers.submit({ preventDefault() {} });
    await started;
    assert.equal(elements["login-view"].hidden, true);
    assert.equal(elements["dashboard-view"].hidden, false);
    release();
    await submission;
  } finally {
    release();
    globalThis.fetch = originalFetch;
  }
});

test("a failed module does not prevent login or navigation", async () => {
  elements["logout-button"].handlers.click();
  elements["resources-content"].failMount = true;
  elements["login-identifier"].value = "admin@organizacao.edu.br";
  const originalError = console.error;
  const errors = [];
  console.error = (...args) => errors.push(args);
  try {
    await elements["login-form"].handlers.submit({ preventDefault() {} });
  } finally {
    console.error = originalError;
    elements["resources-content"].failMount = false;
  }
  assert.equal(errors.length, 1);
  assert.equal(elements["login-view"].hidden, true);
  assert.equal(elements["dashboard-view"].hidden, false);
  assert.match(elements["resources-content"].textContent, /Não foi possível carregar/);
  tabs[1].handlers.click();
  assert.equal(views[1].hidden, false);
});
