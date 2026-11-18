'use strict';

const content = document.querySelector('#content');
const warehouseSelect = document.querySelector('#warehouse');
const notice = document.querySelector('#notice');
const names = {
  INBOUND: '入库', OUTBOUND: '出库', RETURN: '销售退货', TRANSFER: '移库',
  REPLENISH: '补货', COUNT: '盘点', DRAFT: '草稿', APPROVED: '已审核',
  EXECUTING: '执行中', PICKED: '待复核打包', PACKED: '待发运', COUNTED: '待确认差异',
  COMPLETED: '已完成', CANCELLED: '已取消', READY: '待派发', RUNNING: '执行中',
  DONE: '已完成', EXCEPTION: '异常待核对', SENT: '待开始', STARTED: '已开始',
  SUCCEEDED: '成功', FAILED: '失败', STORAGE: '存储位', PICK_FACE: '拣选位',
  QUARANTINE: '隔离位', SIMULATOR: '模拟', MANUAL: '人工',
  ENABLED: '启用', DISABLED: '停用', APPROVE: '审核', RECEIVE: '确认收货',
  RELEASE: '释放任务', PACK: '复核打包', SHIP: '确认发运', ACCEPT_COUNT: '确认盘点差异',
  CANCEL: '取消单据', UNPICK_CANCEL: '确认已退拣并取消', RESERVE: '预留', RELEASE_STOCK: '释放预留',
  PUTAWAY: '上架入账', MOVE_OUT: '移出', MOVE_IN: '移入', COUNT_ADJUST: '盘点调整',
  WAVE_RELEASE: '波次释放', RELEASED: '已释放', DISPATCH: '派发', CONFIRM_COMPLETED: '确认实际完成', CONFIRM_NO_MOVEMENT: '确认未作业'
};
const pages = { documents: '业务单据', inventory: '库存与流水', waves: '出库波次', execution: '执行与设备', settings: '仓库与主数据' };
const pageSize = 30;
let page = 'documents';
let warehouseId = '';
let catalog = { skus: [], locations: [], devices: [] };
let selectedDocument = '';
let selectedTask = '';
let selectedWave = '';
let offsets = {};
let revision = 0;

function el(tag, attributes = {}, ...children) {
  const node = document.createElement(tag);
  for (const [key, value] of Object.entries(attributes)) {
    if (key === 'className') node.className = value;
    else if (key.startsWith('on')) node.addEventListener(key.slice(2), value);
    else if (value !== false) node.setAttribute(key, value === true ? '' : value);
  }
  node.append(...children.flat(Infinity).map(child => child == null ? '' : child));
  return node;
}

const label = value => names[value] || value || '—';
const badge = state => el('span', { className: 'pill', 'data-state': state }, label(state));
const named = (items, id) => items.find(item => item.id === id)?.code || id || '—';
const locationCodeFor = id => named(catalog.locations, id);
const sku = id => named(catalog.skus, id);
const card = (title, ...children) => el('article', { className: 'card' }, el('h2', {}, title), children);
const button = (text, run, className = 'secondary') => el('button', { type: 'button', className, onclick: run }, text);
const submit = text => el('button', { type: 'submit' }, text);
const field = (text, control) => el('label', { className: 'field' }, text, control);
const input = (name, attrs = {}) => el('input', { name, ...attrs });

function select(name, values, blank) {
  const control = el('select', { name }, blank ? el('option', { value: '' }, blank) : null,
    values.map(([value, text]) => el('option', { value }, text)));
  return control;
}

