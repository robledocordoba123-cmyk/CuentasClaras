import { useState } from "react";
import { Link } from "react-router-dom";
import { AlertTriangle, ArrowDownRight, ArrowUpRight, PiggyBank, Scale, Wallet } from "lucide-react";
import { Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { useDatos } from "../utils/useDatos";
import { formatearPesos, formatearPorcentaje, mesActual, nombreMes } from "../utils/formato";
import { Aviso, Cargando, EncabezadoPagina, EstadoVacio, Tarjeta } from "../components/ui";
import SelectorMes from "../components/SelectorMes";
import Guilloche from "../components/Guilloche";

// Ingresos en verde (billete de 100 mil) y gastos en carmín (billete de 10 mil).
const COLOR_INGRESOS = "#2d7d55";
const COLOR_GASTOS = "#c23b5a";

const ESTILO_TOOLTIP = {
  contentStyle: { background: "#ffffff", border: "1px solid #e2e5de", borderRadius: 12, color: "#16181d", boxShadow: "0 8px 24px rgb(22 24 29 / 0.08)" },
  itemStyle: { color: "#24272a" },
  labelStyle: { color: "#5a6058" },
  cursor: { fill: "rgb(22 24 29 / 0.04)" },
};

// Formato corto para los ejes de las gráficas: $1,2 M / $350 mil.
const pesosCortos = (valor) =>
  valor >= 1_000_000 ? `$${(valor / 1_000_000).toLocaleString("es-CO", { maximumFractionDigits: 1 })} M` : `$${Math.round(valor / 1000)} mil`;

// El saldo total es la cifra principal, presentado como un billete: fondo grafito,
// guilloche turquesa y la cifra en la serif de grabado.
function SaldoPrincipal({ valor, cuentas }) {
  return (
    <div className="relative col-span-2 overflow-hidden rounded-2xl bg-tinta p-6 text-white">
      <Guilloche color="#3ec3d4" className="pointer-events-none absolute -right-24 -top-32 h-80 w-80" opacidad={0.32} />
      <div className="pointer-events-none absolute inset-2 rounded-xl border border-white/10" aria-hidden="true" />
      <p className="relative flex items-center gap-2 text-sm font-medium text-[#9fdde6]">
        <Wallet size={16} aria-hidden="true" /> Saldo en tus cuentas
      </p>
      <p className="cifra relative mt-2 text-4xl font-semibold sm:text-5xl">{formatearPesos(valor)}</p>
      <p className="relative mt-2 text-xs text-slate-300">Suma de {cuentas} cuentas: efectivo, billeteras y banco</p>
    </div>
  );
}

function Indicador({ titulo, valor, icono: Icono, tono }) {
  const tonos = {
    verde: "bg-emerald-50 text-emerald-700",
    rojo: "bg-red-50 text-red-700",
    azul: "bg-sky-50 text-sky-800",
    gris: "bg-slate-100 text-slate-600",
  };
  return (
    <div className="rounded-2xl border border-slate-200 bg-panel p-4">
      <div className={`mb-3 inline-flex rounded-lg p-2 ${tonos[tono]}`}>
        <Icono size={18} aria-hidden="true" />
      </div>
      <p className="text-sm text-slate-500">{titulo}</p>
      <p className="cifra mt-0.5 text-2xl font-semibold text-slate-900">{formatearPesos(valor)}</p>
    </div>
  );
}

export default function ResumenPage() {
  const [mes, setMes] = useState(mesActual());
  const { datos: resumen, error } = useDatos(`/resumen?mes=${mes}`);
  const { datos: tendencia } = useDatos("/resumen/tendencia?meses=6");
  const { datos: cuentas } = useDatos("/cuentas");

  const saldoTotal = cuentas?.reduce((suma, c) => suma + Number(c.saldoActual), 0) ?? 0;

  return (
    <div>
      <EncabezadoPagina titulo="Resumen" subtitulo="Cómo va tu plata este mes." accion={<SelectorMes mes={mes} alCambiar={setMes} />} />
      {error && <Aviso>{error}</Aviso>}

      {!resumen ? (
        <Cargando />
      ) : (
        <div className="space-y-6">
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-5">
            <SaldoPrincipal valor={saldoTotal} cuentas={cuentas?.length ?? 0} />
            <Indicador titulo="Ingresos del mes" valor={resumen.totalIngresos} icono={ArrowUpRight} tono="verde" />
            <Indicador titulo="Gastos del mes" valor={resumen.totalGastos} icono={ArrowDownRight} tono="rojo" />
            <Indicador titulo="Balance del mes" valor={resumen.balance} icono={Scale} tono={resumen.balance >= 0 ? "verde" : "rojo"} />
          </div>

          {resumen.alertas.length > 0 && (
            <div className="space-y-2">
              {resumen.alertas.map((a) => (
                <Link
                  key={a.id}
                  to="/presupuestos"
                  className={`flex items-center gap-3 rounded-xl border px-4 py-3 text-sm ${
                    a.estado === "EXCEDIDO" ? "border-red-200 bg-red-50 text-red-800" : "border-amber-200 bg-amber-50 text-amber-800"
                  }`}
                >
                  <AlertTriangle size={18} className="shrink-0" aria-hidden="true" />
                  <span>
                    <strong>{a.categoria.nombre}:</strong>{" "}
                    {a.estado === "EXCEDIDO"
                      ? `te pasaste del presupuesto por ${formatearPesos(-a.disponible)}.`
                      : `llevas el ${formatearPorcentaje(a.porcentajeUsado)} del presupuesto; te quedan ${formatearPesos(a.disponible)}.`}
                  </span>
                </Link>
              ))}
            </div>
          )}

          <div className="grid gap-6 lg:grid-cols-2">
            <Tarjeta titulo={`¿En qué se fue la plata en ${nombreMes(mes, false)}?`}>
              {resumen.gastosPorCategoria.length === 0 ? (
                <EstadoVacio icono={PiggyBank} titulo="Sin gastos este mes" descripcion="Cuando registres gastos, aquí verás en qué se van." />
              ) : (
                <div className="grid items-center gap-4 sm:grid-cols-2">
                  <div className="h-56">
                    <ResponsiveContainer>
                      <PieChart>
                        <Pie stroke="#ffffff" strokeWidth={2} data={resumen.gastosPorCategoria} dataKey="total" nameKey="categoria.nombre" innerRadius="55%" outerRadius="90%" paddingAngle={2}>
                          {resumen.gastosPorCategoria.map((g) => (
                            <Cell key={g.categoria.id} fill={g.categoria.color} />
                          ))}
                        </Pie>
                        <Tooltip formatter={(valor) => formatearPesos(valor)} {...ESTILO_TOOLTIP} />
                      </PieChart>
                    </ResponsiveContainer>
                  </div>
                  <ul className="space-y-2 text-sm">
                    {resumen.gastosPorCategoria.map((g) => (
                      <li key={g.categoria.id} className="flex items-center justify-between gap-2">
                        <span className="flex items-center gap-2">
                          <span className="h-3 w-3 rounded-full" style={{ backgroundColor: g.categoria.color }} />
                          {g.categoria.nombre}
                        </span>
                        <span className="whitespace-nowrap text-right font-medium text-slate-900">
                          {formatearPesos(g.total)} <span className="text-xs font-normal text-slate-500">{formatearPorcentaje(g.porcentaje)}</span>
                        </span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </Tarjeta>

            <Tarjeta titulo="Últimos 6 meses">
              <div className="h-64">
                {tendencia && (
                  <ResponsiveContainer>
                    <BarChart data={tendencia.map((t) => ({ ...t, nombre: nombreMes(t.mes, false) }))}>
                      <CartesianGrid vertical={false} stroke="#e2e5de" />
                      <XAxis dataKey="nombre" tick={{ fontSize: 12, fill: "#5a6058" }} axisLine={{ stroke: "#c9cec5" }} tickLine={false} />
                      <YAxis tickFormatter={pesosCortos} tick={{ fontSize: 11, fill: "#5a6058" }} axisLine={false} tickLine={false} width={70} />
                      <Tooltip formatter={(valor) => formatearPesos(valor)} {...ESTILO_TOOLTIP} />
                      <Legend wrapperStyle={{ fontSize: 13 }} />
                      <Bar dataKey="ingresos" name="Ingresos" fill={COLOR_INGRESOS} radius={[4, 4, 0, 0]} maxBarSize={28} />
                      <Bar dataKey="gastos" name="Gastos" fill={COLOR_GASTOS} radius={[4, 4, 0, 0]} maxBarSize={28} />
                    </BarChart>
                  </ResponsiveContainer>
                )}
              </div>
            </Tarjeta>
          </div>
        </div>
      )}
    </div>
  );
}
