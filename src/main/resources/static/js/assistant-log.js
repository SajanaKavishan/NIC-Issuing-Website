document.addEventListener('DOMContentLoaded', () => {
  const apiBase = '/api/assistant-logs';
  const logsBody = document.getElementById('logsBody');
  const newBtn = document.getElementById('newBtn');
  const formArea = document.getElementById('formArea');
  const logForm = document.getElementById('logForm');
  const logId = document.getElementById('logId');
  const logDate = document.getElementById('logDate');
  const logDesc = document.getElementById('logDesc');
  const cancelBtn = document.getElementById('cancelBtn');

  function isoDate(d) {
    if (!d) return '';
    const dt = new Date(d);
    const yyyy = dt.getFullYear();
    const mm = String(dt.getMonth() + 1).padStart(2, '0');
    const dd = String(dt.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  async function loadLogs() {
    logsBody.innerHTML = '<tr><td colspan="3" class="muted">Loading...</td></tr>';
    try {
      const res = await fetch(apiBase);
      if (!res.ok) throw new Error('Failed to fetch logs');
      let data = await res.json();
      data.sort((a,b) => (b.date || '').localeCompare(a.date || ''));
      if (!data.length) {
        logsBody.innerHTML = '<tr><td colspan="3" class="muted">No logs yet.</td></tr>';
        return;
      }
      logsBody.innerHTML = '';
      data.forEach(item => {
        const tr = document.createElement('tr');
        const tdDate = document.createElement('td');
        tdDate.textContent = item.date ? isoDate(item.date) : '';
        const tdDesc = document.createElement('td');
        tdDesc.textContent = item.description || '';
        const tdActions = document.createElement('td');
        tdActions.style.whiteSpace = 'nowrap';

        const editBtn = document.createElement('button');
        editBtn.className = 'btn-ghost-small small';
        editBtn.textContent = 'Edit';
        editBtn.addEventListener('click', () => openEdit(item));

        const delBtn = document.createElement('button');
        delBtn.className = 'btn-delete small';
        delBtn.textContent = 'Delete';
        delBtn.addEventListener('click', () => deleteLog(item));

        tdActions.appendChild(editBtn);
        tdActions.appendChild(document.createTextNode(' '));
        tdActions.appendChild(delBtn);

        tr.appendChild(tdDate);
        tr.appendChild(tdDesc);
        tr.appendChild(tdActions);
        logsBody.appendChild(tr);
      });
    } catch (e) {
      console.error(e);
      logsBody.innerHTML = '<tr><td colspan="3" class="muted">Error loading logs</td></tr>';
    }
  }

  function openNew() {
    logId.value = '';
    logDate.value = isoDate(new Date());
    logDesc.value = '';
    formArea.style.display = '';
    logDate.focus();
  }

  function openEdit(item) {
    logId.value = item.id;
    logDate.value = item.date ? isoDate(item.date) : '';
    logDesc.value = item.description || '';
    formArea.style.display = '';
    logDate.focus();
  }

  async function deleteLog(item) {
    if (!confirm('Delete this log?')) return;
    try {
      const res = await fetch(`${apiBase}/${item.id}`, { method: 'DELETE' });
      if (res.status === 204) {
        await loadLogs();
        alert('Deleted');
      } else {
        alert('Delete failed');
      }
    } catch (e) {
      console.error(e);
      alert('Delete error');
    }
  }

  newBtn.addEventListener('click', openNew);
  cancelBtn.addEventListener('click', () => { formArea.style.display = 'none'; });

  logForm.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const id = logId.value;
    const payload = {
      date: logDate.value,
      description: logDesc.value.trim()
    };
    if (!payload.date) { alert('Please choose a date'); return; }
    try {
      let res;
      if (id) {
        res = await fetch(`${apiBase}/${id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
      } else {
        res = await fetch(apiBase, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
      }
      if (res.ok) {
        formArea.style.display = 'none';
        await loadLogs();
      } else {
        const txt = await res.text();
        alert('Save failed: ' + txt);
      }
    } catch (e) {
      console.error(e);
      alert('Save error');
    }
  });

  // initial load
  loadLogs();
});

