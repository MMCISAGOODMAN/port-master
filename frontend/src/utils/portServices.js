/**
 * 知名端口服务名映射 (IANA 常用)
 */
const WELL_KNOWN_PORTS = {
  20: 'FTP-DATA', 21: 'FTP', 22: 'SSH', 23: 'Telnet', 25: 'SMTP',
  53: 'DNS', 67: 'DHCP', 68: 'DHCP', 69: 'TFTP', 80: 'HTTP',
  110: 'POP3', 123: 'NTP', 143: 'IMAP', 161: 'SNMP', 162: 'SNMP-Trap',
  443: 'HTTPS', 445: 'SMB', 465: 'SMTPS', 587: 'SMTP', 993: 'IMAPS',
  995: 'POP3S', 1433: 'MSSQL', 1521: 'Oracle', 2181: 'Zookeeper',
  3306: 'MySQL', 3389: 'RDP', 5432: 'PostgreSQL', 5672: 'RabbitMQ',
  5900: 'VNC', 6379: 'Redis', 8080: 'HTTP-Alt', 8443: 'HTTPS-Alt',
  8848: 'Nacos', 9090: 'Prometheus', 9092: 'Kafka', 9200: 'Elasticsearch',
  9300: 'ES-Transport', 11211: 'Memcached', 27017: 'MongoDB',
  3000: 'Node/React', 4173: 'Vite-Preview', 5173: 'Vite', 8000: 'HTTP-Dev',
  8888: 'HTTP-Alt', 9000: 'SonarQube', 9090: 'Prometheus', 27018: 'MongoDB'
}

/** HTTP/HTTPS 类端口，可快速浏览器打开 */
export const WEB_PORTS = new Set([80, 443, 3000, 4173, 5173, 8000, 8080, 8443, 8888, 9000, 9090])

export function getServiceName(port) {
  if (port == null) return ''
  return WELL_KNOWN_PORTS[port] || ''
}

export function isWebPort(port) {
  return WEB_PORTS.has(port)
}

export function getOpenUrl(row) {
  const port = row.port
  if (!port || row.state === 'FREE') return null
  const protocol = port === 443 || port === 8443 ? 'https' : 'http'
  let host = 'localhost'
  const addr = row.localAddress || ''
  if (addr.includes('127.0.0.1')) host = '127.0.0.1'
  else if (addr.startsWith('*:') || addr.startsWith('0.0.0.0:')) host = 'localhost'
  else if (addr.includes(':')) {
    const h = addr.split(':')[0].replace('*', 'localhost')
    if (h && h !== '0.0.0.0') host = h.replace(/^\[/, '').replace(/\]$/, '')
  }
  return `${protocol}://${host}:${port}`
}
