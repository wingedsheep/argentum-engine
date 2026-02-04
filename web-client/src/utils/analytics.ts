const ANALYTICS_ENABLED = import.meta.env.VITE_ANALYTICS_ENABLED === 'true'
const GA_MEASUREMENT_ID = import.meta.env.VITE_GA_MEASUREMENT_ID as string | undefined

export function initAnalytics() {
  if (!ANALYTICS_ENABLED || !GA_MEASUREMENT_ID) {
    return
  }

  // Load gtag.js script
  const script = document.createElement('script')
  script.async = true
  script.src = `https://www.googletagmanager.com/gtag/js?id=${GA_MEASUREMENT_ID}`
  document.head.appendChild(script)

  // Initialize gtag
  window.dataLayer = window.dataLayer || []
  window.gtag = function () {
    // eslint-disable-next-line prefer-rest-params
    window.dataLayer.push(arguments)
  }

  window.gtag('js', new Date())
  window.gtag('config', GA_MEASUREMENT_ID)
}

export function trackEvent(name: string, params?: Record<string, unknown>) {
  if (ANALYTICS_ENABLED && typeof window.gtag !== 'undefined') {
    window.gtag('event', name, params)
  }
}

/**
 * Set user properties for GA4 audience segmentation and realtime filtering.
 * Use this to track user state like "currently in a game".
 */
export function setUserProperties(properties: Record<string, unknown>) {
  if (ANALYTICS_ENABLED && typeof window.gtag !== 'undefined' && GA_MEASUREMENT_ID) {
    window.gtag('set', 'user_properties', properties)
  }
}

/**
 * Mark the user as currently in a game. This enables GA4 realtime
 * filtering to show "active players in games".
 */
export function setInGame(inGame: boolean) {
  setUserProperties({ in_game: inGame ? 'true' : 'false' })
}

declare global {
  interface Window {
    dataLayer: unknown[]
    gtag?: (...args: unknown[]) => void
  }
}
