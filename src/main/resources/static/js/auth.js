const SESSION_TOKEN_KEY = "organizacao-recursos.access-token";
const ALLOWED_PROFILES = new Set(["SOLICITANTE", "RESPONSAVEL", "ADMINISTRADOR"]);

function decodePayload(token) {
  const parts = token.split(".");
  if (parts.length !== 3 || !parts[1]) {
    return null;
  }

  try {
    const encoded = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const padded = encoded.padEnd(Math.ceil(encoded.length / 4) * 4, "=");
    return JSON.parse(atob(padded));
  } catch {
    return null;
  }
}

function sessionFromToken(token) {
  const claims = decodePayload(token);
  if (typeof claims?.sub !== "string"
      || claims.sub.length === 0
      || !ALLOWED_PROFILES.has(claims.profile)) {
    return null;
  }
  if (claims.exp !== undefined
      && (typeof claims.exp !== "number" || claims.exp <= Date.now() / 1000)) {
    return null;
  }
  return { token, subject: claims.sub, profile: claims.profile };
}

export function getToken() {
  try {
    return sessionStorage.getItem(SESSION_TOKEN_KEY);
  } catch {
    return null;
  }
}

export function getSession() {
  const token = getToken();
  if (!token) {
    return null;
  }
  const session = sessionFromToken(token);
  if (!session) {
    clearSession();
  }
  return session;
}

export function startSession(token) {
  const normalizedToken = token.trim();
  const session = sessionFromToken(normalizedToken);
  if (!session) {
    return false;
  }
  try {
    sessionStorage.setItem(SESSION_TOKEN_KEY, normalizedToken);
    return true;
  } catch {
    return false;
  }
}

export function clearSession() {
  try {
    sessionStorage.removeItem(SESSION_TOKEN_KEY);
  } catch {
    // Continue to the authentication view even if browser storage is unavailable.
  }
}
