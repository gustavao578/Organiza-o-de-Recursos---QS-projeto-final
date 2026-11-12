import { apiGet, apiPost } from "./api.js";
import { getSession } from "./auth.js";

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

function feedback(container, message, kind = "info") {
  const element = node(
    "p",
    message,
    `feedback${kind === "error" ? " feedback-error" : kind === "success" ? " feedback-success" : ""}`,
  );
  element.setAttribute("role", "status");
  container.querySelector(".feedback")?.remove();
  container.prepend(element);
}

function renderReservation(reservation) {
  const item = node("li");
  item.className = "item-card";
  item.append(
    node("h3", reservation.nomeRecurso || "Recurso"),
    node("p", `Período: ${reservation.dataInicio || "—"} – ${reservation.dataFim || "—"}`),
    node("p", `Status: ${reservation.status || "não informado"}`),
  );
  return item;
}

export async function mountReservations(container, profile) {
  container.replaceChildren();
  const list = node("ul");
  list.className = "item-list";
  list.setAttribute("aria-label", "Lista de reservas");
  let resourceOptions = [];

  if (profile === "SOLICITANTE") {
    const form = node("form");
    form.className = "form-grid";
    form.setAttribute("aria-label", "Criar reserva");

    const resourceLabel = node("label", "Recurso");
    const resourceSelect = node("select");
    resourceSelect.name = "recursoId";
    resourceSelect.required = true;
    resourceLabel.append(resourceSelect);

    const startLabel = node("label", "Início");
    const start = node("input");
    start.type = "datetime-local";
    start.name = "dataInicio";
    start.required = true;
    startLabel.append(start);

    const endLabel = node("label", "Fim");
    const end = node("input");
    end.type = "datetime-local";
    end.name = "dataFim";
    end.required = true;
    endLabel.append(end);

    const submit = node("button", "Solicitar reserva", "button form-wide");
    submit.type = "submit";
    form.append(resourceLabel, startLabel, endLabel, submit);
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const session = getSession();
      if (!session || session.profile !== "SOLICITANTE") {
        feedback(container, "Entre com um perfil SOLICITANTE para criar reservas.", "error");
        return;
      }
      if (end.value <= start.value) {
        feedback(container, "O fim deve ser posterior ao início.", "error");
        return;
      }

      submit.disabled = true;
      try {
        await apiPost("/reservas", {
          recursoId: resourceSelect.value,
          usuarioId: session.subject,
          dataInicio: start.value,
          dataFim: end.value,
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
    try {
      resourceOptions = await apiGet("/recursos");
      const placeholder = node("option", "Selecione um recurso");
      placeholder.value = "";
      placeholder.disabled = true;
      placeholder.selected = true;
      resourceSelect.append(placeholder);
      resourceOptions.forEach((resource) => {
        const option = node("option", resource.nome || "Recurso");
        option.value = resource.id;
        resourceSelect.append(option);
      });
      if (resourceOptions.length === 0) {
        feedback(container, "Não há recursos disponíveis para reservar.");
        submit.disabled = true;
      }
    } catch (error) {
      feedback(container, error.message, "error");
      submit.disabled = true;
    }
  }

  const refresh = node("button", "Atualizar reservas", "button button-secondary");
  refresh.type = "button";
  refresh.addEventListener("click", () => loadList());
  container.append(refresh, list);

  async function loadList() {
    list.replaceChildren(node("li", "Carregando reservas…", "empty-state"));
    try {
      const reservations = await apiGet("/reservas");
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
