import { apiDelete, apiGet, apiPost, apiPut } from "./api.js";

function node(tagName, text, className) {
  const element = document.createElement(tagName);
  if (text !== undefined) element.textContent = text;
  if (className) element.className = className;
  return element;
}

function feedback(container, message, kind = "info") {
  container.querySelector(".feedback")?.remove();
  const element = node(
    "p",
    message,
    `feedback${kind === "error" ? " feedback-error" : kind === "success" ? " feedback-success" : ""}`,
  );
  element.setAttribute("role", "status");
  container.prepend(element);
}

export async function mountResources(container, session) {
  container.replaceChildren();
  const administrator = session.profile === "ADMINISTRADOR";
  const list = node("ul");
  list.className = "item-list";
  list.setAttribute("aria-label", "Lista de recursos");

  if (administrator) {
    let responsibleUsers = [];
    try {
      const users = await apiGet("/usuarios");
      responsibleUsers = users.filter((user) => user.perfil === "RESPONSAVEL");
    } catch (error) {
      feedback(container, `Não foi possível carregar responsáveis: ${error.message}`, "error");
    }

    const form = node("form");
    form.className = "form-grid";
    form.setAttribute("aria-label", "Criar ou editar recurso");
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

    const responsibleLabel = node("label", "Responsável (opcional)");
    const responsible = node("select");
    responsible.name = "responsavelId";
    const noResponsible = node("option", "Sem responsável");
    noResponsible.value = "";
    responsible.append(noResponsible);
    responsibleUsers.forEach((user) => {
      const option = node("option", `${user.nome} (${user.email})`);
      option.value = user.id;
      responsible.append(option);
    });
    responsibleLabel.append(responsible);

    const restrictedLabel = node("label", "Recurso restrito");
    restrictedLabel.className = "checkbox-field form-wide";
    const restricted = node("input");
    restricted.type = "checkbox";
    restricted.name = "restrito";
    restrictedLabel.append(restricted, document.createTextNode("Requer aprovação"));

    const submit = node("button", "Criar recurso", "button form-wide");
    submit.type = "submit";
    const cancel = node("button", "Cancelar edição", "button button-secondary form-wide");
    cancel.type = "button";
    cancel.hidden = true;
    form.append(nameLabel, typeLabel, responsibleLabel, restrictedLabel, submit, cancel);
    container.append(form);

    let editingId = null;
    function resetForm() {
      editingId = null;
      form.reset();
      submit.textContent = "Criar recurso";
      cancel.hidden = true;
    }

    cancel.addEventListener("click", resetForm);
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      submit.disabled = true;
      const payload = {
        nome: name.value.trim(),
        tipo: type.value.trim(),
        responsavelId: responsible.value || null,
        restrito: restricted.checked,
      };
      try {
        if (editingId) {
          await apiPut(`/recursos/${editingId}`, payload);
          feedback(container, "Recurso atualizado.", "success");
        } else {
          await apiPost("/recursos", payload);
          feedback(container, "Recurso criado.", "success");
        }
        resetForm();
        await loadList();
      } catch (error) {
        feedback(container, error.message, "error");
      } finally {
        submit.disabled = false;
      }
    });

    function beginEdit(resource) {
      editingId = resource.id;
      name.value = resource.nome || "";
      type.value = resource.tipo || "";
      responsible.value = resource.responsavelId || "";
      restricted.checked = Boolean(resource.restrito);
      submit.textContent = "Salvar alterações";
      cancel.hidden = false;
      name.focus();
    }

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
          item.append(
            node("h3", resource.nome || "Recurso sem nome"),
            node("p", `Tipo: ${resource.tipo || "não informado"}`),
            node("p", resource.restrito ? "Acesso restrito" : "Acesso livre"),
          );
          const responsible = responsibleUsers.find((user) => user.id === resource.responsavelId);
          if (responsible) item.append(node("p", `Responsável: ${responsible.nome}`));

          const edit = node("button", "Editar", "button button-secondary");
          edit.type = "button";
          edit.addEventListener("click", () => beginEdit(resource));
          const remove = node("button", "Excluir", "button button-secondary");
          remove.type = "button";
          remove.addEventListener("click", async () => {
            if (!window.confirm(`Excluir o recurso "${resource.nome}"?`)) return;
            try {
              await apiDelete(`/recursos/${resource.id}`);
              feedback(container, "Recurso excluído.", "success");
              await loadList();
            } catch (error) {
              feedback(container, error.message, "error");
            }
          });
          item.append(edit, remove);
          list.append(item);
        });
      } catch (error) {
        list.replaceChildren(node("li", error.message, "feedback feedback-error"));
      }
    }

    const reload = node("button", "Atualizar lista", "button button-secondary");
    reload.type = "button";
    reload.addEventListener("click", () => loadList());
    form.after(reload);
    await loadList();
    return;
  }

  const reload = node("button", "Atualizar lista", "button button-secondary");
  reload.type = "button";
  reload.addEventListener("click", () => loadList());
  container.append(reload, list);

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
        item.append(
          node("h3", resource.nome || "Recurso sem nome"),
          node("p", `Tipo: ${resource.tipo || "não informado"}`),
          node("p", resource.restrito ? "Acesso restrito" : "Acesso livre"),
        );
        list.append(item);
      });
    } catch (error) {
      list.replaceChildren(node("li", error.message, "feedback feedback-error"));
    }
  }

  await loadList();
}
