import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/client.js'
import RelayGrid from '../components/RelayGrid.jsx'

export default function Dashboard() {
  const [devices, setDevices] = useState([])
  const [deviceId, setDeviceId] = useState('esp32-default')
  const [state, setState] = useState(null)
  const [error, setError] = useState(null)

  const refresh = useCallback(async () => {
    try {
      const list = await api('/api/devices')
      setDevices(list)
      if (!list.some((d) => d.id === deviceId) && list.length > 0) {
        setDeviceId(list[0].id)
      }
      const current = await api(`/api/devices/${encodeURIComponent(deviceId)}/state`)
      setState(current)
      setError(null)
    } catch (err) {
      setError(err.message)
    }
  }, [deviceId])

  useEffect(() => {
    refresh()
    const id = setInterval(refresh, 3000)
    return () => clearInterval(id)
  }, [refresh])

  async function toggleRelay(index, newState) {
    try {
      await api(
        `/api/devices/${encodeURIComponent(deviceId)}/relay?relay=${index}&state=${newState}`,
        { method: 'POST' },
      )
      setState((prev) => ({
        ...prev,
        relays: prev.relays.map((r, i) => (i === index ? newState : r)),
        overrides: prev.overrides.map((o, i) => (i === index ? true : o)),
      }))
    } catch (err) {
      setError(err.message)
    }
  }

  async function resetOverrides() {
    try {
      await api(`/api/devices/${encodeURIComponent(deviceId)}/reset_override`, { method: 'POST' })
      refresh()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div>
      <div className="row">
        <select value={deviceId} onChange={(e) => setDeviceId(e.target.value)}>
          {(devices.length ? devices.map((d) => d.id) : ['esp32-default']).map((id) => (
            <option key={id} value={id}>
              {id}
            </option>
          ))}
        </select>
        {state && (
          <span className={state.connected ? 'badge ok' : 'badge warn'}>
            {state.connected ? 'Dispositivo conectado' : 'Sin conexión'}
          </span>
        )}
      </div>

      {error && <p className="error">{error}</p>}

      {state && (
        <>
          <div className="grid-2">
            <div className="card metric">
              <span className="muted">Temperatura</span>
              <strong>{state.temp.toFixed(1)} °C</strong>
            </div>
            <div className="card metric">
              <span className="muted">Humedad</span>
              <strong>{state.hum.toFixed(1)} %</strong>
            </div>
          </div>
          <RelayGrid
            relays={state.relays}
            overrides={state.overrides}
            onToggle={toggleRelay}
          />
          <button className="secondary" onClick={resetOverrides}>
            Devolver control a las reglas
          </button>
        </>
      )}
    </div>
  )
}
