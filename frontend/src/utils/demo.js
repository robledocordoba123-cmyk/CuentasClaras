// Solo en la demo pública (VITE_MODO_DEMO=true): acceso de un clic a una cuenta
// con datos de ejemplo, para que quien visite pueda probar sin registrarse.
export const MODO_DEMO = import.meta.env.VITE_MODO_DEMO === "true";
export const CUENTA_DEMO = { email: "demo@cuentasclaras.co", password: "Demo2026!" };
