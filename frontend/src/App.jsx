import { useState } from 'react';
import ClientesPage from './pages/ClientesPage';
import CuentasPage from './pages/CuentasPage';
import TransaccionesPage from './pages/TransaccionesPage';
import './App.css';

const TABS = [
  { id: 'clientes', label: 'Clientes', Component: ClientesPage },
  { id: 'cuentas', label: 'Cuentas', Component: CuentasPage },
  { id: 'transacciones', label: 'Transacciones', Component: TransaccionesPage },
];

function App() {
  const [activeTab, setActiveTab] = useState('clientes');
  const ActiveComponent = TABS.find((tab) => tab.id === activeTab).Component;

  return (
    <div className="app">
      <header className="app-header">
        <h1>Banco Prueba Técnica</h1>
        <p>Administración de clientes, cuentas y transacciones</p>
      </header>

      <nav className="tabs">
        {TABS.map((tab) => (
          <button
            key={tab.id}
            type="button"
            className={tab.id === activeTab ? 'tab active' : 'tab'}
            onClick={() => setActiveTab(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </nav>

      <main className="app-main">
        <ActiveComponent />
      </main>
    </div>
  );
}

export default App;
