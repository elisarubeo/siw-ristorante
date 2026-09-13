interface Props {
    colonne: string[]
    righe: Array<{ chiave: string | number; celle: Array<string | number> }>
    etichetta?: string
}

export default function TabellaDati({ colonne, righe, etichetta = 'Mostra i numeri' }: Props) {
    return (
        <details className="tabella-dati">
            <summary>{etichetta}</summary>
            <div className="contenitore-tabella">
                <table>
                    <thead>
                        <tr>
                            {colonne.map((colonna, indice) => (
                                <th key={colonna} scope="col" className={indice > 0 ? 'numerica' : undefined}>
                                    {colonna}
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {righe.map((riga) => (
                            <tr key={riga.chiave}>
                                {riga.celle.map((cella, indice) => (
                                    indice === 0
                                        ? <th key={indice} scope="row" style={{ fontWeight: 400 }}>{cella}</th>
                                        : <td key={indice} className="numerica">{cella}</td>
                                ))}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </details>
    )
}
