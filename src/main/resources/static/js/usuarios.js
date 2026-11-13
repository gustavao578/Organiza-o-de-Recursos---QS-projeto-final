import { apiDelete, apiGet, apiPost, apiPut } from "./api.js";

function node(tagName, text, className) {
  const element = document.createElement(tagName);
  if (text !== undefined) element.textContent = text;
  if (className) element.className = className;
  return element;
}

function showFeedback(container, message, kind = "info") {
  container.querySelector(".feedback")?.remove();
  const feedback = node(
    "p",
    message,
    `feedback${kind === "error" ? " feedback-error" : kind === "success" ? " feedback-success" : ""}`,
  );
  feedback.setAttribute("role", "status");
  container.prepend(feedback);
}

export async function mountUsers(container, session) {
  container.replaceChildren();
  const administrator = session.profile === "ADMINISTRADOR";
  const form = node("form");
  form.className = "form-grid";
  form.setAttribute("aria-label", administrator ? "Criar ou editar usuário" : "Editar meu cadastro");

  const nameLabel = node("label", "Nome");
  const name = node("input");
  name.required = true;
  nameLabel.append(name);

  const emailLabel = node("label", "E-mail");
  const email = node("input");
  email.type = "email";
  email.required = true;
  emailLabel.append(email);

  const registrationLabel = node("label", "Matrícula");
  const registration = node("input");
  registration.required = true;
  registrationLabel.append(registration);

  let profile = null;
  if (administrator) {
    const profileLabel = node("label", "Perfil");
    profile = node("select");
    ["SOLICITANTE", "RESPONSAVEL", "ADMINISTRADOR"].forEach((value) => {
      const option = node("option", value);
      option.value = value;
      profile.append(option);
    });
    profileLabel.append(profile);
    form.append(profileLabel);
  }

  const submit = node("button", administrator ? "Criar usuário" : "Salvar cadastro", "button form-wide");
  submit.type = "submit";
  const cancel = node("button", "Cancelar edição", "button button-secondary form-wide");
  cancel.type = "button";
  cancel.hidden = true;
  form.prepend(nameLabel, emailLabel, registrationLabel);
  form.append(submit, cancel);
  container.append(form);

  const list = node("ul");
  list.className = "item-list";
  list.setAttribute("aria-label", "Lista de usuários");
  if (administrator) container.append(list);

  let editingId = administrator ? null : session.subject;
  function setForm(user, creating = false) {
    editingId = creating ? null : user.id;
    name.value = user.nome || "";
    email.value = user.email || "";
    registration.value = user.matricula || "";
    if (profile) profile.value = user.perfil || "SOLICITANTE";
    submit.textContent = creating ? "Criar usuário" : "Salvar alterações";
    cancel.hidden = !administrator || creating;
  }

  if (!administrator) {
    try {
      setForm(await apiGet(`/usuarios/${session.subject}`));
    } catch (error) {
      showFeedback(container, error.message, "error");
      submit.disabled = true;
    }
  } else {
    setForm({}, true);
  }

  cancel.addEventListener("click", () => setForm({}, true));
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    submit.disabled = true;
    const payload = {
      nome: name.value.trim(),
      email: email.value.trim(),
      matricula: registration.value.trim(),
    };
    if (profile) payload.perfil = profile.value;
    try {
      if (editingId) {
        await apiPut(`/usuarios/${editingId}`, payload);
        showFeedback(container, "Cadastro atualizado.", "success");
      } else {
        await apiPost("/usuarios", payload);
        setForm({}, true);
        showFeedback(container, "Usuário criado.", "success");
      }
      if (administrator) await loadUsers();
    } catch (error) {
      showFeedback(container, error.message, "error");
    } finally {
      submit.disabled = false;
    }
  });

  async function loadUsers() {
    list.replaceChildren(node("li", "Carregando usuários…", "empty-state"));
    try {
      const users = await apiGet("/usuarios");
      list.replaceChildren();
      if (!Array.isArray(users) || users.length === 0) {
        list.append(node("li", "Nenhum usuário cadastrado.", "empty-state"));
        return;
      }
      users.forEach((user) => {
        const item = node("li");
        item.className = "item-card";
        item.append(
          node("h3", user.nome),
          node("p", user.email),
          node("p", `Matrícula: ${user.matricula}`),
          node("p", `Perfil: ${user.perfil}`),
        );
        const edit = node("button", "Editar", "button button-secondary");
        edit.type = "button";
        edit.addEventListener("click", () => {
          setForm(user);
          name.focus();
        });
        const remove = node("button", "Excluir", "button button-secondary");
        remove.type = "button";
        remove.addEventListener("click", async () => {
          if (!window.confirm(`Excluir o usuário "${user.nome}"?`)) return;
          try {
            await apiDelete(`/usuarios/${user.id}`);
            showFeedback(container, "Usuário excluído.", "success");
            await loadUsers();
          } catch (error) {
            showFeedback(container, error.message, "error");
          }
        });
        item.append(edit, remove);
        list.append(item);
      });
    } catch (error) {
      list.replaceChildren(node("li", error.message, "feedback feedback-error"));
    }
  }

  if (administrator) await loadUsers();
}
