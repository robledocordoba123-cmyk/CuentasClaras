import { useState } from "react";
import { Link } from "react-router-dom";
import { AlertTriangle, ArrowDownRight, ArrowUpRight, PiggyBank, Scale, Wallet } from "lucide-react";
import { Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { useDatos } from "../utils/useDatos";
import { formatearPesos, mesActual, nombreMes } from "../utils/formato";
import { Aviso, Cargando, EncabezadoPagina, EstadoVacio, Tarjeta } from "../components/ui";
import SelectorMes from "../components/SelectorMes";

// Formato corto para los ejes de las gráficas: $1,2 M / $350 mil.
const pesosCortos = (valor) =>
  valor >= 1_000_000 ? `$${(valor / 1_000_000).toLocaleString("es-CO", { maximumFractionDigits: 1 })} M` : `$${Math.round(valor / 1000)} mil`;

function Indicador({ titulo, valor, icono: Icono, tono }) {
  const tonos = {
    verde: "bg-emerald-50 text-emerald-600",
    rojo: "bg-red-50 text-red-600",
    azul: "bg-sky-50 text-sky-600",
    gris: "bg-slate-100 text-slate-600",
  };
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className={`mb-3 inline-flex rounded-lg p-2 ${tonos[tono]}`}>
        <Icono size={18} aria-hidden="true" />
      </div>
      <p className="text-sm text-slate-500">{titulo}</p>
      <p className="mt-0.5 text-xl font-bold text-slate-900">{formatearPesos(valor)}</p>
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
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            <Indicador titulo="Saldo en tus cuentas" valor={saldoTotal} icono={Wallet} tono="azul" />
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
                      : `llevas el ${a.porcentajeUsado}% del presupuesto; te quedan ${formatearPesos(a.disponible)}.`}
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
                        <Pie data={resumen.gastosPorCategoria} dataKey="total" nameKey="categoria.nombre" innerRadius="55%" outerRadius="90%" paddingAngle={2}>
                          {resumen.gastosPorCategoria.map((g) => (
                            <Cell key={g.categoria.id} fill={g.categoria.color} />
                          ))}
                        </Pie>
                        <Tooltip formatter={(valor) => formatearPesos(valor)} />
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
                        <span className="text-right font-medium text-slate-900">
                          {formatearPesos(g.total)} <span className="text-xs font-normal text-slate-500">{g.porcentaje}%</span>
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
                      <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                      <XAxis dataKey="nombre" tick={{ fontSize: 12 }} />
                      <YAxis tickFormatter={pesosCortos} tick={{ fontSize: 11 }} width={70} />
                      <Tooltip formatter={(valor) => formatearPesos(valor)} />
                      <Legend />
                      <Bar dataKey="ingresos" name="Ingresos" fill="#10b981" radius={[4, 4, 0, 0]} />
                      <Bar dataKey="gastos" name="Gastos" fill="#f87171" radius={[4, 4, 0, 0]} />
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
