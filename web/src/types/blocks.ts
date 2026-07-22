export type NoteBlock = TextBlock | ImageBlock | ChecklistBlock

export interface TextBlock {
  type: 'text'
  id: string
  markdown: string
}

export interface ImageBlock {
  type: 'image'
  id: string
  dataUrl: string
  fileName: string
}

export interface ChecklistBlock {
  type: 'checklist'
  id: string
  items: ChecklistItem[]
}

export interface ChecklistItem {
  id: string
  text: string
  checked: boolean
}

export function newId(): string {
  return crypto.randomUUID?.() || Math.random().toString(36).slice(2)
}

export function newTextBlock(markdown = ''): TextBlock {
  return { type: 'text', id: newId(), markdown }
}

export function newImageBlock(dataUrl: string, fileName: string): ImageBlock {
  return { type: 'image', id: newId(), dataUrl, fileName }
}

export function newChecklistBlock(): ChecklistBlock {
  return { type: 'checklist', id: newId(), items: [{ id: newId(), text: '', checked: false }] }
}

// ── Serialization (compatible with Android NoteBlockSerializer) ──

function encodeText(text: string): string {
  return btoa(unescape(encodeURIComponent(text)))
}

function decodeText(b64: string): string {
  try {
    return decodeURIComponent(escape(atob(b64)))
  } catch {
    return ''
  }
}

export function encodeBlocks(blocks: NoteBlock[]): string {
  const lines: string[] = []
  for (const block of blocks) {
    switch (block.type) {
      case 'text':
        lines.push(block.markdown)
        break
      case 'image':
        lines.push(`<!--media:image:${block.fileName}-->`)
        lines.push(`<!--mediadata:image:${block.dataUrl}-->`)
        break
      case 'checklist':
        lines.push('<!--checklist:start-->')
        for (const item of block.items) {
          const prefix = item.checked ? 'c' : 'u'
          lines.push(`<!--checklist:item:${prefix}:${encodeText(item.text)}-->`)
        }
        lines.push('<!--checklist:end-->')
        break
    }
  }
  return lines.join('\n')
}

export function decodeBlocks(raw: string): NoteBlock[] {
  if (!raw) return [newTextBlock()]

  const lines = raw.split('\n')
  const blocks: NoteBlock[] = []
  let currentText = ''
  let inChecklist = false
  let checklistItems: ChecklistItem[] = []

  const flushText = () => {
    if (currentText.trim()) {
      blocks.push(newTextBlock(currentText.trimEnd()))
    }
    currentText = ''
  }

  for (const line of lines) {
    if (line.startsWith('<!--checklist:start-->')) {
      flushText()
      inChecklist = true
      checklistItems = []
    } else if (line.startsWith('<!--checklist:end-->')) {
      inChecklist = false
      if (checklistItems.length > 0) {
        blocks.push({ type: 'checklist', id: newId(), items: checklistItems })
      }
      checklistItems = []
    } else if (line.startsWith('<!--checklist:item:')) {
      const match = line.match(/<!--checklist:item:([cu]):(.+)-->/)
      if (match) {
        checklistItems.push({
          id: newId(),
          checked: match[1] === 'c',
          text: decodeText(match[2]),
        })
      }
    } else if (line.startsWith('<!--mediadata:image:')) {
      const dataUrl = line.replace('<!--mediadata:image:', '').replace('-->', '')
      const prevImg = blocks[blocks.length - 1]
      if (prevImg?.type === 'image') {
        prevImg.dataUrl = dataUrl
      }
    } else if (line.startsWith('<!--media:image:')) {
      flushText()
      const fileName = line.replace('<!--media:image:', '').replace('-->', '')
      blocks.push(newImageBlock('', fileName))
    } else if (inChecklist) {
      // ignore stray lines in checklist
    } else {
      currentText += (currentText ? '\n' : '') + line
    }
  }

  flushText()
  if (blocks.length === 0) blocks.push(newTextBlock())
  return blocks
}
