import { useEffect, useState } from 'react'
import { api } from '../api/client.js'
import RuleForm from '../components/RuleForm.jsx'

const DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN']
const DAY_LABELS = { MON: 'L', TUE: 'M', WED: 'X', THU: 'J', FRI: 'V', SAT: 'S', SUN: 'D' }

function describeDays(csv) {
  if (!csv) return 'Todos los días'
  return csv.split(',').map((d) => DAY_LABELS[d.trim()] || d).join(' ')
}

export default function Rules() {
  const [rules, setRules] = useState([])
  const [devices, setDevices] = useState([])
  const [editing, setEditing] = useState(null) // null = lista; {} = nueva; {…} = editar
  const [error, setError] = useState(null)

  async function refresh() {
    try {
      const [r, d] = await Promise.all([api('/api/rules'), api('/api/devices')])
      setRules(r)
      setDevices(d.map((x) => x.id))
      setError(null)
    } catch (err) {
      setError(err.message)
    }
  }

  useEffect(() => {
    refresh()
  }, [])

  async function remove(rule) {
    if (!confirm(`¿Eliminar la regla "${rule.name || rule.id}"?`)) return
    try {
      await api(`/api/rules/${rule.id}`, { method: 'DELETE' })
      refresh()
    } catch (err) {
      setError(err.message)
    }
  }

  async function toggleEnabled(rule) {
    try {
      await api(`/api/rules/${rule.id}`, { method: 'PUT', body: { ...rule, enabled: !rule.enabled } })
      refresh()
    } catch (err) {
      setError(err.message)
    }
  }

  if (editing !== null) {
    return (
      <RuleForm
        rule={editing.id ? editing : null}
        devices={devices}
        onCancel={() => setEditing(null)}
        onSaved={() => {
          setEditing(null)
          refresh()
        }}
      />
    )
  }

  return (
    <div>
      <div className="row space-between">
        <h2>Reglas</h2>
        <button onClick={() => setEditing({})}>+ Nueva</button>
      </div>
      {error && <p className="error">{error}</p>}
      {rules.length === 0 && <p className="muted">No hay reglas definidas.</p>}
      {rules.map((rule) => (
        <div key={rule.id} className={`card rule-row ${rule.enabled ? '' : 'disabled'}`}>
          <div className="row space-between">
            <strong>{rule.name || `Regla #${rule.id}`}</strong>
            <label className="switch-label">
              <input
                type="checkbox"
                checked={rule.enabled}
                onChange={() => toggleEnabled(rule)}
              />
              activa
            </label>
          </div>
          <p className="small">
            Relé {(rule.relayIndex ?? 0) + 1} → {rule.targetState ? 'ON' : 'OFF'}
            {' · '}
            {rule.conditionLogic}
            {' · '}
            {describeDays(rule.daysOfWeek)}
            {rule.deviceId ? ` · ${rule.deviceId}` : ''}
          </p>
          <p className="small muted">
            {rule.timeStart && rule.timeEnd ? `${rule.timeStart}–${rule.timeEnd} · ` : ''}
            {rule.tempMin != null && `temp≥${rule.tempMin} `}
            {rule.tempMax != null && `temp≤${rule.tempMax} `}
            {rule.humMin != null && `hum≥${rule.humMin}% `}
            {rule.humMax != null && `hum≤${rule.humMax}%`}
            {` · histéresis ${rule.hysteresis ?? 0}`}
            {rule.minSwitchIntervalSeconds > 0 &&
              ` · cooldown ${rule.minSwitchIntervalSeconds}s`}
          </p>
          <div className="row">
            <button className="secondary" onClick={() => setEditing(rule)}>Editar</button>
            <button className="danger" onClick={() => remove(rule)}>Eliminar</button>
          </div>
        </div>
      ))}
    </div>
  )
}
