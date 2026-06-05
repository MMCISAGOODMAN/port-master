import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'

/**
 * 导出工具 - 支持 Excel / Markdown / TXT
 */
export function exportToExcel(data, filename = 'ports') {
  const rows = data.map(item => ({
    '协议': item.protocol,
    '端口': item.port,
    '本地地址': item.localAddress,
    '外部地址': item.foreignAddress,
    'PID': item.pid || '',
    '进程名': item.processName,
    '程序路径': item.programPath,
    '连接状态': item.state
  }))
  const ws = XLSX.utils.json_to_sheet(rows)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '端口列表')
  const buf = XLSX.write(wb, { bookType: 'xlsx', type: 'array' })
  saveAs(new Blob([buf], { type: 'application/octet-stream' }), `${filename}.xlsx`)
}

export function exportToMarkdown(data, filename = 'ports') {
  const header = '| 协议 | 端口 | 本地地址 | 外部地址 | PID | 进程名 | 程序路径 | 连接状态 |\n'
  const sep = '| --- | --- | --- | --- | --- | --- | --- | --- |\n'
  const rows = data.map(item =>
    `| ${item.protocol} | ${item.port} | ${item.localAddress} | ${item.foreignAddress} | ${item.pid || ''} | ${item.processName} | ${item.programPath} | ${item.state} |`
  ).join('\n')
  const content = header + sep + rows
  saveAs(new Blob([content], { type: 'text/markdown;charset=utf-8' }), `${filename}.md`)
}

export function exportToTxt(data, filename = 'ports') {
  const header = '协议\t端口\t本地地址\t外部地址\tPID\t进程名\t程序路径\t连接状态\n'
  const rows = data.map(item =>
    [item.protocol, item.port, item.localAddress, item.foreignAddress,
     item.pid || '', item.processName, item.programPath, item.state].join('\t')
  ).join('\n')
  saveAs(new Blob([header + rows], { type: 'text/plain;charset=utf-8' }), `${filename}.txt`)
}
