import { clearSession, getToken } from "./auth.js";

export const API_BASE_URL = "";

export class ApiError extends Error {
  constructor(status, message, details = null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

export async function apiRequest(path, options = {}) {
  const headers = new Headers(options.headers || {});
  headers.set("Accept", "application/json");
  if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }

  const token = getToken();
  if (token && path !== "/auth/login") {
    headers.set("Authorization", `Bearer ${token}`);
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers,
      credentials: "same-origin",
    });
  } catch {
    throw new ApiError(0, "Não foi possível conectar à aplicação. Tente novamente.");
  }

  const contentType = response.headers.get("content-type") || "";
  let payload = null;
  if (contentType.includes("application/json")) {
    try {
      payload = await response.json();
    } catch {
      payload = null;
    }
  } else if (response.status !== 204) {
    payload = await response.text().catch(() => null);
  }

  if (response.status === 401) {
    if (path === "/auth/login") {
      const message = typeof payload?.erro === "string" && payload.erro
        ? payload.erro
        : "E-mail ou matrícula não encontrados.";
      throw new ApiError(401, message);
    }
    clearSession();
    window.dispatchEvent(new Event("session-expired"));
    throw new ApiError(401, "Sessão inválida ou expirada. Entre novamente.");
  }
  if (response.status === 403) {
    throw new ApiError(403, "Você não tem permissão para realizar esta ação.");
  }
  if (response.status === 409) {
    const message = typeof payload?.erro === "string"
      ? payload.erro
      : "Há um conflito com um registro existente.";
    const details = typeof payload?.detalhes === "string" ? payload.detalhes : null;
    throw new ApiError(409, details ? `${message} ${details}` : message, details);
  }
  if (!response.ok) {
    const fieldErrors = response.status === 400 && payload && typeof payload === "object"
      ? Object.entries(payload)
        .filter(([, value]) => typeof value === "string")
        .map(([field, value]) => `${field}: ${value}`)
      : [];
    const message = fieldErrors.length > 0
      ? fieldErrors.join("; ")
      : typeof payload?.erro === "string"
        ? payload.erro
        : `A solicitação falhou (HTTP ${response.status}).`;
    throw new ApiError(response.status, message);
  }

  return payload;
}

export function apiGet(path) {
  return apiRequest(path);
}

export function apiPost(path, body) {
  return apiRequest(path, { method: "POST", body: JSON.stringify(body) });
}

export function apiPut(path, body) {
  return apiRequest(path, { method: "PUT", body: JSON.stringify(body) });
}

export function apiPatch(path, body) {
  return apiRequest(path, { method: "PATCH", body: JSON.stringify(body) });
}

export function apiDelete(path) {
  return apiRequest(path, { method: "DELETE" });
}