async function api(path, method = 'GET', body) {
  const response = await fetch('/api' + path, {
    method, ...(body === undefined ? {} : { headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
  });
  const result = await response.json();
  if (!response.ok) throw new Error(result.message || '请求失败');
  return result;
}

function message(text, error = false) {
  notice.textContent = text;
  notice.classList.toggle('error', error);
  notice.hidden = !text;
}

async function run(work) {
  try {
    message('');
    await work();
  } catch (error) {
    message(error.message, true);
  }
}

function form(children, work) {
  return el('form', { onsubmit: event => {
    event.preventDefault();
    const target = event.currentTarget;
    const values = new FormData(target);
    const button = target.querySelector('button[type=submit]');
    button.disabled = true;
    run(() => work(values)).finally(() => { button.disabled = false; });
  } }, children);
}

function table(headers, rows, empty) {
  if (!rows.length) return el('p', { className: 'muted' }, empty);
  return el('div', { className: 'table-scroll' }, el('table', {},
    el('thead', {}, el('tr', {}, headers.map(title => el('th', { scope: 'col' }, title)))),
    el('tbody', {}, rows.map(row => el('tr', {}, row.map(cell => el('td', {}, cell ?? '—')))))));
}

function pagination(key, size) {
  const offset = offsets[key] || 0;
  if (!offset && size < pageSize) return null;
  const move = (direction) => () => run(async () => {
    offsets[key] = offset + direction * pageSize;
    await render();
  });
  const back = button('上一页', move(-1));
  const next = button('下一页', move(1));
  back.disabled = !offset;
  next.disabled = size < pageSize;
  return el('div', { className: 'row pagination' }, back, `第 ${offset / pageSize + 1} 页`, next);
}

function scoped(resource) { return `/warehouses/${warehouseId}/${resource}`; }
function listing(resource) { return api(scoped(resource) + `?limit=${pageSize}&offset=${offsets[resource] || 0}`); }

function openPage(target) {
  page = target;
  for (const nav of document.querySelectorAll('nav button')) {
    if (nav.dataset.page === page) nav.setAttribute('aria-current', 'page');
    else nav.removeAttribute('aria-current');
  }
  document.querySelector('#title').textContent = pages[page];
  return render();
}

async function refreshWarehouses() {
  const warehouses = await api('/warehouses');
  if (!warehouses.some(item => item.id === warehouseId)) warehouseId = warehouses[0]?.id || '';
  warehouseSelect.replaceChildren(...(warehouses.length
    ? warehouses.map(item => el('option', { value: item.id }, `${item.code} · ${item.name}`))
    : [el('option', { value: '' }, '暂无仓库')]));
  warehouseSelect.value = warehouseId;
  warehouseSelect.disabled = !warehouses.length;
}

async function render() {
  const token = ++revision;
  if (warehouseId) {
    const data = await api(scoped('catalog'));
    if (token !== revision) return;
    catalog = data;
  }
  let view;
  if (page === 'settings') view = settingsPage();
  else if (!warehouseId) {
    view = card('开始仓库作业', el('p', {}, '创建仓库、商品及库位后即可建单，也可以载入示例。'),
      el('div', { className: 'actions' }, button('配置仓库', () => run(() => openPage('settings')), ''), demoButton()));
  } else if (page === 'documents') view = await documentPage();
  else if (page === 'inventory') view = await inventoryPage();
  else if (page === 'waves') view = await wavePage();
  else view = await executionPage();
  if (token === revision) content.replaceChildren(...[].concat(view));
}

function masterForm(title, resource, allowed, fieldName) {
  return card(title, form([
    el('div', { className: 'fields' },
      field('编码', input('code', { required: true, pattern: '[A-Za-z0-9._\\-]{1,40}', maxlength: '40' })),
      field('名称', input('name', { required: true, maxlength: '100' })),
      allowed ? field('类型', select(fieldName, allowed.map(value => [value, label(value)]))) : null),
    submit('新增')
  ], async values => {
    const created = await api(resource, 'POST', Object.fromEntries(values));
    if (resource === '/warehouses') {
      warehouseId = created.id;
      selectedDocument = selectedTask = selectedWave = '';
      offsets = {};
      await refreshWarehouses();
    }
    await render();
    message(`已新增 ${created.code}。`);
  }));
}

function demoButton() {
  return button('载入演示数据', () => run(async () => {
    const current = await api('/warehouses');
    let warehouse = current.find(item => item.code === 'DEMO');
    if (!warehouse) warehouse = await api('/warehouses', 'POST', { code: 'DEMO', name: '示范仓' });
    const path = `/warehouses/${warehouse.id}`;
    const existing = await api(path + '/catalog');
    const ensure = (name, item) => existing[name].find(entry => entry.code === item.code)
      || api(path + '/' + name, 'POST', item);
    const item = await ensure('skus', { code: 'SKU-100', name: '样品料箱' });
    const storage = await ensure('locations', { code: 'A-01', name: '存储区 A01', locationType: 'STORAGE' });
    await ensure('locations', { code: 'P-01', name: '拣货区 P01', locationType: 'PICK_FACE' });
    await ensure('locations', { code: 'Q-01', name: '退货隔离区', locationType: 'QUARANTINE' });
    await ensure('devices', { code: 'SIM-01', name: '模拟作业站', deviceMode: 'SIMULATOR' });
    const doc = await api(path + '/documents', 'POST', { kind: 'INBOUND', externalRef: 'DEMO-IN-001',
      lines: [{ skuId: item.id, batchCode: 'BATCH-001', destinationId: storage.id, quantity: 24 }] });
    warehouseId = warehouse.id;
    selectedDocument = doc.id;
    selectedTask = selectedWave = '';
    offsets = {};
    await refreshWarehouses();
    await openPage('documents');
    message('演示单已载入。审核、收货后可派发到模拟作业站。');
  }));
}

function settingsPage() {
  const forms = [masterForm('仓库', '/warehouses')];
  if (warehouseId) forms.push(masterForm('商品', scoped('skus')),
    masterForm('库位', scoped('locations'), ['STORAGE', 'PICK_FACE', 'QUARANTINE'], 'locationType'),
    masterForm('作业设备', scoped('devices'), ['SIMULATOR', 'MANUAL'], 'deviceMode'));
  return [el('div', { className: 'action-bar' }, demoButton()),
    el('div', { className: 'cards' }, forms),
    warehouseId ? el('div', { className: 'cards' },
      card('商品目录', table(['编码', '名称'], catalog.skus.map(item => [item.code, item.name]), '暂无商品')),
      card('库位目录', table(['编码', '名称', '用途'], catalog.locations.map(item => [item.code, item.name, label(item.locationType)]), '暂无库位'))) : null]
    .filter(Boolean);
}

function documentForm() {
  const kind = select('kind', ['INBOUND', 'OUTBOUND', 'TRANSFER', 'REPLENISH', 'COUNT', 'RETURN'].map(value => [value, label(value)]));
  const lines = el('div');
  const sourceDocument = input('sourceDocumentId', { maxlength: '36' });
  const sourceField = field('原出库单 ID', sourceDocument);

  function addLine() {
    const skuOptions = catalog.skus.map(item => [item.id, `${item.code} · ${item.name}`]);
    const locations = catalog.locations.map(item => [item.id, `${item.code} · ${label(item.locationType)}`]);
    const current = kind.value;
    const hasSource = ['OUTBOUND', 'TRANSFER', 'REPLENISH', 'COUNT'].includes(current);
    const hasDestination = ['INBOUND', 'RETURN', 'TRANSFER', 'REPLENISH'].includes(current);
    const skuSelect = select('skuId', skuOptions, '请选择');
    const from = select('sourceId', locations, current === 'OUTBOUND' ? '自动分配' : '请选择');
    const to = select('destinationId', locations, '请选择');
    skuSelect.required = true;
    from.required = current !== 'OUTBOUND';
    to.required = true;
    const group = el('fieldset', {}, el('legend', {}, '单据明细'), el('div', { className: 'fields' },
      field('商品', skuSelect),
      field(current === 'OUTBOUND' ? '批次（可选）' : '批次', input('batchCode', { required: current !== 'OUTBOUND', maxlength: '40' })),
      current === 'INBOUND' ? field('有效期（可选）', input('expiresOn', { type: 'date' })) : null,
      hasSource ? field('源库位', from) : null, hasDestination ? field('目标库位', to) : null,
      current !== 'COUNT' ? field('数量', input('quantity', { type: 'number', min: '1', max: '1000000000', required: true })) : null,
      button('移除此行', () => { if (lines.children.length > 1) group.remove(); })));
    lines.append(group);
  }

  function updateType() {
    sourceField.hidden = kind.value !== 'RETURN';
    sourceDocument.required = kind.value === 'RETURN';
    sourceField.style.display = sourceField.hidden ? 'none' : '';
    lines.replaceChildren();
    addLine();
  }
  kind.addEventListener('change', updateType);
  updateType();
  return form([
    el('div', { className: 'fields' }, field('业务类型', kind),
      field('外部单号', input('externalRef', { maxlength: '64', required: true })), sourceField),
    lines, el('div', { className: 'actions' },
      button('添加明细', () => { if (lines.children.length < 100) addLine(); }), submit('创建单据'))
  ], async values => {
    const details = [...lines.children].map(group => {
      const line = {};
      for (const control of group.querySelectorAll('input,select')) {
        if (control.value) line[control.name] = control.name === 'quantity' ? Number(control.value) : control.value;
      }
      return line;
    });
    const payload = { kind: values.get('kind'), externalRef: values.get('externalRef'), lines: details };
    if (payload.kind === 'RETURN') payload.sourceDocumentId = values.get('sourceDocumentId');
    const created = await api(scoped('documents'), 'POST', payload);
    selectedDocument = created.id;
    await render();
    message(`已打开单据 ${created.externalRef}。`);
  });
}

async function documentPage() {
  const [rows, detail] = await Promise.all([listing('documents'),
    selectedDocument ? api('/documents/' + selectedDocument) : null]);
  return [card('单据列表', el('details', { className: 'action-bar' }, el('summary', {}, '新建单据'), documentForm()),
    table(['单号', '业务类型', '状态', '操作'], rows.map(doc => [doc.externalRef, label(doc.kind), badge(doc.state),
      button('查看', () => run(async () => { selectedDocument = doc.id; await render(); }))]), '暂无业务单据'),
    pagination('documents', rows.length)), ...(detail ? [documentDetail(detail)] : [])];
}

function documentDetail(detail) {
  const doc = detail.document;
  const options = {
    DRAFT: ['APPROVE', 'CANCEL'],
    APPROVED: [['INBOUND', 'RETURN'].includes(doc.kind) ? 'RECEIVE' : 'RELEASE', 'CANCEL'],
    EXECUTING: detail.tasks.every(item => item.state === 'READY') ? ['CANCEL']
      : doc.kind === 'OUTBOUND' && detail.tasks.every(item => ['READY', 'DONE'].includes(item.state)) ? ['UNPICK_CANCEL'] : [],
    PICKED: ['PACK', 'UNPICK_CANCEL'], PACKED: ['SHIP', 'UNPICK_CANCEL'], COUNTED: ['ACCEPT_COUNT']
  }[doc.state] || [];
  const action = select('action', options.map(value => [value, label(value)]));
  return card(`${doc.externalRef} · ${label(doc.kind)}`,
    el('p', { className: 'row' }, badge(doc.state), '单据 ID', el('code', {}, doc.id)),
    options.length ? form([el('div', { className: 'fields' }, field('业务动作', action),
      field('作业依据 / 原因', input('reason', { required: true, maxlength: '240' })), submit('提交'))],
    async values => {
      await api(`/documents/${doc.id}/actions/${values.get('action')}`, 'POST', { reason: values.get('reason') });
      await render();
      message('单据动作已提交。');
    }) : null,
    el('h3', {}, '单据明细'),
    table(['行', '商品', '批次', '源库位', '目标库位', '数量'], detail.lines.map(line => [line.lineNo,
      sku(line.skuId), line.batchCode, locationCodeFor(line.sourceId), locationCodeFor(line.destinationId),
      doc.kind === 'COUNT' ? '—' : line.quantity]), '暂无明细'),
    el('h3', {}, '执行任务'),
    taskTable(detail.tasks),
    el('h3', {}, '操作记录'),
    table(['动作', '依据', '时间'], detail.audit.map(entry =>
      [label(entry.action), entry.detail, timestamp(entry.createdAt)]), '暂无记录'));
}

function timestamp(value) { return value ? value.replace('T', ' ').slice(0, 19) : '—'; }

function taskTable(tasks) {
  return table(['任务', '批次', '源库位', '目标库位', '数量 / 账面数', '实盘', '状态', '操作'],
    tasks.map(task => [task.id.slice(0, 8), task.batchCode, locationCodeFor(task.sourceId), locationCodeFor(task.destinationId),
      task.quantity, task.observedQuantity, badge(task.state),
      button('查看执行', () => run(async () => { selectedTask = task.id; await openPage('execution'); }))]), '暂无任务');
}

async function inventoryPage() {
  const [stock, ledger] = await Promise.all([listing('inventory'), listing('ledger')]);
  const batches = new Map(stock.map(item => [item.lotId, item.batchCode]));
  return [card('库存余额', table(['商品', '库位', '用途', '批次', '有效期', '在库', '预留', '未预留'],
    stock.map(item => [item.skuCode, item.locationCode, label(item.locationType), item.batchCode,
      item.expiresOn, item.onHand, item.reserved, item.available]), '完成上架后即可查看库存余额'),
    pagination('inventory', stock.length)),
  card('库存流水', table(['动作', '库位', '批次', '在库变动', '预留变动', '时间'],
    ledger.map(item => [label(item.reason === 'RELEASE' ? 'RELEASE_STOCK' : item.reason), locationCodeFor(item.locationId),
      item.batchCode || batches.get(item.lotId) || item.lotId.slice(0, 8), item.deltaOnHand, item.deltaReserved,
      timestamp(item.createdAt)]), '暂无库存流水'), pagination('ledger', ledger.length))];
}

async function wavePage() {
  const [waves, documents, detail] = await Promise.all([listing('waves'),
    api(scoped('documents') + '?limit=200'), selectedWave ? api('/waves/' + selectedWave) : null]);
  const approved = documents.filter(doc => doc.kind === 'OUTBOUND' && doc.state === 'APPROVED');
  const create = form([el('div', { className: 'fields' }, field('波次编号', input('code', { maxlength: '40', required: true })),
    field('优先级（0–100）', input('priority', { type: 'number', value: '10', min: '0', max: '100', required: true }))),
    table(['选择', '单号'], approved.map(doc => [input('documentIds', { type: 'checkbox', value: doc.id,
      'aria-label': '选择 ' + doc.externalRef }), doc.externalRef]), '暂无已审核的出库单'),
    submit('创建波次')], async values => {
    const documentIds = values.getAll('documentIds').reverse();
    if (!documentIds.length) throw new Error('请选择至少一张已审核出库单');
    const wave = await api(scoped('waves'), 'POST', { code: values.get('code'), priority: Number(values.get('priority')), documentIds });
    selectedWave = wave.id;
    await render();
    message('已创建波次。');
  });
  const list = card('波次列表', el('details', { className: 'action-bar' }, el('summary', {}, '新建波次'), create),
    table(['编号', '优先级', '状态', '操作'], waves.map(wave => [wave.code, wave.priority, badge(wave.state),
      button('查看', () => run(async () => { selectedWave = wave.id; await render(); }))]), '暂无波次'),
    pagination('waves', waves.length));
  if (!detail) return [list];
  const wave = detail.wave;
  const actions = wave.state === 'DRAFT' ? form([el('div', { className: 'fields' },
    field('波次动作', select('action', [['RELEASE', '整波释放'], ['CANCEL', '取消草稿']])),
    field('作业依据 / 原因', input('reason', { required: true, maxlength: '240' })), submit('提交'))],
    async values => {
      await api(`/waves/${wave.id}/actions/${values.get('action')}`, 'POST', { reason: values.get('reason') });
      await render();
      message('波次动作已提交。');
    }) : null;
  return [list, card(wave.code, el('p', { className: 'row' }, badge(wave.state), `优先级 ${wave.priority}`, wave.reason),
    actions, table(['顺序', '出库单', '状态', '操作'], detail.documents.map((doc, index) =>
      [index + 1, doc.externalRef, badge(doc.state), button('查看单据',
        () => run(async () => { selectedDocument = doc.id; await openPage('documents'); }))]), '暂无关联单据'))];
}

async function executionPage() {
  const [queue, selected] = await Promise.all([listing('tasks'),
    selectedTask ? api(`/tasks/${selectedTask}`) : null]);
  const detail = selected ? executionDetail(selected.task, selected.document, selected.commands, selected.events) : null;
  return [card('作业队列', taskTable(queue), pagination('tasks', queue.length)), ...(detail ? [detail] : []),
    card('设备状态', table(['编码', '名称', '模式', '状态', '操作'], catalog.devices.map(device =>
      [device.code, device.name, label(device.deviceMode), badge(device.state),
        button(device.state === 'ENABLED' ? '停用' : '启用', () => run(async () => {
          await api(`/devices/${device.id}/state`, 'PUT', { state: device.state === 'ENABLED' ? 'DISABLED' : 'ENABLED' });
          await render();
        }))]), '在主数据中登记设备后即可派发'))];
}

function executionDetail(task, document, commands, events) {
  const command = commands[commands.length - 1];
  let operations;
  if (task.state === 'READY') {
    operations = form([el('div', { className: 'fields' }, field('工作站 / 设备',
      select('deviceId', catalog.devices.filter(item => item.state === 'ENABLED')
        .map(item => [item.id, `${item.code} · ${label(item.deviceMode)}`]), '请选择')), submit('派发'))],
    async values => {
      await api(`/tasks/${task.id}/dispatch`, 'POST', { deviceId: values.get('deviceId') });
      await render();
      message('已创建设备指令。');
    });
  } else if (task.state === 'RUNNING' && command) {
    const eventTypes = command.state === 'SENT' ? ['STARTED', 'FAILED'] : ['SUCCEEDED', 'FAILED'];
    operations = form([el('div', { className: 'fields' },
      field('回执', select('eventType', eventTypes.map(item => [item, label(item)]))),
      document.kind === 'COUNT' && command.state === 'STARTED'
        ? field('实盘数（成功时填写）', input('observedQuantity', { type: 'number', min: '0', max: '1000000000000' })) : null,
      field('回执备注（失败必填）', input('detail', { maxlength: '240' })), submit('提交回执'))], async values => {
      const eventType = values.get('eventType');
      const payload = { eventType, eventKey: eventType.toLowerCase() };
      if (values.get('detail')) payload.detail = values.get('detail');
      if (eventType === 'SUCCEEDED' && values.get('observedQuantity')) payload.observedQuantity = Number(values.get('observedQuantity'));
      await api(`/commands/${command.id}/events`, 'POST', payload);
      await render();
      message('回执已处理。');
    });
  } else if (task.state === 'EXCEPTION') {
    operations = form([el('div', { className: 'fields' }, field('现场核对结论', select('completed',
      [['', '请选择'], ['false', '确认未作业，重新进入队列'], ['true', '确认已作业，完成任务']])),
    document.kind === 'COUNT' ? field('实盘数（实际完成时填写）', input('observedQuantity',
      { type: 'number', min: '0', max: '1000000000000' })) : null,
    field('核对依据', input('reason', { required: true, maxlength: '240' })), submit('确认核对'))], async values => {
      if (!values.get('completed')) throw new Error('请选择现场核对结论');
      const payload = { completed: values.get('completed') === 'true', reason: values.get('reason') };
      if (payload.completed && values.get('observedQuantity')) payload.observedQuantity = Number(values.get('observedQuantity'));
      await api(`/tasks/${task.id}/reconcile`, 'POST', payload);
      await render();
      message('已保存现场核对结果。');
    });
  }
  return card(`任务 ${task.id.slice(0, 8)} · ${document.externalRef}`,
    el('p', { className: 'row' }, badge(task.state),
      button('查看关联单据', () => run(async () => { selectedDocument = document.id; await openPage('documents'); }))),
    operations,
    table(['轮次', '设备', '指令状态', '指令 ID'], commands.map(item =>
      [item.attempt, named(catalog.devices, item.deviceId), badge(item.state), item.id]), '暂无指令'),
    command ? table(['回执编号', '回执', '实盘数', '备注'], events.map(item =>
      [item.eventKey, badge(item.eventType), item.observedQuantity, item.detail]), '暂无本轮回执') : null);
}

for (const nav of document.querySelectorAll('nav button')) nav.addEventListener('click', () => run(() => openPage(nav.dataset.page)));
warehouseSelect.addEventListener('change', () => run(async () => {
  warehouseId = warehouseSelect.value;
  selectedDocument = selectedTask = selectedWave = '';
  offsets = {};
  await render();
}));
document.querySelector('#refresh').addEventListener('click', () => run(render));
run(async () => { await refreshWarehouses(); await render(); });
