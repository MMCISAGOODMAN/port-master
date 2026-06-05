/**
 * 扫描结果对比工具
 */

function rowKey(row) {
  return `${row.protocol}:${row.port}:${row.localAddress}:${row.foreignAddress}:${row.state}:${row.pid || ''}`
}

/** 对比两次扫描，返回 diff 映射 key -> 'new' | 'removed' | 'changed' */
export function diffScans(previous, current) {
  const diffMap = new Map()
  if (!previous?.length) return diffMap

  const prevMap = new Map()
  previous.forEach(r => prevMap.set(rowKey(r), r))

  const currKeys = new Set()
  current.forEach(r => {
    const key = rowKey(r)
    currKeys.add(key)
    if (!prevMap.has(key)) {
      diffMap.set(key, 'new')
    } else {
      const prev = prevMap.get(key)
      if (prev.processName !== r.processName || prev.state !== r.state) {
        diffMap.set(key, 'changed')
      }
    }
  })

  previous.forEach(r => {
    const key = rowKey(r)
    if (!currKeys.has(key)) {
      diffMap.set(key, 'removed')
    }
  })

  return diffMap
}

export function getDiffStats(diffMap) {
  let newCount = 0, removed = 0, changed = 0
  diffMap.forEach(v => {
    if (v === 'new') newCount++
    else if (v === 'removed') removed++
    else if (v === 'changed') changed++
  })
  return { newCount, removed, changed }
}

export { rowKey }
