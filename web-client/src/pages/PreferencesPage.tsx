/**
 * /preferences — the player's table setup (see `store/preferencesStore.ts`). Open to guests too:
 * their preferences live in this browser, and a signed-in account carries them across devices.
 */
import { useEffect } from 'react'
import { PreferencesPanel } from '@/components/preferences/PreferencesPanel'
import { AccountPage } from '@/components/profile/accountUi'
import { pageStyles as p } from '@/components/ui/PageShell'
import { useAuthStore } from '@/store/authStore'
import { syncPreferences } from '@/store/preferencesStore'

export function PreferencesPage() {
  const status = useAuthStore((st) => st.status)
  const accountsEnabled = useAuthStore((st) => st.accountsEnabled)
  const init = useAuthStore((st) => st.init)

  useEffect(() => {
    if (status === 'idle') void init()
  }, [status, init])

  // Pick up a change made on another device before the player starts editing here.
  useEffect(() => {
    if (status === 'authenticated') void syncPreferences()
  }, [status])

  return (
    <AccountPage title="Preferences" width="narrow">
      <div>
        <h1 className={p.h1}>Preferences</h1>
        <p className={p.lede}>
          {status === 'authenticated'
              ? 'Saved to your account, so they follow you to every device.'
              : accountsEnabled
                ? 'Saved in this browser. Sign in to keep them on every device.'
                : 'Saved in this browser.'}
        </p>
      </div>
      <PreferencesPanel />
    </AccountPage>
  )
}
