import React from 'react'
import ReactDOM from 'react-dom/client'
import 'core-js/actual/structured-clone';

import App from './App/App.tsx'

import '@blueprintjs/core/lib/css/blueprint.css';

declare global {
    interface Window {
        apiUrl?: string;
    }
}

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <App apiUrl={window.apiUrl ?? "api"} />
    </React.StrictMode>,
)
