import { useEffect, useState } from 'react';
import { api, ApiError } from '../api/client';
import ErrorBanner from '../components/ErrorBanner';

const TIPOS_CUENTA = ['AHORROS', 'CORRIENTE'];
const ESTADOS = ['ACTIVA', 'INACTIVA', 'CANCELADA'];

export default function CuentasPage() {
  const [cuentas, setCuentas] = useState([]);
  const [clientes, setClientes] = useState([]);
  const [filtroClienteId, setFiltroClienteId] = useState('');
  const [form, setForm] = useState({ tipoCuenta: 'AHORROS', clienteId: '', exentaGmf: false });
  const [nuevoEstado, setNuevoEstado] = useState({});
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const cargarClientes = async () => {
    setClientes(await api.get('/clientes'));
  };

  const cargarCuentas = async (clienteId) => {
    setLoading(true);
    try {
      const query = clienteId ? `?clienteId=${clienteId}` : '';
      setCuentas(await api.get(`/cuentas${query}`));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudieron cargar las cuentas');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    cargarClientes().catch(() => setError('No se pudieron cargar los clientes'));
    cargarCuentas('');
  }, []);

  const handleFiltroChange = (e) => {
    const clienteId = e.target.value;
    setFiltroClienteId(clienteId);
    cargarCuentas(clienteId);
  };

  const handleFormChange = (e) => {
    const { name, type, value, checked } = e.target;
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await api.post('/cuentas', form);
      setForm({ tipoCuenta: 'AHORROS', clienteId: '', exentaGmf: false });
      await cargarCuentas(filtroClienteId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo crear la cuenta');
    }
  };

  const handleCambiarEstado = async (cuentaId) => {
    const estado = nuevoEstado[cuentaId];
    if (!estado) return;
    setError('');
    try {
      await api.patch(`/cuentas/${cuentaId}/estado`, { estado });
      await cargarCuentas(filtroClienteId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo cambiar el estado de la cuenta');
    }
  };

  const handleCambiarExencionGmf = async (cuenta) => {
    setError('');
    try {
      await api.patch(`/cuentas/${cuenta.id}`, { exentaGmf: !cuenta.exentaGmf });
      await cargarCuentas(filtroClienteId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo actualizar la exención de GMF');
    }
  };

  const nombreCliente = (clienteId) => {
    const cliente = clientes.find((c) => c.id === clienteId);
    return cliente ? `${cliente.nombres} ${cliente.apellido}` : clienteId;
  };

  return (
    <section>
      <h2>Cuentas</h2>
      <ErrorBanner message={error} onClose={() => setError('')} />

      <form className="form-card" onSubmit={handleSubmit}>
        <div className="form-grid">
          <label>
            Tipo de cuenta
            <select name="tipoCuenta" value={form.tipoCuenta} onChange={handleFormChange}>
              {TIPOS_CUENTA.map((tipo) => (
                <option key={tipo} value={tipo}>{tipo}</option>
              ))}
            </select>
          </label>
          <label>
            Cliente
            <select name="clienteId" value={form.clienteId} onChange={handleFormChange} required>
              <option value="" disabled>Selecciona un cliente</option>
              {clientes.map((cliente) => (
                <option key={cliente.id} value={cliente.id}>
                  {cliente.nombres} {cliente.apellido}
                </option>
              ))}
            </select>
          </label>
          <label className="checkbox-label">
            <input type="checkbox" name="exentaGmf" checked={form.exentaGmf} onChange={handleFormChange} />
            Exenta de GMF
          </label>
        </div>
        <div className="form-actions">
          <button type="submit">Crear cuenta</button>
        </div>
      </form>

      <label>
        Filtrar por cliente
        <select value={filtroClienteId} onChange={handleFiltroChange}>
          <option value="">Todos los clientes</option>
          {clientes.map((cliente) => (
            <option key={cliente.id} value={cliente.id}>
              {cliente.nombres} {cliente.apellido}
            </option>
          ))}
        </select>
      </label>

      {loading ? (
        <p>Cargando...</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Número</th>
              <th>Tipo</th>
              <th>Cliente</th>
              <th>Estado</th>
              <th>Saldo</th>
              <th>Saldo disponible</th>
              <th>Exenta GMF</th>
              <th>Cambiar estado</th>
            </tr>
          </thead>
          <tbody>
            {cuentas.map((cuenta) => (
              <tr key={cuenta.id}>
                <td>{cuenta.numeroCuenta}</td>
                <td>{cuenta.tipoCuenta}</td>
                <td>{cuenta.clienteNombreCompleto || nombreCliente(cuenta.clienteId)}</td>
                <td>{cuenta.estado}</td>
                <td>${Number(cuenta.saldo).toLocaleString('es-CO')}</td>
                <td>${Number(cuenta.saldoDisponible).toLocaleString('es-CO')}</td>
                <td>
                  <button type="button" className="secondary" onClick={() => handleCambiarExencionGmf(cuenta)}>
                    {cuenta.exentaGmf ? 'Sí' : 'No'}
                  </button>
                </td>
                <td className="actions">
                  <select
                    value={nuevoEstado[cuenta.id] || ''}
                    onChange={(e) => setNuevoEstado({ ...nuevoEstado, [cuenta.id]: e.target.value })}
                  >
                    <option value="" disabled>Selecciona</option>
                    {ESTADOS.map((estado) => (
                      <option key={estado} value={estado}>{estado}</option>
                    ))}
                  </select>
                  <button type="button" onClick={() => handleCambiarEstado(cuenta.id)}>Aplicar</button>
                </td>
              </tr>
            ))}
            {cuentas.length === 0 && (
              <tr>
                <td colSpan="8">No hay cuentas registradas todavía.</td>
              </tr>
            )}
          </tbody>
        </table>
      )}
    </section>
  );
}
