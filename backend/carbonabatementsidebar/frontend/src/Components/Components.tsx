import './Components.scss'

export function AbatementSection({ title, children }: { title: string, children: React.ReactNode }) {
    return (
        <div className="abatement-section">
            <div className="abatement-section-heading">
                <h1>{title}</h1>
            </div>
            <div className="abatement-section-content">{children}</div>
        </div>
    )
}
