export default function Field({ label, error, children, ...props }) {
  return (
    <label className="block text-sm">
      <span className="mb-1 block text-slate-600">{label}</span>
      {children || (
        <input className={`input ${error ? 'border-red-400' : ''}`} {...props} />
      )}
      {error && <span className="mt-1 block text-xs text-red-600">{error}</span>}
    </label>
  )
}