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

export async function mountMaintenance(container, session) {
  container.replaceChildren();
  const form = node("form");
  form.className = "form-grid";
  form.setAttribute("aria-label", "Criar ou editar manutenção");
  const resourceLabel = node("label", "Recurso");
  const resourceSelect = node("select");
  resourceSelect.required = true;
  resourceLabel.append(resourceSelect);

  const startLabel = node("label", "Início");
  const start = node("input");
  start.type = "datetime-local";
  start.step = "1";
  start.required = true;
  startLabel.append(start);

  const endLabel = node("label", "Fim");
  const end = node("input");
  end.type = "datetime-local";
  end.step = "1";
  end.required = true;
  endLabel.append(end);

  const descriptionLabel = node("label", "Descrição");
  const description = node("input");
  description.required = true;
  descriptionLabel.append(description);

  const submit = node("button", "Criar manutenção", "button form-wide");
  submit.type = "submit";
  const cancel = node("button", "Cancelar edição", "button button-secondary form-wide");
  cancel.type = "button";
  cancel.hidden = true;
  form.append(resourceLabel, startLabel, endLabel, descriptionLabel, submit, cancel);
  const list = node("ul");
  list.className = "item-list";
  list.setAttribute("aria-label", "Lista de manutenções");
  container.append(form, list);

  let editingId = null;
  try {
    const resources = await apiGet("/recursos");
    const placeholder = node("option", "Selecione um recurso");
    placeholder.value = "";
    placeholder.disabled = true;
    placeholder.selected = true;
    resourceSelect.append(placeholder);
    resources.forEach((resource) => {
      const option = node("option", resource.nome);
      option.value = resource.id;
      resourceSelect.append(option);
    });
    if (resources.length === 0) submit.disabled = true;
  } catch (error) {
    submit.disabled = true;
    feedback(container, `Não foi possível carregar recursos: ${error.message}`, "error");
  }

  function resetForm() {
    editingId = null;
    form.reset();
    submit.textContent = "Criar manutenção";
    cancel.hidden = true;
  }

  cancel.addEventListener("click", resetForm);
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (end.value <= start.value) {
      feedback(container, "O fim deve ser posterior ao início.", "error");
      return;
    }
    submit.disabled = true;
    const payload = {
      recursoId: resourceSelect.value,
      dataInicio: start.value,
      dataFim: end.value,
      descricao: description.value.trim(),
    };
    try {
      if (editingId) {
        await apiPut(`/manutencoes/${editingId}`, payload);
        feedback(container, "Manutenção atualizada.", "success");
      } else {
        await apiPost("/manutencoes", payload);
        feedback(container, "Manutenção criada.", "success");
      }
      resetForm();
      await loadList();
    } catch (error) {
      feedback(container, error.message, "error");
    } finally {
      submit.disabled = false;
    }
  });

  async function loadList() {
    list.replaceChildren(node("li", "Carregando manutenções…", "empty-state"));
    try {
      const maintenance = await apiGet("/manutencoes");
      list.replaceChildren();
      if (!Array.isArray(maintenance) || maintenance.length === 0) {
        list.append(node("li", "Nenhuma manutenção cadastrada.", "empty-state"));
        return;
      }
      maintenance.forEach((entry) => {
        const item = node("li");
        item.className = "item-card";
        item.append(
          node("h3", entry.nomeRecurso || "Recurso"),
          node("p", `Início: ${entry.dataInicio}`),
          node("p", `Fim: ${entry.dataFim}`),
          node("p", entry.descricao),
        );
        const edit = node("button", "Editar", "button button-secondary");
        edit.type = "button";
        edit.addEventListener("click", () => {
          editingId = entry.id;
          resourceSelect.value = entry.recursoId;
          start.value = entry.dataInicio;
          end.value = entry.dataFim;
          description.value = entry.descricao;
          submit.textContent = "Salvar alterações";
          cancel.hidden = false;
          start.focus();
        });
        const remove = node("button", "Excluir", "button button-secondary");
        remove.type = "button";
        remove.addEventListener("click", async () => {
          if (!window.confirm("Excluir esta manutenção?")) return;
          try {
            await apiDelete(`/manutencoes/${entry.id}`);
            feedback(container, "Manutenção excluída.", "success");
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

  await loadList();
}
