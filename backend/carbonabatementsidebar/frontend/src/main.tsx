import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App/App.tsx'

import '@blueprintjs/core/lib/css/blueprint.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <App />
    </React.StrictMode>,
)
