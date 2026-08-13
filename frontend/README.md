# Frontend — Banco Prueba Técnica

Cliente React + Vite que consume la API REST del backend para administrar
clientes, cuentas y transacciones.

## Ejecutar en desarrollo

```bash
npm install
cp .env.example .env   # ajustar VITE_API_URL si el backend no está en localhost:8080
npm run dev
```

## Compilar para producción

```bash
npm run build
```

Genera los archivos estáticos en `dist/`. La imagen Docker (`Dockerfile`) construye
este build y lo sirve con nginx (ver `nginx.conf`).

## Estructura

- `src/api/client.js`: cliente HTTP mínimo sobre `fetch`, lee la URL base de la API
  desde `VITE_API_URL`.
- `src/pages/`: una página por módulo de negocio (Clientes, Cuentas, Transacciones).
- `src/components/ErrorBanner.jsx`: muestra los mensajes de error que devuelve la API.
