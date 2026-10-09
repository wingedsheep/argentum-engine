import { useNavigate } from 'react-router-dom'
import styles from '@/components/ui/GameUI.module.css'
import { GearIcon } from './PreferencesDialog'

/** Icon button into /preferences for the landing and lobby top bars — open to guests as well. */
export function PreferencesButton() {
  const navigate = useNavigate()
  return (
    <button
      type="button"
      className={styles.fullscreenButtonCompact}
      onClick={() => navigate('/preferences')}
      title="Preferences — auto-pass stops, card stacking, motion"
      aria-label="Preferences"
      data-testid="preferences-button"
    >
      <GearIcon />
    </button>
  )
}
