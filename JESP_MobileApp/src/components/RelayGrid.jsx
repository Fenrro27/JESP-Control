export default function RelayGrid({ relays, overrides, onToggle }) {
  return (
    <div className="relay-grid">
      {relays.map((on, i) => (
        <button
          key={i}
          className={`relay ${on ? 'on' : ''}`}
          onClick={() => onToggle(i, !on)}
        >
          <span className="relay-num">Relé {i + 1}</span>
          <span className="relay-state">{on ? 'ON' : 'OFF'}</span>
          {overrides[i] && <span className="relay-override">manual</span>}
        </button>
      ))}
    </div>
  )
}
