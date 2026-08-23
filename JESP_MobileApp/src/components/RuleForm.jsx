import { useState } from 'react'
import { api } from '../api/client.js'

const DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN']

export default function RuleForm({ rule, devices, onCancel, onSaved }) {
  const [form, setForm] = useState({
    name: rule?.name || '',
    deviceId: rule?.deviceId || '',
    relayIndex: rule?.relayIndex ?? 0,
    targetState: rule?.targetState ?? true,
    enabled: rule?.enabled ?? true,
    priority: rule?.priority ?? 100,
    timeStart: rule?.timeStart || '',
    timeEnd: rule?.timeEnd || '',
    tempMin: rule?.tempMin ?? '',
    tempMax: rule?.tempMax ?? '',
    humMin: rule?.humMin ?? '',
    humMax: rule?.humMax ?? '',
    conditionLogic: rule?.conditionLogic || 'AND',
    daysOfWeek: rule?.daysOfWeek ? rule.daysOfWeek.split(',') : [],
    hysteresis: rule?.hysteresis ?? 0.5,
    minSwitchIntervalSeconds: rule?.minSwitchIntervalSeconds ?? 0,
  })
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)

  function set(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  function toggleDay(day) {
    setForm((prev) => ({
      ...prev,
      daysOfWeek: prev.daysOfWeek.includes(day)
        ? prev.daysOfWeek.filter((d) => d !== day)
        : [...prev.daysOfWeek, day],
    }))
  }

  function numOrNull(value) {
    return value === '' || value == null ? null : Number(value)
  }

  async function submit(e) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const body = {
        ...form,
        relayIndex: Number(form.relayIndex),
        priority: Number(form.priority),
        hysteresis: numOrNull(form.hysteresis),
        minSwitchIntervalSeconds: Number(form.minSwitchIntervalSeconds),
        tempMin: numOrNull(form.tempMin),
        tempMax: numOrNull(form.tempMax),
        humMin: numOrNull(form.humMin),
        humMax: numOrNull(form.humMax),
        timeStart: form.timeStart || null,
        timeEnd: form.timeEnd || null,
        deviceId: form.deviceId || null,
        daysOfWeek: form.daysOfWeek.length ? form.daysOfWeek.join(',') : null,
      }
      if (rule?.id) {
        await api(`/api/rules/${rule.id}`, { method: 'PUT', body })
      } else {
        await api('/api/rules', { method: 'POST', body })
      }
      onSaved()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div>
      <h2>{rule?.id ? 'Editar regla' : 'Nueva regla'}</h2>
      <form className="card" onSubmit={submit}>
        <label>
          Nombre
          <input value={form.name} onChange={(e) => set('name', e.target.value)} placeholder="Ej. Ventilación nocturna" />
        </label>

        <div className="grid-2">
          <label>
            Dispositivo
            <select value={form.deviceId} onChange={(e) => set('deviceId', e.target.value)}>
              <option value="">(por defecto)</option>
              {devices.map((d) => (
                <option key={d} value={d}>{d}</option>
              ))}
            </select>
          </label>
          <label>
            Relé (1-6)
            <input
              type="number" min="0" max="5"
              value={form.relayIndex}
              onChange={(e) => set('relayIndex', e.target.value)}
              required
            />
          </label>
        </div>

        <div className="grid-2">
          <label>
            Acción
            <select value={form.targetState} onChange={(e) => set('targetState', e.target.value === 'true')}>
              <option value="true">Encender (ON)</option>
              <option value="false">Apagar (OFF)</option>
            </select>
          </label>
          <label>
            Lógica entre condiciones
            <select value={form.conditionLogic} onChange={(e) => set('conditionLogic', e.target.value)}>
              <option value="AND">Y (todas)</option>
              <option value="OR">O (cualquiera)</option>
            </select>
          </label>
        </div>

        <fieldset>
          <legend>Horario</legend>
          <div className="grid-2">
            <label>
              Desde
              <input type="time" value={form.timeStart} onChange={(e) => set('timeStart', e.target.value)} />
            </label>
            <label>
              Hasta
              <input type="time" value={form.timeEnd} onChange={(e) => set('timeEnd', e.target.value)} />
            </label>
          </div>
          <div className="days">
            {DAYS.map((d) => (
              <button
                key={d}
                type="button"
                className={`day ${form.daysOfWeek.includes(d) ? 'on' : ''}`}
                onClick={() => toggleDay(d)}
              >
                {{ MON: 'L', TUE: 'M', WED: 'X', THU: 'J', FRI: 'V', SAT: 'S', SUN: 'D' }[d]}
              </button>
            ))}
          </div>
          <p className="muted small">Sin días seleccionados = todos los días.</p>
        </fieldset>

        <fieldset>
          <legend>Condiciones de sensor</legend>
          <div className="grid-2">
            <label>
              Temp. mín (°C)
              <input type="number" step="0.1" value={form.tempMin} onChange={(e) => set('tempMin', e.target.value)} />
            </label>
            <label>
              Temp. máx (°C)
              <input type="number" step="0.1" value={form.tempMax} onChange={(e) => set('tempMax', e.target.value)} />
            </label>
            <label>
              Humedad mín (%)
              <input type="number" step="0.1" value={form.humMin} onChange={(e) => set('humMin', e.target.value)} />
            </label>
            <label>
              Humedad máx (%)
              <input type="number" step="0.1" value={form.humMax} onChange={(e) => set('humMax', e.target.value)} />
            </label>
          </div>
        </fieldset>

        <fieldset>
          <legend>Comportamiento avanzado</legend>
          <div className="grid-2">
            <label>
              Prioridad (menor = más prioritaria)
              <input type="number" value={form.priority} onChange={(e) => set('priority', e.target.value)} />
            </label>
            <label>
              Histéresis (°C/%)
              <input type="number" step="0.1" min="0" value={form.hysteresis} onChange={(e) => set('hysteresis', e.target.value)} />
            </label>
          </div>
          <label>
            Cooldown entre conmutaciones (segundos)
            <input
              type="number" min="0"
              value={form.minSwitchIntervalSeconds}
              onChange={(e) => set('minSwitchIntervalSeconds', e.target.value)}
            />
          </label>
        </fieldset>

        {error && <p className="error">{error}</p>}
        <div className="row">
          <button type="submit" disabled={saving}>
            {saving ? 'Guardando…' : 'Guardar'}
          </button>
          <button type="button" className="secondary" onClick={onCancel}>
            Cancelar
          </button>
        </div>
      </form>
    </div>
  )
}
