import assert from "node:assert/strict";
import test from "node:test";
import { getSession, startSession } from "../../main/resources/static/js/auth.js";
import { mountResources } from "../../main/resources/static/js/recursos.js";
import { mountReservations } from "../../main/resources/static/js/reservas.js";

class Element {
  constructor(tagName) {
    this.tagName = tagName;
    this.children = [];
    this.attributes = {};
    this.handlers = {};
    this.value = "";
    this.classList = { add() {} };
  }
  append(...children) { this.children.push(...children); }
  prepend(child) { this.children.unshift(child); }
  replaceChildren(...children) { this.children = children; }
  setAttribute(name, value) { this.attributes[name] = value; }
  addEventListener(name, handler) { this.handlers[name] = handler; }
  querySelector() { return null; }
}

globalThis.document = {
  createElement: (tagName) => new Element(tagName),
  createTextNode: (text) => ({ textContent: text }),
};
let storedToken = null;
globalThis.sessionStorage = {
  getItem: () => storedToken,
  setItem: (_key, value) => { storedToken = value; },
  removeItem: () => { storedToken = null; },
};

function token(claims) {
  return `header.${Buffer.from(JSON.stringify(claims)).toString("base64url")}.signature`;
}

test("session displays name, email or subject in that order", () => {
  for (const [claims, expected] of [
    [{ name: "Nome", email: "email@example.test" }, "Nome"],
    [{ email: "email@example.test" }, "email@example.test"],
    [{}, "id-1"],
  ]) {
    assert.equal(startSession(token({ sub: "id-1", profile: "SOLICITANTE", ...claims })), true);
    assert.equal(getSession().name, expected);
  }
});

test("resource and reservation filters send selected query parameters", async () => {
  const requests = [];
  globalThis.fetch = async (path) => {
    requests.push(path);
    const body = path === "/recursos" ? [{ id: "resource-id", nome: "Sala" }] : [];
    return new Response(JSON.stringify(body), { headers: { "content-type": "application/json" } });
  };
  const session = { subject: "id-1", profile: "RESPONSAVEL" };
  const resourceContainer = new Element("div");
  await mountResources(resourceContainer, session);
  const resourceForm = resourceContainer.children.find((child) =>
    child.attributes?.["aria-label"] === "Filtrar recursos");
  resourceForm.children[0].children[0].value = "Sala & Lab";
  resourceForm.children[1].children[0].value = "false";
  resourceForm.handlers.submit({ preventDefault() {} });
  await new Promise(setImmediate);
  assert.equal(requests.at(-1), "/recursos?tipo=Sala+%26+Lab&restrito=false");

  const reservationContainer = new Element("div");
  await mountReservations(reservationContainer, session);
  const reservationForm = reservationContainer.children.find((child) =>
    child.attributes?.["aria-label"] === "Filtrar reservas");
  reservationForm.children[0].children[0].value = "NÃO_COMPARECEU";
  reservationForm.children[1].children[0].value = "resource-id";
  reservationForm.handlers.submit({ preventDefault() {} });
  await new Promise(setImmediate);
  assert.equal(requests.at(-1), "/reservas?status=N%C3%83O_COMPARECEU&recursoId=resource-id");
});
