export async function copyTextToClipboard(text: string): Promise<boolean> {
  if (navigator.clipboard && window.isSecureContext) {
    try {
      await navigator.clipboard.writeText(text)
      return true
    } catch {
      // fall through to legacy path
    }
  }

  try {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.setAttribute('readonly', '')
    textarea.style.position = 'fixed'
    textarea.style.top = '0'
    textarea.style.left = '0'
    textarea.style.width = '1px'
    textarea.style.height = '1px'
    textarea.style.padding = '0'
    textarea.style.border = 'none'
    textarea.style.outline = 'none'
    textarea.style.boxShadow = 'none'
    textarea.style.background = 'transparent'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)

    const selection = document.getSelection()
    const savedRange = selection && selection.rangeCount > 0 ? selection.getRangeAt(0) : null

    textarea.focus()
    textarea.select()
    textarea.setSelectionRange(0, text.length)

    const ok = document.execCommand('copy')
    document.body.removeChild(textarea)

    if (savedRange && selection) {
      selection.removeAllRanges()
      selection.addRange(savedRange)
    }

    return ok
  } catch {
    return false
  }
}
