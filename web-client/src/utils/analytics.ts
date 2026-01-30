declare function gtag(...args: unknown[]): void

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
  function gtagFn(...args: unknown[]) {
    window.dataLayer.push(args)
  }
  ;(window as unknown as { gtag: typeof gtag }).gtag = gtagFn as typeof gtag

  gtagFn('js', new Date())
  gtagFn('config', GA_MEASUREMENT_ID)
}

export function trackEvent(name: string, params?: Record<string, unknown>) {
  if (ANALYTICS_ENABLED && typeof gtag !== 'undefined') {
    gtag('event', name, params)
  }
}

declare global {
  interface Window {
    dataLayer: unknown[]
  }
}
