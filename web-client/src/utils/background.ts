const backgroundModules = import.meta.glob('../assets/backgrounds/*.jpeg', { eager: true, query: '?url', import: 'default' }) as Record<string, string>
const backgroundUrls = Object.values(backgroundModules)

function pickBackground(): string {
  const HOUR_MS = 60 * 60 * 1000
  const stored = localStorage.getItem('argentum-bg')
  if (stored) {
    const { index, timestamp } = JSON.parse(stored)
    if (Date.now() - timestamp < HOUR_MS && backgroundUrls[index]) {
      return backgroundUrls[index]
    }
  }
  const index = Math.floor(Math.random() * backgroundUrls.length)
  localStorage.setItem('argentum-bg', JSON.stringify({ index, timestamp: Date.now() }))
  return backgroundUrls[index] ?? ''
}

export const randomBackground = pickBackground()
