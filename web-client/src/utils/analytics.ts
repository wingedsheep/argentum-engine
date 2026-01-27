declare function gtag(...args: any[]): void

export function trackEvent(name: string, params?: Record<string, any>) {
  if (typeof gtag !== 'undefined') {
    gtag('event', name, params)
  }
}
