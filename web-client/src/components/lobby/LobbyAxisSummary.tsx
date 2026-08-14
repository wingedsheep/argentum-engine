/**
 * The axes of the lobby you are in, as labelled chips in its header.
 *
 * Both lobby kinds show it, and everyone sees it — the tournament lobby's settings panel is
 * host-only, so before this a joining player had no way to tell what they had walked into beyond
 * a one-word format chip. It is also the thing that teaches the vocabulary: the words on these
 * chips are the words on the controls below them and on the home screen's mode cards.
 *
 * The Rules chip only appears when it is Commander. A chip reading "Rules: Standard" on every lobby
 * would be a row of noise for the default, but a joining player does need to know they have walked
 * into a Commander game — that is the one thing about a lobby which changes what deck they need.
 */
import {
  cardsLabel,
  cardsTopicId,
  eventLabel,
  eventTopicId,
  rulesLabel,
  rulesTopicId,
  tableLabel,
  tableTopicId,
  type AxisSelection,
} from './axes'
import { HelpTip } from '../help/HelpTip'
import styles from '../ui/GameUI.module.css'

export function LobbyAxisSummary({ axes }: { axes: AxisSelection }) {
  return (
    <div className={styles.axisSummary} data-testid="lobby-axis-summary">
      <AxisChip name="Cards" value={cardsLabel(axes.cards)} topicId={cardsTopicId(axes.cards)} />
      {axes.rules === 'COMMANDER' && (
        <AxisChip name="Rules" value={rulesLabel(axes.rules)} topicId={rulesTopicId(axes.rules)} />
      )}
      <AxisChip name="Table" value={tableLabel(axes.table)} topicId={tableTopicId(axes.table)} />
      <AxisChip name="Event" value={eventLabel(axes.event)} topicId={eventTopicId(axes.event)} />
    </div>
  )
}

function AxisChip({ name, value, topicId }: { name: string; value: string; topicId: string }) {
  return (
    <span className={styles.axisChip}>
      <span className={styles.axisChipName}>{name}</span>
      <span className={styles.axisChipValue}>{value}</span>
      <HelpTip topicId={topicId} size="sm" />
    </span>
  )
}
