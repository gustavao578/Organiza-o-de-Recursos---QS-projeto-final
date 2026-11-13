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

function formatDate(value) {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "short",
      timeStyle: "short",
    }).format(date);
}

function toInputDateTime(value) {
  if (!value || typeof value !== "string") return "";
  return value.length === 16 ? `${value}:00` : value.slice(0, 19);
}

function button(label, action) {
  const element = node("button", label, "button button-secondary");
  element.type = "button";
  element.addEventListener("click", action);
  return element;
}

export async function mountReservations(container, session) {
  container.replaceChildren();
  let resources = [];
  try {
    resources = await apiGet("/recursos");
  } catch (error) {
    feedback(container, `Não foi possível carregar recursos: ${error.message}`, "error");
  }

  if (session.profile === "SOLICITANTE") {
    const form = node("form");
    form.className = "form-grid";
    form.setAttribute("aria-label", "Criar reserva");
    const resourceLabel = node("label", "Recurso");
    const resourceSelect = node("select");
    resourceSelect.name = "recursoId";
    resourceSelect.required = true;
    const placeholder = node("option", "Selecione um recurso");
    placeholder.value = "";
    placeholder.disabled = true;
    placeholder.selected = true;
    resourceSelect.append(placeholder);
    resources.forEach((resource) => {
      const option = node("option", resource.nome || "Recurso");
      option.value = resource.id;
      resourceSelect.append(option);
    });
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

    const submit = node("button", "Solicitar reserva", "button form-wide");
    submit.type = "submit";
    if (resources.length === 0) submit.disabled = true;
    form.append(resourceLabel, startLabel, endLabel, submit);
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      if (end.value <= start.value) {
        feedback(container, "O fim deve ser posterior ao início.", "error");
        return;
      }
      submit.disabled = true;
      try {
        await apiPost("/reservas", {
          recursoId: resourceSelect.value,
          usuarioId: session.subject,
          dataInicio: toInputDateTime(start.value),
          dataFim: toInputDateTime(end.value),
        });
        form.reset();
        feedback(container, "Reserva criada com sucesso.", "success");
        await loadList();
      } catch (error) {
        feedback(container, error.message, "error");
      } finally {
        submit.disabled = false;
      }
    });
    container.append(form);
  }

  // Barra de filtro de reservas
  const filterForm = node("form");
  filterForm.className = "filter-bar";
  filterForm.setAttribute("aria-label", "Filtrar reservas");

  const statusFilterLabel = node("label", "Filtrar por status");
  const statusFilter = node("select");
  [
    { label: "Todos os status", value: "" },
    { label: "PENDENTE", value: "PENDENTE" },
    { label: "APROVADA", value: "APROVADA" },
    { label: "EM_USO", value: "EM_USO" },
    { label: "CONCLUÍDA", value: "CONCLUÍDA" },
    { label: "REJEITADA", value: "REJEITADA" },
    { label: "CANCELADA", value: "CANCELADA" },
    { label: "NÃO_COMPARECEU", value: "NÃO_COMPARECEU" },
  ].forEach((st) => {
    const o = node("option", st.label);
    o.value = st.value;
    statusFilter.append(o);
  });
  statusFilterLabel.append(statusFilter);

  const resourceFilterLabel = node("label", "Filtrar por recurso");
  const resourceFilter = node("select");
  const allResourcesOpt = node("option", "Todos os recursos");
  allResourcesOpt.value = "";
  resourceFilter.append(allResourcesOpt);
  resources.forEach((res) => {
    const o = node("option", res.nome || "Recurso");
    o.value = res.id;
    resourceFilter.append(o);
  });
  resourceFilterLabel.append(resourceFilter);

  const refresh = node("button", "Filtrar / Atualizar reservas", "button button-secondary form-wide");
  refresh.type = "submit";
  filterForm.append(statusFilterLabel, resourceFilterLabel, refresh);
  filterForm.addEventListener("submit", (event) => {
    event.preventDefault();
    void loadList();
  });
  container.append(filterForm);

  const list = node("ul");
  list.className = "item-list";
  list.setAttribute("aria-label", "Lista de reservas");
  container.append(list);

  async function performAction(action, successMessage) {
    try {
      await action();
      feedback(container, successMessage, "success");
      await loadList();
    } catch (error) {
      feedback(container, error.message, "error");
    }
  }

  function renderEditForm(item, reservation) {
    item.querySelector("form")?.remove();
    const form = node("form");
    form.className = "form-grid";
    form.setAttribute("aria-label", "Editar reserva");
    const resourceLabel = node("label", "Recurso");
    const resourceSelect = node("select");
    resourceSelect.required = true;
    resources.forEach((resource) => {
      const option = node("option", resource.nome || "Recurso");
      option.value = resource.id;
      option.selected = resource.id === reservation.recursoId;
      resourceSelect.append(option);
    });
    resourceLabel.append(resourceSelect);

    const startLabel = node("label", "Início");
    const start = node("input");
    start.type = "datetime-local";
    start.step = "1";
    start.required = true;
    start.value = toInputDateTime(reservation.dataInicio);
    startLabel.append(start);

    const endLabel = node("label", "Fim");
    const end = node("input");
    end.type = "datetime-local";
    end.step = "1";
    end.required = true;
    end.value = toInputDateTime(reservation.dataFim);
    endLabel.append(end);

    const save = node("button", "Salvar reserva", "button form-wide");
    save.type = "submit";
    const cancel = button("Cancelar edição", () => form.remove());
    cancel.classList.add("form-wide");
    form.append(resourceLabel, startLabel, endLabel, save, cancel);
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      if (end.value <= start.value) {
        feedback(container, "O fim deve ser posterior ao início.", "error");
        return;
      }
      save.disabled = true;
      try {
        await performAction(() => apiPut(`/reservas/${reservation.id}`, {
          recursoId: resourceSelect.value,
          usuarioId: session.subject,
          dataInicio: toInputDateTime(start.value),
          dataFim: toInputDateTime(end.value),
        }), "Reserva atualizada.");
      } finally {
        save.disabled = false;
      }
    });
    item.append(form);
  }

  async function showHistory(item, reservation) {
    item.querySelector(".reservation-history")?.remove();
    try {
      const entries = await apiGet(`/reservas/${reservation.id}/historico`);
      const history = node("div", undefined, "reservation-history");
      const title = node("h4", "Histórico de auditoria");
      history.append(title);
      if (!Array.isArray(entries) || entries.length === 0) {
        history.append(node("p", "Nenhuma alteração registrada."));
      } else {
        const historyList = node("ul");
        entries.forEach((entry) => {
          historyList.append(node(
            "li",
            `${entry.estadoAnterior} → ${entry.estadoNovo} — ${formatDate(entry.instante)}`
              + (entry.motivo ? ` — ${entry.motivo}` : ""),
          ));
        });
        history.append(historyList);
      }
      item.append(history);
    } catch (error) {
      feedback(container, error.message, "error");
    }
  }

  function renderReservation(reservation) {
    const item = node("li");
    item.className = "item-card";
    const resource = resources.find((option) => option.id === reservation.recursoId);
    item.append(
      node("h3", reservation.nomeRecurso || "Recurso"),
      node("p", `Solicitante: ${reservation.nomeUsuario || "Usuário"}`),
      node("p", `Início: ${formatDate(reservation.dataInicio)}`),
      node("p", `Fim: ${formatDate(reservation.dataFim)}`),
      node("p", `Status: ${reservation.status || "não informado"}`),
    );
    if (reservation.recursoRestrito) item.append(node("p", "Recurso restrito"));

    const isAdmin = session.profile === "ADMINISTRADOR";
    const isResponsible = session.profile === "RESPONSAVEL"
      && resource?.responsavelId === session.subject;
    const isOwner = session.profile === "SOLICITANTE"
      && reservation.usuarioId === session.subject;
    const canDecide = isAdmin || isResponsible;
    const pending = reservation.status === "PENDENTE" || reservation.status === "SOLICITADA";

    if (canDecide && pending) {
      item.append(button("Aprovar", () => performAction(
        () => apiPost(`/reservas/${reservation.id}/aprovar`, {}),
        "Reserva aprovada.",
      )));
      item.append(button("Rejeitar", () => {
        const motivo = window.prompt("Motivo da rejeição (opcional):") || "";
        return performAction(
          () => apiPost(`/reservas/${reservation.id}/rejeitar`, { motivo }),
          "Reserva rejeitada.",
        );
      }));
    }
    if (isResponsible && reservation.status === "APROVADA") {
      item.append(button("Iniciar", () => performAction(
        () => apiPost(`/reservas/${reservation.id}/iniciar`, {}),
        "Reserva iniciada.",
      )));
      item.append(button("Registrar não comparecimento", () => {
        const motivo = window.prompt("Motivo (opcional):") || "";
        return performAction(
          () => apiPost(`/reservas/${reservation.id}/nao-compareceu`, { motivo }),
          "Não comparecimento registrado.",
        );
      }));
    }
    if (isResponsible && reservation.status === "EM_USO") {
      item.append(button("Concluir", () => performAction(
        () => apiPost(`/reservas/${reservation.id}/concluir`, {}),
        "Reserva concluída.",
      )));
    }
    if (isOwner && pending) {
      item.append(button("Editar", () => renderEditForm(item, reservation)));
    }
    if (isOwner && ["PENDENTE", "SOLICITADA", "APROVADA"].includes(reservation.status)) {
      item.append(button("Cancelar reserva", () => {
        const motivo = window.prompt("Motivo do cancelamento (opcional):") || "";
        return performAction(
          () => apiPost(`/reservas/${reservation.id}/cancelar`, { motivo }),
          "Reserva cancelada.",
        );
      }));
    }
    if ((isOwner || isAdmin) && reservation.status === "PENDENTE") {
      item.append(button("Excluir reserva", () => {
        if (!window.confirm("Excluir esta reserva?")) return;
        return performAction(
          () => apiDelete(`/reservas/${reservation.id}`),
          "Reserva excluída.",
        );
      }));
    }
    if (isAdmin || isResponsible) {
      item.append(button("Ver histórico", () => showHistory(item, reservation)));
    }
    return item;
  }

  async function loadList() {
    list.replaceChildren(node("li", "Carregando reservas…", "empty-state"));
    try {
      const params = new URLSearchParams();
      if (statusFilter.value) params.set("status", statusFilter.value);
      if (resourceFilter.value) params.set("recursoId", resourceFilter.value);
      const queryString = params.toString() ? `?${params.toString()}` : "";
      const reservations = await apiGet(`/reservas${queryString}`);
      list.replaceChildren();
      if (!Array.isArray(reservations) || reservations.length === 0) {
        list.append(node("li", "Nenhuma reserva cadastrada.", "empty-state"));
        return;
      }
      reservations.forEach((reservation) => list.append(renderReservation(reservation)));
    } catch (error) {
      list.replaceChildren(node("li", error.message, "feedback feedback-error"));
    }
  }

  await loadList();
}