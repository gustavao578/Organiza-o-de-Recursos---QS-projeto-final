import { apiGet } from "./api.js";

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
    `feedback${kind === "error" ? " feedback-error" : ""}`,
  );
  element.setAttribute("role", "status");
  container.prepend(element);
}

export async function mountReports(container) {
  container.replaceChildren();
  const form = node("form");
  form.className = "form-grid";
  form.setAttribute("aria-label", "Consultar relatório de utilização");

  const startLabel = node("label", "Início do período");
  const start = node("input");
  start.type = "datetime-local";
  start.step = "1";
  start.required = true;
  startLabel.append(start);

  const endLabel = node("label", "Fim do período");
  const end = node("input");
  end.type = "datetime-local";
  end.step = "1";
  end.required = true;
  endLabel.append(end);

  const submit = node("button", "Consultar relatório", "button form-wide");
  submit.type = "submit";
  form.append(startLabel, endLabel, submit);
  const result = node("div");
  container.append(form, result);

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (end.value <= start.value) {
      feedback(container, "O fim deve ser posterior ao início.", "error");
      return;
    }
    submit.disabled = true;
    try {
      const query = new URLSearchParams({ inicio: start.value, fim: end.value });
      const report = await apiGet(`/relatorios/utilizacao?${query}`);
      result.replaceChildren();
      result.append(node("h3", "Horas por recurso"));
      appendRows(result, report.porRecurso, "Não há utilização de recursos neste período.");
      result.append(node("h3", "Horas por usuário"));
      appendRows(result, report.porUsuario, "Não há utilização por usuário neste período.");
      result.append(node("p", `Conflitos evitados: ${report.conflitosEvitados}`));
    } catch (error) {
      result.replaceChildren();
      feedback(container, error.message, "error");
    } finally {
      submit.disabled = false;
    }
  });
}

function appendRows(container, rows, emptyMessage) {
  if (!Array.isArray(rows) || rows.length === 0) {
    container.append(node("p", emptyMessage, "empty-state"));
    return;
  }
  const list = node("ul");
  rows.forEach((row) => {
    list.append(node("li", `${row.nome}: ${Number(row.horas).toLocaleString("pt-BR")} h`));
  });
  container.append(list);
}
