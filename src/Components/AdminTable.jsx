const AdminTable = ({ columns, rows, emptyMessage = 'No records found.' }) => {
  if (!rows?.length) return <div className="card card-body">{emptyMessage}</div>
  return <div className="table-responsive"><table className="table table-hover align-middle"><thead><tr>{columns.map((column) => <th key={column.key}>{column.label}</th>)}</tr></thead>
    <tbody>{rows.map((row) => <tr key={row.id}>{columns.map((column) => <td key={column.key}>{column.render ? column.render(row) : row[column.key] ?? '—'}</td>)}</tr>)}</tbody>
  </table></div>
}

export default AdminTable
