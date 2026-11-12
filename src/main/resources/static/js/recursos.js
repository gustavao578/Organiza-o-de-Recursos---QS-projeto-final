import { apiGet, apiPost } from "./api.js";

function node(tagName, text, className) {
  const element = document.createElement(tagName);
  if (text !== undefined) {
    element.textContent = text;
  }
  if (className) {
    element.className = className;
  }
  return element;
}

function showFeedback(container, message, kind = "info") {
  const feedback = node("p", message, `feedback${kind === "error" ? " feedback-error" : ""}`);
  feedback.setAttribute("role", "status");
  const existing = container.querySelector(".feedback");
  existing?.remove();
  container.prepend(feedback);
}

export async function mountResources(container, profile) {
  container.replaceChildren();

  if (profile === "ADMINISTRADOR") {
    const form = node("form");
    form.className = "form-grid";
    form.setAttribute("aria-label", "Criar recurso");
    const nameLabel = node("label", "Nome do recurso");
    const name = node("input");
    name.name = "nome";
    name.required = true;
    name.maxLength = 150;
    nameLabel.append(name);

    const typeLabel = node("label", "Tipo");
    const type = node("input");
    type.name = "tipo";
    type.required = true;
    type.maxLength = 80;
    typeLabel.append(type);

    const restrictedLabel = node("label", "Requer aprovação");
    restrictedLabel.className = "checkbox-field form-wide";
    const restricted = node("input");
    restricted.type = "checkbox";
    restricted.name = "restrito";
    restrictedLabel.append(restricted, document.createTextNode("Recurso restrito"));

    const submit = node("button", "Criar recurso", "button form-wide");
    submit.type = "submit";
    form.append(nameLabel, typeLabel, restrictedLabel, submit);
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      submit.disabled = true;
      try {
        await apiPost("/recursos", {
          nome: name.value.trim(),
          tipo: type.value.trim(),
          restrito: restricted.checked,
        });
        form.reset();
        showFeedback(container, "Recurso criado.", "success");
        await loadList();
      } catch (error) {
        showFeedback(container, error.message, "error");
      } finally {
        submit.disabled = false;
      }
    });
    container.append(form);
  }

  const reload = node("button", "Atualizar lista", "button button-secondary");
  reload.type = "button";
  reload.addEventListener("click", () => loadList());
  container.append(reload);

  const list = node("ul");
  list.className = "item-list";
  list.setAttribute("aria-label", "Lista de recursos");
  container.append(list);

  async function loadList() {
    list.replaceChildren(node("li", "Carregando recursos…", "empty-state"));
    try {
      const resources = await apiGet("/recursos");
      list.replaceChildren();
      if (!Array.isArray(resources) || resources.length === 0) {
        list.append(node("li", "Nenhum recurso cadastrado.", "empty-state"));
        return;
      }
      resources.forEach((resource) => {
        const item = node("li");
        item.className = "item-card";
        const title = node("h3", resource.nome || "Recurso sem nome");
        const typeText = node("p", `Tipo: ${resource.tipo || "não informado"}`);
        const access = node("p", resource.restrito ? "Acesso restrito" : "Acesso livre");
        item.append(title, typeText, access);
        list.append(item);
      });
    } catch (error) {
      list.replaceChildren(node("li", error.message, "feedback feedback-error"));
    }
  }

  await loadList();
}
