import { useEffect, useState } from 'react';
import { api, ApiError } from '../api/client';
import ErrorBanner from '../components/ErrorBanner';

const FORM_CONSIGNACION = { cuentaId: '', valor: '' };
const FORM_RETIRO = { cuentaId: '', valor: '' };
const FORM_TRANSFERENCIA = { cuentaOrigenId: '', cuentaDestinoId: '', valor: '' };

export default function TransaccionesPage() {
  const [cuentas, setCuentas] = useState([]);
  const [error, setError] = useState('');
  const [mensaje, setMensaje] = useState('');

  const [consignacion, setConsignacion] = useState(FORM_CONSIGNACION);
  const [retiro, setRetiro] = useState(FORM_RETIRO);
  const [transferencia, setTransferencia] = useState(FORM_TRANSFERENCIA);

  const [cuentaConsulta, setCuentaConsulta] = useState('');
  const [estadoCuenta, setEstadoCuenta] = useState(null);

  const cargarCuentas = async () => {
    try {
      setCuentas(await api.get('/cuentas'));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudieron cargar las cuentas');
    }
  };

  useEffect(() => {
    cargarCuentas();
  }, []);

  const opcionesCuenta = () =>
    cuentas.map((cuenta) => (
      <option key={cuenta.id} value={cuenta.id}>
        {cuenta.numeroCuenta} · {cuenta.tipoCuenta} · {cuenta.clienteNombreCompleto}
      </option>
    ));

  const consultarEstado = async (cuentaId) => {
    if (!cuentaId) {
      setEstadoCuenta(null);
      return;
    }
    try {
      setEstadoCuenta(await api.get(`/transacciones/cuentas/${cuentaId}/estado`));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo consultar el estado de la cuenta');
    }
  };

  const handleConsignar = async (e) => {
    e.preventDefault();
    setError('');
    setMensaje('');
    try {
      await api.post('/transacciones/consignaciones', {
        cuentaId: consignacion.cuentaId,
        valor: Number(consignacion.valor),
      });
      setMensaje('Consignación registrada correctamente.');
      setConsignacion(FORM_CONSIGNACION);
      await cargarCuentas();
      if (cuentaConsulta) await consultarEstado(cuentaConsulta);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo registrar la consignación');
    }
  };

  const handleRetirar = async (e) => {
    e.preventDefault();
    setError('');
    setMensaje('');
    try {
      await api.post('/transacciones/retiros', {
        cuentaId: retiro.cuentaId,
        valor: Number(retiro.valor),
      });
      setMensaje('Retiro registrado correctamente.');
      setRetiro(FORM_RETIRO);
      await cargarCuentas();
      if (cuentaConsulta) await consultarEstado(cuentaConsulta);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo registrar el retiro');
    }
  };

  const handleTransferir = async (e) => {
    e.preventDefault();
    setError('');
    setMensaje('');
    try {
      await api.post('/transacciones/transferencias', {
        cuentaOrigenId: transferencia.cuentaOrigenId,
        cuentaDestinoId: transferencia.cuentaDestinoId,
        valor: Number(transferencia.valor),
      });
      setMensaje('Transferencia registrada correctamente.');
      setTransferencia(FORM_TRANSFERENCIA);
      await cargarCuentas();
      if (cuentaConsulta) await consultarEstado(cuentaConsulta);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo registrar la transferencia');
    }
  };

  return (
    <section>
      <h2>Transacciones</h2>
      <ErrorBanner message={error} onClose={() => setError('')} />
      {mensaje && <p className="success-message">{mensaje}</p>}

      <div className="transacciones-grid">
        <form className="form-card" onSubmit={handleConsignar}>
          <h3>Consignación</h3>
          <label>
            Cuenta
            <select
              value={consignacion.cuentaId}
              onChange={(e) => setConsignacion({ ...consignacion, cuentaId: e.target.value })}
              required
            >
              <option value="" disabled>Selecciona una cuenta</option>
              {opcionesCuenta()}
            </select>
          </label>
          <label>
            Valor
            <input
              type="number"
              min="0.01"
              step="0.01"
              value={consignacion.valor}
              onChange={(e) => setConsignacion({ ...consignacion, valor: e.target.value })}
              required
            />
          </label>
          <button type="submit">Consignar</button>
        </form>

        <form className="form-card" onSubmit={handleRetirar}>
          <h3>Retiro</h3>
          <label>
            Cuenta
            <select
              value={retiro.cuentaId}
              onChange={(e) => setRetiro({ ...retiro, cuentaId: e.target.value })}
              required
            >
              <option value="" disabled>Selecciona una cuenta</option>
              {opcionesCuenta()}
            </select>
          </label>
          <label>
            Valor
            <input
              type="number"
              min="0.01"
              step="0.01"
              value={retiro.valor}
              onChange={(e) => setRetiro({ ...retiro, valor: e.target.value })}
              required
            />
          </label>
          <button type="submit">Retirar</button>
        </form>

        <form className="form-card" onSubmit={handleTransferir}>
          <h3>Transferencia</h3>
          <label>
            Cuenta origen
            <select
              value={transferencia.cuentaOrigenId}
              onChange={(e) => setTransferencia({ ...transferencia, cuentaOrigenId: e.target.value })}
              required
            >
              <option value="" disabled>Selecciona una cuenta</option>
              {opcionesCuenta()}
            </select>
          </label>
          <label>
            Cuenta destino
            <select
              value={transferencia.cuentaDestinoId}
              onChange={(e) => setTransferencia({ ...transferencia, cuentaDestinoId: e.target.value })}
              required
            >
              <option value="" disabled>Selecciona una cuenta</option>
              {opcionesCuenta()}
            </select>
          </label>
          <label>
            Valor
            <input
              type="number"
              min="0.01"
              step="0.01"
              value={transferencia.valor}
              onChange={(e) => setTransferencia({ ...transferencia, valor: e.target.value })}
              required
            />
          </label>
          <button type="submit">Transferir</button>
        </form>
      </div>

      <section className="estado-cuenta">
        <h3>Estado de cuenta</h3>
        <label>
          Cuenta
          <select
            value={cuentaConsulta}
            onChange={(e) => {
              setCuentaConsulta(e.target.value);
              consultarEstado(e.target.value);
            }}
          >
            <option value="">Selecciona una cuenta</option>
            {opcionesCuenta()}
          </select>
        </label>

        {estadoCuenta && (
          <>
            <p>
              Saldo actual: <strong>${Number(estadoCuenta.cuenta.saldo).toLocaleString('es-CO')}</strong>
              {' '}({estadoCuenta.cuenta.estado})
            </p>
            <table>
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Tipo</th>
                  <th>Valor</th>
                  <th>Saldo disponible</th>
                  <th>Cuenta relacionada</th>
                  <th>Descripción</th>
                </tr>
              </thead>
              <tbody>
                {estadoCuenta.movimientos.map((mov) => (
                  <tr key={mov.id}>
                    <td>{mov.fecha ? new Date(mov.fecha).toLocaleString('es-CO') : '-'}</td>
                    <td>{mov.tipoMovimiento}</td>
                    <td>${Number(mov.valor).toLocaleString('es-CO')}</td>
                    <td>${Number(mov.saldoDisponible).toLocaleString('es-CO')}</td>
                    <td>{mov.numeroCuentaRelacionada || '-'}</td>
                    <td>{mov.descripcion || '-'}</td>
                  </tr>
                ))}
                {estadoCuenta.movimientos.length === 0 && (
                  <tr>
                    <td colSpan="6">Sin movimientos todavía.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </>
        )}
      </section>
    </section>
  );
}
