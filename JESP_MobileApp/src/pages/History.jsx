import { useEffect, useState } from 'react'
import {
  LineChart, Line, XAxis, YAxis, Tooltip, Legend,
  CartesianGrid, ResponsiveContainer,
} from 'recharts'
import { api } from '../api/client.js'

const RANGES = [
  { label: '6 h', hours: 6 },
  { label: '24 h', hours: 24 },
  { label: '7 días', hours: 24 * 7 },
]

export default function History() {
  const [hours, setHours] = useState(24)
  const [points, setPoints] = useState([])
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const to = new Date()
        const from = new Date(to.getTime() - hours * 3600 * 1000)
        const data = await api(
          `/api/history?limit=1000&from=${from.toISOString().slice(0, 19)}` +
          `&to=${to.toISOString().slice(0, 19)}`,
        )
        if (!cancelled) {
          setPoints(data.map((p) => ({
            time: p.timestamp.slice(5, 16).replace('T', ' '),
            temp: p.temp,
            hum: p.hum,
          })))
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
  }, [hours])

  return (
    <div>
      <h2>Historial</h2>
      <div className="row">
        {RANGES.map((r) => (
          <button
            key={r.hours}
            className={hours === r.hours ? '' : 'secondary'}
            onClick={() => setHours(r.hours)}
          >
            {r.label}
          </button>
        ))}
      </div>
      {error && <p className="error">{error}</p>}
      <div className="card chart">
        <ResponsiveContainer width="100%" height={280}>
          <LineChart data={points}>
            <CartesianGrid strokeDasharray="3 3" opacity={0.2} />
            <XAxis dataKey="time" minTickGap={40} fontSize={11} />
            <YAxis fontSize={11} />
            <Tooltip />
            <Legend />
            <Line type="monotone" dataKey="temp" name="Temp (°C)" dot={false} stroke="#f59e0b" />
            <Line type="monotone" dataKey="hum" name="Hum (%)" dot={false} stroke="#38bdf8" />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
