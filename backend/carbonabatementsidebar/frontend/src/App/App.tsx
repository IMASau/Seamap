import { useState } from 'react'

import './App.scss'

export default function App() {
    const [count, setCount] = useState(0)
    const urlParams = new URLSearchParams(window.location.search);
    const cqlFilter = urlParams.get('cql_filter');

    return (
        <>
            <div>
                {cqlFilter}
                <button onClick={() => setCount((count) => count + 1)}>
                    count is {count}
                </button>
            </div>
        </>
    )
}
