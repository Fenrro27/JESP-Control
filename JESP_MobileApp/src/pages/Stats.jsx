import { useEffect, useState } from 'react'
import { api } from '../api/client.js'

function fmt(n) {
  return n == null ? '—' : Number(n).toFixed(1)
}

export default function Stats() {
  const [summary, setSummary] = useState(null)
  const [trend, setTrend] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const to = new Date()
        const from = new Date(to.getTime() - 24 * 3600 * 1000)
        const isoFrom = from.toISOString().slice(0, 19)
        const isoTo = to.toISOString().slice(0, 19)
        const [s, t] = await Promise.all([
          api(`/api/stats/summary?from=${isoFrom}&to=${isoTo}`),
          api(`/api/stats/trend?from=${isoFrom}&to=${isoTo}`),
        ])
        if (!cancelled) {
          setSummary(s)
          setTrend(t)
          setError(null)
        }
      } catch (err) {
        if (!cancelled) setError(err.message)
      }
    }
    load()
    const id = setInterval(load, 30000)
    return () => {
      cancelled = true
      clearInterval(id)
    }
  }, [])

  return (
    <div>
      <h2>Estadísticas (24 h)</h2>
      {error && <p className="error">{error}</p>}
      <div className="grid-2">
        <div className="card metric">
          <span className="muted">Temp. media</span>
          <strong>{fmt(summary?.avgTemp)} °C</strong>
        </div>
        <div className="card metric">
          <span className="muted">Temp. mín / máx</span>
          <strong>{fmt(summary?.minTemp)} / {fmt(summary?.maxTemp)} °C</strong>
        </div>
        <div className="card metric">
          <span className="muted">Humedad media</span>
          <strong>{fmt(summary?.avgHum)} %</strong>
        </div>
        <div className="card metric">
          <span className="muted">Registros</span>
          <strong>{summary?.records ?? '—'}</strong>
        </div>
        <div className="card metric">
          <span className="muted">Tendencia temperatura</span>
          <strong>{trend?.direction ?? '—'}</strong>
          <span className="muted small">
            {trend ? `${Number(trend.delta).toFixed(2)} °C en 24 h` : ''}
          </span>
        </div>
      </div>
    </div>
  )
}
