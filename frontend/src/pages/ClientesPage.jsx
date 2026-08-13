import { useEffect, useState } from 'react';
import { api, ApiError } from '../api/client';
import ErrorBanner from '../components/ErrorBanner';

const TIPOS_IDENTIFICACION = ['CC', 'CE', 'TI', 'PASAPORTE'];

const FORM_VACIO = {
  tipoIdentificacion: 'CC',
  numeroIdentificacion: '',
  nombres: '',
  apellido: '',
  correoElectronico: '',
  fechaNacimiento: '',
};

export default function ClientesPage() {
  const [clientes, setClientes] = useState([]);
  const [form, setForm] = useState(FORM_VACIO);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const cargarClientes = async () => {
    setLoading(true);
    try {
      setClientes(await api.get('/clientes'));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudieron cargar los clientes');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    cargarClientes();
  }, []);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (editingId) {
        await api.put(`/clientes/${editingId}`, form);
      } else {
        await api.post('/clientes', form);
      }
      setForm(FORM_VACIO);
      setEditingId(null);
      await cargarClientes();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo guardar el cliente');
    }
  };

  const handleEditar = (cliente) => {
    setEditingId(cliente.id);
    setForm({
      tipoIdentificacion: cliente.tipoIdentificacion,
      numeroIdentificacion: cliente.numeroIdentificacion,
      nombres: cliente.nombres,
      apellido: cliente.apellido,
      correoElectronico: cliente.correoElectronico,
      fechaNacimiento: cliente.fechaNacimiento,
    });
  };

  const handleCancelarEdicion = () => {
    setEditingId(null);
    setForm(FORM_VACIO);
  };

  const handleEliminar = async (id) => {
    setError('');
    try {
      await api.delete(`/clientes/${id}`);
      await cargarClientes();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo eliminar el cliente');
    }
  };

  return (
    <section>
      <h2>Clientes</h2>
      <ErrorBanner message={error} onClose={() => setError('')} />

      <form className="form-card" onSubmit={handleSubmit}>
        <div className="form-grid">
          <label>
            Tipo de identificación
            <select name="tipoIdentificacion" value={form.tipoIdentificacion} onChange={handleChange}>
              {TIPOS_IDENTIFICACION.map((tipo) => (
                <option key={tipo} value={tipo}>
                  {tipo}
                </option>
              ))}
            </select>
          </label>
          <label>
            Número de identificación
            <input name="numeroIdentificacion" value={form.numeroIdentificacion} onChange={handleChange} required />
          </label>
          <label>
            Nombres
            <input name="nombres" value={form.nombres} onChange={handleChange} required />
          </label>
          <label>
            Apellido
            <input name="apellido" value={form.apellido} onChange={handleChange} required />
          </label>
          <label>
            Correo electrónico
            <input type="email" name="correoElectronico" value={form.correoElectronico} onChange={handleChange} required />
          </label>
          <label>
            Fecha de nacimiento
            <input type="date" name="fechaNacimiento" value={form.fechaNacimiento} onChange={handleChange} required />
          </label>
        </div>
        <div className="form-actions">
          <button type="submit">{editingId ? 'Guardar cambios' : 'Crear cliente'}</button>
          {editingId && (
            <button type="button" className="secondary" onClick={handleCancelarEdicion}>
              Cancelar
            </button>
          )}
        </div>
      </form>

      {loading ? (
        <p>Cargando...</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Identificación</th>
              <th>Nombre completo</th>
              <th>Correo</th>
              <th>Fecha nacimiento</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {clientes.map((cliente) => (
              <tr key={cliente.id}>
                <td>{cliente.tipoIdentificacion} {cliente.numeroIdentificacion}</td>
                <td>{cliente.nombres} {cliente.apellido}</td>
                <td>{cliente.correoElectronico}</td>
                <td>{cliente.fechaNacimiento}</td>
                <td className="actions">
                  <button type="button" onClick={() => handleEditar(cliente)}>Editar</button>
                  <button type="button" className="danger" onClick={() => handleEliminar(cliente.id)}>Eliminar</button>
                </td>
              </tr>
            ))}
            {clientes.length === 0 && (
              <tr>
                <td colSpan="5">No hay clientes registrados todavía.</td>
              </tr>
            )}
          </tbody>
        </table>
      )}
    </section>
  );
}
