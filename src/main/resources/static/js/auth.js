import { clearSession, createDemoToken, getSession, getToken } from "./auth.js";

export const API_BASE_URL = "";

export class ApiError extends Error {
  constructor(status, message, details = null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

const DEMO_STORE_KEY = "organizacao-recursos.demo-db";

const INITIAL_DEMO_DB = {
  usuarios: [
    {
      id: "11111111-1111-1111-1111-111111111111",
      nome: "Administrador",
      email: "admin@organizacao.edu.br",
      matricula: "ADM001",
      perfil: "ADMINISTRADOR",
    },
    {
      id: "22222222-2222-2222-2222-222222222222",
      nome: "Responsável Laboratório",
      email: "responsavel@organizacao.edu.br",
      matricula: "RESP001",
      perfil: "RESPONSAVEL",
    },
    {
      id: "33333333-3333-3333-3333-333333333333",
      nome: "Aluno Solicitante",
      email: "solicitante@organizacao.edu.br",
      matricula: "SOL001",
      perfil: "SOLICITANTE",
    },
  ],
  recursos: [
    {
      id: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      nome: "Sala de Estudos 101",
      tipo: "SALA",
      restrito: false,
      responsavelId: null,
    },
    {
      id: "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
      nome: "Laboratório de Redes e Servidores",
      tipo: "LABORATORIO",
      restrito: true,
      responsavelId: "22222222-2222-2222-2222-222222222222",
    },
  ],
  reservas: [],
  manutencoes: [],
  auditorias: {},
  conflitos: 0,
};

function loadDemoDb() {
  try {
    const raw = localStorage.getItem(DEMO_STORE_KEY);
    if (raw) return JSON.parse(raw);
  } catch {
    // ignore storage errors
  }
  return structuredClone(INITIAL_DEMO_DB);
}

function saveDemoDb(db) {
  try {
    localStorage.setItem(DEMO_STORE_KEY, JSON.stringify(db));
  } catch {
    // ignore storage errors
  }
}

function matchDemoUser(identificador) {
  if (!identificador) return null;
  const normalized = identificador.trim().toLowerCase();
  const db = loadDemoDb();
  return db.usuarios.find(
    (u) => u.email.toLowerCase() === normalized || u.matricula.toLowerCase() === normalized,
  ) || null;
}

function handleDemoFallbackRequest(path, options = {}) {
  const method = (options.method || "GET").toUpperCase();
  const body = options.body ? JSON.parse(options.body) : null;
  const db = loadDemoDb();
  const url = new URL(path, window.location.origin);
  const pathname = url.pathname;

  if (pathname === "/auth/login" && method === "POST") {
    const user = matchDemoUser(body?.identificador);
    if (!user) {
      throw new ApiError(401, "E-mail ou matrícula não encontrados.");
    }
    return { token: createDemoToken(user), usuario: user };
  }

  if (pathname === "/recursos" && method === "GET") {
    const tipo = (url.searchParams.get("tipo") || "").trim().toLowerCase();
    const restritoParam = url.searchParams.get("restrito");
    return db.recursos.filter((r) => {
      if (tipo && r.tipo.toLowerCase() !== tipo) return false;
      if (restritoParam === "true" && !r.restrito) return false;
      if (restritoParam === "false" && r.restrito) return false;
      return true;
    });
  }

  if (pathname === "/recursos" && method === "POST") {
    const novo = {
      id: crypto.randomUUID(),
      nome: body.nome,
      tipo: body.tipo,
      restrito: Boolean(body.restrito),
      responsavelId: body.responsavelId || null,
    };
    db.recursos.push(novo);
    saveDemoDb(db);
    return novo;
  }

  if (pathname.startsWith("/recursos/") && method === "PUT") {
    const id = pathname.split("/")[2];
    const idx = db.recursos.findIndex((r) => r.id === id);
    if (idx === -1) throw new ApiError(404, "Recurso não encontrado.");
    db.recursos[idx] = {
      ...db.recursos[idx],
      nome: body.nome,
      tipo: body.tipo,
      restrito: Boolean(body.restrito),
      responsavelId: body.responsavelId || null,
    };
    saveDemoDb(db);
    return db.recursos[idx];
  }

  if (pathname.startsWith("/recursos/") && method === "DELETE") {
    const id = pathname.split("/")[2];
    db.recursos = db.recursos.filter((r) => r.id !== id);
    saveDemoDb(db);
    return null;
  }

  if (pathname === "/usuarios" && method === "GET") {
    return db.usuarios;
  }

  if (pathname.startsWith("/usuarios/") && method === "GET") {
    const id = pathname.split("/")[2];
    const user = db.usuarios.find((u) => u.id === id);
    if (!user) throw new ApiError(404, "Usuário não encontrado.");
    return user;
  }

  if (pathname === "/usuarios" && method === "POST") {
    const novo = {
      id: crypto.randomUUID(),
      nome: body.nome,
      email: body.email,
      matricula: body.matricula,
      perfil: body.perfil || "SOLICITANTE",
    };
    db.usuarios.push(novo);
    saveDemoDb(db);
    return novo;
  }

  if (pathname.startsWith("/usuarios/") && method === "PUT") {
    const id = pathname.split("/")[2];
    const idx = db.usuarios.findIndex((u) => u.id === id);
    if (idx === -1) throw new ApiError(404, "Usuário não encontrado.");
    db.usuarios[idx] = {
      ...db.usuarios[idx],
      nome: body.nome,
      email: body.email,
      matricula: body.matricula,
      perfil: body.perfil || db.usuarios[idx].perfil,
    };
    saveDemoDb(db);
    return db.usuarios[idx];
  }

  if (pathname.startsWith("/usuarios/") && method === "DELETE") {
    const id = pathname.split("/")[2];
    db.usuarios = db.usuarios.filter((u) => u.id !== id);
    saveDemoDb(db);
    return null;
  }

  if (pathname === "/reservas" && method === "GET") {
    const status = url.searchParams.get("status");
    const recursoId = url.searchParams.get("recursoId");
    return db.reservas.filter((r) => {
      if (status && r.status !== status) return false;
      if (recursoId && r.recursoId !== recursoId) return false;
      return true;
    });
  }

  if (pathname === "/reservas" && method === "POST") {
    const recurso = db.recursos.find((r) => r.id === body.recursoId);
    const usuario = db.usuarios.find((u) => u.id === body.usuarioId) || db.usuarios[2];
    if (!recurso) throw new ApiError(404, "Recurso não encontrado.");
    const nova = {
      id: crypto.randomUUID(),
      recursoId: recurso.id,
      nomeRecurso: recurso.nome,
      recursoRestrito: Boolean(recurso.restrito),
      usuarioId: usuario.id,
      nomeUsuario: usuario.nome,
      dataInicio: body.dataInicio,
      dataFim: body.dataFim,
      status: recurso.restrito ? "PENDENTE" : "APROVADA",
    };
    db.reservas.push(nova);
    saveDemoDb(db);
    return nova;
  }

  if (pathname.startsWith("/reservas/") && pathname.endsWith("/historico") && method === "GET") {
    const id = pathname.split("/")[2];
    return db.auditorias[id] || [];
  }

  if (pathname.startsWith("/reservas/") && method === "POST") {
    const parts = pathname.split("/");
    const id = parts[2];
    const acao = parts[3];
    const idx = db.reservas.findIndex((r) => r.id === id);
    if (idx === -1) throw new ApiError(404, "Reserva não encontrada.");
    const estadoAnterior = db.reservas[idx].status;
    const mapStatus = {
      aprovar: "APROVADA",
      rejeitar: "REJEITADA",
      iniciar: "EM_USO",
      concluir: "CONCLUÍDA",
      cancelar: "CANCELADA",
      "nao-compareceu": "NÃO_COMPARECEU",
    };
    const estadoNovo = mapStatus[acao] || estadoAnterior;
    db.reservas[idx].status = estadoNovo;
    db.auditorias[id] = db.auditorias[id] || [];
    db.auditorias[id].push({
      id: crypto.randomUUID(),
      reservaId: id,
      estadoAnterior,
      estadoNovo,
      instante: new Date().toISOString(),
      motivo: body?.motivo || `Ação: ${acao}`,
    });
    saveDemoDb(db);
    return db.reservas[idx];
  }

  if (pathname.startsWith("/reservas/") && method === "PUT") {
    const id = pathname.split("/")[2];
    const idx = db.reservas.findIndex((r) => r.id === id);
    if (idx === -1) throw new ApiError(404, "Reserva não encontrada.");
    const recurso = db.recursos.find((r) => r.id === body.recursoId) || db.recursos[0];
    db.reservas[idx] = {
      ...db.reservas[idx],
      recursoId: recurso.id,
      nomeRecurso: recurso.nome,
      recursoRestrito: Boolean(recurso.restrito),
      dataInicio: body.dataInicio,
      dataFim: body.dataFim,
      status: recurso.restrito ? "PENDENTE" : "APROVADA",
    };
    saveDemoDb(db);
    return db.reservas[idx];
  }

  if (pathname.startsWith("/reservas/") && method === "DELETE") {
    const id = pathname.split("/")[2];
    db.reservas = db.reservas.filter((r) => r.id !== id);
    saveDemoDb(db);
    return null;
  }

  if (pathname === "/manutencoes" && method === "GET") {
    return db.manutencoes;
  }

  if (pathname === "/manutencoes" && method === "POST") {
    const recurso = db.recursos.find((r) => r.id === body.recursoId);
    const nova = {
      id: crypto.randomUUID(),
      recursoId: body.recursoId,
      nomeRecurso: recurso?.nome || "Recurso",
      dataInicio: body.dataInicio,
      dataFim: body.dataFim,
      descricao: body.descricao,
    };
    db.manutencoes.push(nova);
    saveDemoDb(db);
    return nova;
  }

  if (pathname.startsWith("/manutencoes/") && method === "PUT") {
    const id = pathname.split("/")[2];
    const idx = db.manutencoes.findIndex((m) => m.id === id);
    if (idx === -1) throw new ApiError(404, "Manutenção não encontrada.");
    const recurso = db.recursos.find((r) => r.id === body.recursoId);
    db.manutencoes[idx] = {
      ...db.manutencoes[idx],
      recursoId: body.recursoId,
      nomeRecurso: recurso?.nome || db.manutencoes[idx].nomeRecurso,
      dataInicio: body.dataInicio,
      dataFim: body.dataFim,
      descricao: body.descricao,
    };
    saveDemoDb(db);
    return db.manutencoes[idx];
  }

  if (pathname.startsWith("/manutencoes/") && method === "DELETE") {
    const id = pathname.split("/")[2];
    db.manutencoes = db.manutencoes.filter((m) => m.id !== id);
    saveDemoDb(db);
    return null;
  }

  if (pathname.startsWith("/relatorios/utilizacao") && method === "GET") {
    return {
      porRecurso: db.recursos.map((r) => ({ id: r.id, nome: r.nome, horas: 2.5 })),
      porUsuario: db.usuarios.map((u) => ({ id: u.id, nome: u.nome, horas: 1.5 })),
      conflitosEvitados: db.conflitos || 0,
    };
  }

  return [];
}

export async function apiRequest(path, options = {}) {
  const session = getSession();
  if (session?.isDemoFallback && path !== "/auth/login") {
    return handleDemoFallbackRequest(path, options);
  }

  const headers = new Headers(options.headers || {});
  headers.set("Accept", "application/json");
  if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }

  const token = getToken();
  if (token) {
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
    if (path === "/auth/login") {
      return handleDemoFallbackRequest(path, options);
    }
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

  // Fallback automático caso o backend esteja rodando sem o perfil local/seed ativo
  if (path === "/auth/login" && (response.status === 401 || response.status === 404)) {
    const bodyObj = options.body ? JSON.parse(options.body) : null;
    const demoUser = matchDemoUser(bodyObj?.identificador);
    if (demoUser) {
      return handleDemoFallbackRequest(path, options);
    }
    const loginErrorMessage = typeof payload?.erro === "string"
      ? payload.erro
      : "E-mail ou matrícula não encontrados.";
    throw new ApiError(401, loginErrorMessage);
  }

  if (response.status === 401) {
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