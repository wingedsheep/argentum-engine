/**
 * One lobby screen, whichever server implementation is behind it.
 *
 * Before this there were two: `QuickGameLobbyOverlay` (456 lines) and `LobbyOverlay` (1131), which
 * already shared a stylesheet — `QuickGameLobbyOverlay`'s header comment said so — but not a line
 * of structure. The visual language was identical and the *behaviour* had quietly diverged: only
 * one had a fullscreen button, only one showed a QR code, the two disagreed about who could see
 * the settings, and the axes could only be changed on one of them.
 *
 * The merge is over `UnifiedLobbyView` (`lobbyViewModel.ts`) rather than a shared base component,
 * so the screen never asks which kind it is looking at except where the kinds genuinely differ:
 * the deck section (a quick lobby auto-submits as you pick; a premade tournament lobby has an
 * explicit Submit) and the tournament-only knobs (`TournamentLobbySettings`).
 *
 * The payoff is the thing Phase 4 exists for: someone who entered via "vs AI" can now change the
 * Table to Free-for-All without backing out to the home screen, because the axis rows are the same
 * rows on both kinds and `axisChoices.ts` knows which ones need the lobby recreating.
 */
import { Fragment, useCallback, useEffect, useRef, useState, type ReactNode } from 'react'
import { useGameStore } from '@/store/gameStore'
import type { LobbyState } from '@/store/slices/types'
import { randomBackground } from '@/utils/background'
import { buildJoinUrl } from '@/utils/joinLink'
import { labelForFormat } from '@/utils/deckLegality'
import { JumpInEmblem } from './JumpInEmblem'
import momirVigUrl from '@/assets/momir-vig.svg'
import { DeckPicker, type DeckPickerTab } from '../ui/DeckPicker'
import { DeckPickerModal } from '../ui/DeckPickerModal'
import { FullscreenButton } from '../ui/FullscreenButton'
import { ChatButton } from '@/components/messages/ChatButton'
import { ArgentumMark } from '@/components/ui/ArgentumMark'
import { JoinQrModal } from '../ui/JoinQrModal'
import { SettingsLabel } from '../ui/SettingsLabel'
import {
  LobbyAiDeckModal,
  QuickAiDeckModal,
  aiDeckSummary,
  initialAiSource,
  type AiDeckSource,
} from './AiDeckChooser'
import {
  CardsAxisBody,
  CardsAxisStrip,
  EventAxisBody,
  EventAxisStrip,
  RulesAxisBody,
  RulesAxisStrip,
  TableAxisBody,
  TableAxisStrip,
  eventCaption,
} from './LobbyAxes'
import { SettingsGroup } from './SettingsGroup'
import lobby from './Lobby.module.css'
import { GROUP_IDS, groupLabel, groupTopicId } from './settingsGroups'
import { LobbyAxisSummary } from './LobbyAxisSummary'
import { TeamChip, TournamentLobbySettings } from './TournamentLobbySettings'
import { rulesFromLobbySettings } from './axes'
import { recreateTargetLabel, type RecreateSpec } from './axisChoices'
import { fromQuickGameLobby, fromTournamentLobby, type UnifiedLobbyView } from './lobbyViewModel'
import { takePendingLobbyIntent } from '@/store/slices/pendingLobbyIntent'
import { useSetupLibrary } from '@/store/setupLibrary'
import { useCaptureRecipe } from './useCaptureRecipe'
import { useLobbyCommands } from './useLobbyCommands'
import styles from '../ui/GameUI.module.css'

export function LobbyScreen() {
  const quickLobby = useGameStore((s) => s.quickGameLobbyState)
  const lobbyState = useGameStore((s) => s.lobbyState)
  const aiEnabled = useGameStore((s) => s.aiEnabled)
  const playerId = useGameStore((s) => s.playerId)
  const captureLast = useSetupLibrary((s) => s.captureLast)
  const saveSetup = useSetupLibrary((s) => s.saveSetup)

  // Deck-picker state the Cards axis needs to read: its validity gates the ready button, and its
  // tab *is* the Cards value on a quick lobby (Random pool is the Random tab).
  const [deckValid, setDeckValid] = useState(true)
  // Whatever created this lobby had things to say about it that no message could carry — which tab
  // the deck picker opens on, which saved deck to preselect, whether to start straight away, and
  // anything a setup couldn't restore. Read once, on mount. See `pendingLobbyIntent.ts`.
  const [intent] = useState(takePendingLobbyIntent)
  const [deckTab, setDeckTab] = useState<DeckPickerTab | undefined>(intent?.deckTab)
  const [copied, setCopied] = useState(false)
  // Which source the AI-deck control is on. Kept above the modal so closing it does not forget the
  // source while the server's updated summary is in flight.
  const [aiSource, setAiSource] = useState<AiDeckSource>(() => initialAiSource(quickLobby?.aiDeck))
  /** Which quick-lobby seat's deck modal is open. */
  const [quickDeckSeat, setQuickDeckSeat] = useState<'human' | 'ai' | null>(null)
  /**
   * Whether your quick-lobby deck picker is mounted — not the same as open.
   *
   * The picker is what submits a deck, so a lobby launched with one already decided (a Random deck,
   * or the saved deck named on the launch panel) mounts it hidden straight away; otherwise "Random
   * deck · Starts right away" would sit in the lobby until someone opened a modal to confirm it.
   * Once mounted it stays mounted, so reopening shows the deck you chose rather than a blank list.
   */
  const [deckPickerMounted, setDeckPickerMounted] = useState(
    () => intent?.deckTab === 'random' || Boolean(intent?.deckName) || Boolean(intent?.exampleName),
  )
  const [tournamentDeckOpen, setTournamentDeckOpen] = useState(false)
  /**
   * The premade-decks twin of {@link deckPickerMounted}: a deck named on the launch panel is
   * submitted without anyone opening the dialog, so "Constructed vs 3 AI" with a deck picked starts
   * on its own instead of stopping at a Choose-deck button whose answer is already known.
   */
  const [tournamentDeckMounted, setTournamentDeckMounted] = useState(
    () => Boolean(intent?.deckName) || Boolean(intent?.exampleName),
  )
  /** A deck submission the picker has debounced but not yet sent — readying now would race it. */
  const [deckSubmitPending, setDeckSubmitPending] = useState(false)
  const [pendingRecreate, setPendingRecreate] = useState<RecreateSpec | null>(null)
  /** Which AI seat's deck the host is choosing, by player id. Null = the modal is closed. */
  const [aiDeckSeat, setAiDeckSeat] = useState<string | null>(null)
  // Which saved deck is loaded, by name — the identity `onDeckChange`'s card list can't carry, and
  // the one thing a setup needs in order to bring the same deck back. See `lobbyRecipe.ts`.
  const [savedDeckName, setSavedDeckName] = useState<string | null>(null)
  const [exampleName, setExampleName] = useState<string | null>(null)
  /**
   * The deck your seat last submitted, by name, for the dialog's footer. Browsing to another tab
   * clears the picker's own selection but not the submission, so the footer keeps naming what
   * you would actually play.
   */
  const pickedNow = savedDeckName ?? exampleName ?? (deckTab === 'random' ? 'Random deck' : null)
  const [lastPicked, setLastPicked] = useState<string | null>(null)
  useEffect(() => { if (pickedNow) setLastPicked(pickedNow) }, [pickedNow])
  const [savingSetup, setSavingSetup] = useState(false)
  const [notes, setNotes] = useState<readonly string[]>(intent?.notes ?? [])

  const view: UnifiedLobbyView | null = quickLobby
    ? fromQuickGameLobby(quickLobby, { deckValid, deckTab, aiEnabled })
    : lobbyState
      ? fromTournamentLobby(lobbyState, { aiEnabled, playerId })
      : null

  const commands = useLobbyCommands(view, setDeckTab)
  const capture = useCaptureRecipe(view, lobbyState, quickLobby, deckTab, savedDeckName, exampleName)
  /**
   * Start the game, remembering what it was first.
   *
   * The primary action is the right moment and the only one: it is where the settings stop moving
   * and the deck has been chosen. The wizard is too early (it knows three of twenty-odd answers) and
   * the server is too late (it never learns which of *your* decks this is).
   */
  const runPrimary = () => {
    const kind = view?.primaryAction?.kind
    if (kind === 'START' || kind === 'READY') captureLast(capture().recipe)
    commands.runPrimary()
  }

  /**
   * Auto-start, for a setup that had nothing left to ask.
   *
   * Not a second launch path — the same button, pressed for you — so nothing here can drift from the
   * normal flow, and a recipe that only half-applied leaves you sitting in a correctly-built lobby
   * with its notes on screen instead of in a game you didn't mean to start.
   *
   * **Only when nobody can join.** A lobby with an invite code exists so that people can use it;
   * starting it the instant it opens would slam the door on them. A tournament lobby always has a
   * code, so one the host asked to fill with AI says so explicitly with `startWhenSeated`, and is
   * started once that many players — you and every AI seat — are in it.
   */
  const seatedForAutoStart = intent?.startWhenSeated !== undefined && view !== null && view.isHost &&
    view.primaryAction?.kind === 'START' && !view.primaryAction.disabled &&
    view.players.length >= intent.startWhenSeated
  // A quick lobby readies itself only for a deck the launch already named. "Choose in the lobby"
  // means the player is still deciding — readying the instant any valid deck appears would start
  // the game on the first tile they click, or halfway through pasting a list.
  const launchNamedDeck = intent?.deckTab === 'random' || Boolean(intent?.deckName) ||
    Boolean(intent?.exampleName) || view?.axes.cards.kind === 'MOMIR'
  const canAutoStart = intent?.autoStart === true && view !== null && (
    (!view.invitable && launchNamedDeck && view.primaryAction?.kind === 'READY' && !view.primaryAction.disabled) ||
    seatedForAutoStart
  )
  const autoStarted = useRef(false)
  useEffect(() => {
    if (!canAutoStart || autoStarted.current) return
    autoStarted.current = true
    runPrimary()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [canAutoStart])

  /**
   * A guest follows the host into a Random-deck game.
   *
   * Random is per seat on the server, so the host's choice never reached the guest: they joined a
   * game the host launched as "Random deck" and met a lobby titled Constructed asking them to choose
   * a deck. Once, on the first broadcast that shows the host on a rolled deck and you on nothing,
   * your picker is moved to Random and mounted so it submits — exactly what the host's own launch
   * did. Choosing something else afterwards is still one click on your row.
   */
  const followedHostRandom = useRef(false)
  const hostRollsRandom = quickLobby?.players.find((p) => !p.isAi)?.randomDeck === true
  const youHaveNoDeck = quickLobby?.players.find((p) => p.playerId === quickLobby.youPlayerId)?.deckSelected === false
  const youAreGuest = view !== null && !view.isHost
  useEffect(() => {
    if (followedHostRandom.current || !youAreGuest || !hostRollsRandom || !youHaveNoDeck) return
    if (deckPickerMounted || quickLobby?.momirBasic) return
    followedHostRandom.current = true
    setDeckTab('random')
    setDeckPickerMounted(true)
  }, [youAreGuest, hostRollsRandom, youHaveNoDeck, deckPickerMounted, quickLobby?.momirBasic])

  if (!view) return null

  const copyLobbyId = () => {
    navigator.clipboard.writeText(view.lobbyId)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  const showSettings = view.isWaiting && view.isHost
  const isMomir = view.axes.cards.kind === 'MOMIR'

  return (
    <div className={lobby.overlay} style={{ backgroundImage: `url(${randomBackground})` }}>
      <div className={lobby.artTint} aria-hidden />
      <header className={lobby.topBar}>
        <span className={lobby.brand}>
          <ArgentumMark size={34} />
          Argentum
        </span>
        <div className={lobby.topBarEnd}><ChatButton /><FullscreenButton compact /></div>
      </header>
      <div className={lobby.page}>
      <div className={lobby.layout} data-two-col={showSettings}>
      <div className={lobby.column}>
        <div className={lobby.header}>
          <div className={lobby.headerTop}>
            {isMomir && <MomirCrest />}
            {view.axes.cards.kind === 'JUMP_IN' && <JumpInEmblem className={lobby.headerEmblem} />}
            <h1 className={lobby.title}>{view.title}</h1>
          </div>
          <p className={lobby.subtitle}>{view.subtitle}</p>
          <LobbyAxisSummary axes={view.axes} jumpstart={Boolean(lobbyState?.settings.jumpstartActive)} />
          {view.isWaiting && view.isHost && (
            <button
              type="button"
              className={styles.saveSetupButton}
              data-testid="save-setup"
              onClick={() => setSavingSetup(true)}
              title="Save this lobby — sets, packs, timer, deck and all — to launch again in one click"
            >
              ★ Save setup
            </button>
          )}
        </div>

        {/* What a setup couldn't bring back, said once and dismissable.
            The server aborts an entire `updateLobbySettings` on a field it can't resolve — an unknown
            set code, a cube card that isn't implemented — so a partially-applied setup is a real
            outcome, and one that is invisible unless it is stated. */}
        {notes.length > 0 && (
          <div className={styles.lobbyNotes} role="status">
            <div className={styles.lobbyNotesBody}>
              {notes.map((note) => <p key={note}>{note}</p>)}
            </div>
            <button
              type="button"
              className={styles.lobbyNotesDismiss}
              aria-label="Dismiss"
              onClick={() => setNotes([])}
            >
              ×
            </button>
          </div>
        )}

        {view.invitable && (
          <div className={lobby.invite}>
            <button
              type="button"
              onClick={copyLobbyId}
              className={lobby.inviteButton}
              data-copied={copied}
              aria-label={copied ? 'Invite code copied' : `Copy invite code ${view.lobbyId}`}
            >
              <span className={lobby.inviteText}>
                <span className={lobby.inviteLabel}>Invite code</span>
                <span className={lobby.inviteCode} data-testid="invite-code">{view.lobbyId}</span>
              </span>
              <span className={lobby.inviteAction}>{copied ? 'Copied!' : 'Copy'}</span>
            </button>
            <JoinQrModal url={buildJoinUrl(view.lobbyId)} buttonClassName={lobby.qrButton} />
          </div>
        )}

        <div
          className={`${styles.lobbyGuidance} ${styles[`lobbyGuidance${capitalize(view.guidance.tone)}`]}`}
          role="status"
          data-testid="lobby-guidance"
        >
          <span className={styles.lobbyGuidanceIcon} aria-hidden>
            {view.guidance.tone === 'ready' ? '✓' : view.guidance.tone === 'action' ? '→' : '…'}
          </span>
          <div className={styles.lobbyGuidanceBody}>
            <strong>{view.guidance.title}</strong>
            <span>{view.guidance.detail}</span>
          </div>
        </div>

        {/* Every brought deck now belongs to a player row; no lobby puts a deck browser before its
            roster, whether it is backed by the quick or tournament implementation. */}
        <div className={styles.playerListPanel}>
          <div className={styles.playerListHeader}>
            <span className={styles.playerListTitle}>Players</span>
            <span className={styles.playerCount}>{view.players.length} / {view.maxPlayers}</span>
            {/* Beside the count rather than under a list that can be eight rows long — at a full
                pod the old placement put "+ Add AI Player" below the fold of its own panel. */}
            {view.canAddAi && (
              <button onClick={commands.addAi} className={styles.addAiInlineButton}>+ Add AI</button>
            )}
          </div>
          {seatRows(view).map(({ player, seat, teamHeader }) => (
            <Fragment key={player.playerId}>
              {teamHeader && (
                <div className={styles.teamHeader}>
                  <span>{teamHeader.label}</span>
                  <span className={teamHeader.balanced ? styles.teamHeaderCount : styles.teamHeaderCountOff}>
                    {teamHeader.have} / {teamHeader.need}
                  </span>
                </div>
              )}
            <div className={styles.playerRow}>
              <div className={styles.playerInfo}>
                {/* "Attack: left only" says left and right follow the seating order, and until now
                    the seating order was nowhere on screen. */}
                <span className={styles.seatNumber}>{seat}</span>
                <div className={`${styles.statusDot} ${player.isConnected ? styles.statusDotOnline : styles.statusDotOffline}`} />
                <span className={styles.playerName}>{player.name}</span>
                {player.isYou && <span className={styles.hostBadge}>You</span>}
                {player.isAi && <span className={styles.hostBadge}>AI</span>}
                {view.teams.mode !== 'NONE' && (
                  <TeamChip
                    team={view.teams.mode === 'MANUAL' ? (view.teams.byPlayerId[player.playerId] ?? 0) : null}
                    editable={view.isWaiting && view.isHost}
                    onClick={() => commands.togglePlayerTeam(player.playerId)}
                  />
                )}
                {player.isHost && <span className={styles.hostBadge}>Host</span>}
              </div>
              <div className={styles.playerActions}>
                {view.kind === 'QUICK' && !isMomir && player.isYou && (
                  <button
                    type="button"
                    onClick={() => { setDeckPickerMounted(true); setQuickDeckSeat('human') }}
                    disabled={player.tone === 'ready'}
                    className={`${styles.playerStatus} ${styles.playerDeckButton} ${player.needsDeck ? styles.playerDeckButtonCta : player.tone === 'ready' ? statusClass(player.tone) : styles.playerDeckButtonChosen}`}
                    title={player.tone === 'ready' ? 'Cancel ready before changing your deck' : 'Change your deck'}
                    data-testid="your-deck-button"
                  >
                    {/* Your own row is where a deck is chosen, so until one is it names the action
                        rather than reporting a state ("Choosing deck…" read as somebody else's).
                        Once chosen it names the deck you know — the server only knows "Custom (60)". */}
                    {player.needsDeck
                      ? 'Choose deck'
                      : ownDeckLabel(player.status, player.tone === 'ready', savedDeckName ?? exampleName)}{' '}
                    <span aria-hidden>✎</span>
                  </button>
                )}
                {view.kind === 'QUICK' && !isMomir && player.isAi && view.isHost && (
                  <button
                    type="button"
                    onClick={() => setQuickDeckSeat('ai')}
                    disabled={view.you?.tone === 'ready'}
                    className={`${styles.playerStatus} ${styles.playerDeckButton} ${statusClass(player.tone)}`}
                    title={view.you?.tone === 'ready' ? 'Cancel ready before changing the AI deck' : `Choose what ${player.name} plays`}
                  >
                    {player.status} <span aria-hidden>✎</span>
                  </button>
                )}
                {view.kind === 'TOURNAMENT' && view.isWaiting &&
                  view.axes.cards.kind === 'BRING_A_DECK' && player.isYou && (
                    <button
                      type="button"
                      onClick={() => { setTournamentDeckMounted(true); setTournamentDeckOpen(true) }}
                      className={`${styles.playerStatus} ${styles.playerDeckButton} ${player.tone === 'ready' ? statusClass(player.tone) : styles.playerDeckButtonCta}`}
                      title={player.tone === 'ready' ? 'View or change your submitted deck' : 'Choose and submit your deck'}
                    >
                      {/* Every other seat shows its state here, but on your own seat this is the only
                          control that submits a deck — so it says what pressing it does instead. */}
                      {player.tone === 'ready' ? '✓ Deck ready · Change' : 'Choose deck'}{' '}
                      <span aria-hidden>✎</span>
                    </button>
                )}
                {/* The AI's deck is the host's to pick only where the lobby deals it no pool; the
                    view model already answers that by leaving `aiDeck` null everywhere else. */}
                {view.isWaiting && view.isHost && player.isAi && player.aiDeck && (
                  <button
                    onClick={() => setAiDeckSeat(player.playerId)}
                    className={styles.settingsButton}
                    title={`Choose what ${player.name} plays`}
                  >
                    {aiDeckSummary(player.aiDeck)}
                  </button>
                )}
                {!(view.kind === 'QUICK' && !isMomir && (player.isYou || (player.isAi && view.isHost))) &&
                  !(view.kind === 'TOURNAMENT' && view.isWaiting &&
                    view.axes.cards.kind === 'BRING_A_DECK' && player.isYou) && (
                  <span className={`${styles.playerStatus} ${statusClass(player.tone)}`}>{player.status}</span>
                )}
                {view.isWaiting && view.isHost && player.isAi && (
                  <button
                    onClick={() => commands.removeAi(player.playerId)}
                    className={styles.removeAiButton}
                    title="Remove AI player"
                  >
                    ×
                  </button>
                )}
              </div>
            </div>
            </Fragment>
          ))}
          {view.players.length === 0 && (
            <div className={styles.emptyPlayerList}>Waiting for players to join...</div>
          )}
          {view.players.length === 1 && view.invitable && (
            <div
              className={styles.playerRow}
              style={{ borderBottom: 'none', justifyContent: 'center', color: 'var(--text-faint)', fontStyle: 'italic' }}
            >
              {view.maxPlayers > 2 ? 'Waiting for players…' : 'Waiting for opponent…'}
            </div>
          )}
        </div>

      </div>

        {/* Settings sit beside the players on a wide screen and below them on a narrow one. */}
        {showSettings && (
          <div className={`${lobby.column} ${lobby.settingsColumn}`}>
          <div className={styles.settingsPanel}>
            {GROUP_IDS.map((id) => {
              const axisStrip = {
                CARDS: <CardsAxisStrip view={view} commands={commands} onRecreate={setPendingRecreate} />,
                RULES: <RulesAxisStrip view={view} commands={commands} onRecreate={setPendingRecreate} />,
                TABLE: <TableAxisStrip view={view} commands={commands} onRecreate={setPendingRecreate} />,
                EVENT: <EventAxisStrip view={view} commands={commands} onRecreate={setPendingRecreate} />,
                LOBBY: null,
              }[id]
              // Built as a list so a group with neither an axis nor any relevant rows can disappear.
              const rows: ReactNode[] = []
              if (id === 'CARDS') rows.push(<CardsAxisBody key="axis" view={view} commands={commands} jumpstart={Boolean(lobbyState?.settings.jumpstartActive)} />)
              if (id === 'RULES') rows.push(<RulesAxisBody key="axis" view={view} commands={commands} />)
              if (id === 'TABLE') rows.push(<TableAxisBody key="axis" view={view} />)
              if (id === 'EVENT' && eventCaption(view) !== '') {
                rows.push(<EventAxisBody key="axis" view={view} />)
              }
              if (id === 'EVENT' && view.ranked.available) {
                rows.push(<RankedRow key="ranked" on={view.ranked.on} onChange={commands.setRanked} />)
              }
              // A lobby nobody can join has nothing to make public — the server forces a vs-AI
              // lobby private regardless (`QuickGameLobbyHandler.handleCreate`).
              if (id === 'LOBBY' && view.invitable) {
                rows.push(<VisibilityRow key="vis" isPublic={view.isPublic} onChange={commands.setPublic} />)
              }
              // Event is the one group whose tournament rows can *all* be absent — everything else
              // has either an unconditional row (AI assistance) or an axis caption. Asked here as one
              // condition rather than as a table of "which rows does this group have", which would be
              // a second copy of what `TournamentLobbySettings` already decides and would drift.
              if (lobbyState && view.kind === 'TOURNAMENT' &&
                  (id !== 'EVENT' || view.axes.event === 'ROUND_ROBIN')) {
                rows.push(
                  <TournamentLobbySettings key="tournament" group={id} view={view} lobbyState={lobbyState} />,
                )
              }
              // A group with neither a strip nor a row is not a group: a quick lobby has no
              // AI-assistance switch and no tournament rows, so "This lobby" would be an empty box.
              if (!axisStrip && rows.length === 0) return null
              return (
                <SettingsGroup
                  key={id}
                  label={groupLabel(id)}
                  topicId={groupTopicId(id, view)}
                  axisStrip={axisStrip}
                  blocking={view.blockGroup === id ? view.primaryAction?.reason : undefined}
                  testId={id.toLowerCase()}
                >
                  {rows.length > 0 ? rows : null}
                </SettingsGroup>
              )
            })}
          </div>
          </div>
        )}
      </div>
        <div className={lobby.actionBar}>
          <div className={lobby.actionBarInner}>
            <button onClick={commands.leave} className={lobby.leave} type="button">Leave</button>
            {/* Said, not just hovered: a `title` on a disabled button is the least discoverable
                place to put the one sentence explaining why it can't be pressed. */}
            {view.primaryAction?.disabled && view.primaryAction.reason && (
              <span className={lobby.blockReason}>{view.primaryAction.reason}</span>
            )}
            {view.primaryAction && (
              <button
                type="button"
                onClick={runPrimary}
                disabled={view.primaryAction.disabled}
                title={view.primaryAction.reason ?? ''}
                className={lobby.start}
              >
                {view.primaryAction.label}
              </button>
            )}
          </div>
        </div>
      </div>

      {deckPickerMounted && quickLobby && !isMomir && (
        <DeckPickerModal
          title="Your deck"
          subtitle={view.invitable
            ? 'Pick what you bring to this game. Your opponent chooses their own.'
            : 'Pick what you play against the AI.'}
          hidden={quickDeckSeat !== 'human'}
          onClose={() => setQuickDeckSeat(null)}
          selection={quickLobby?.players.find((p) => p.playerId === quickLobby.youPlayerId)?.deckSelected ? lastPicked : null}
          primary={view.primaryAction?.kind === 'READY'
            ? {
                // Against the AI, ready *is* start — the AI is always ready.
                label: view.invitable ? 'Ready up' : 'Start game',
                disabled: view.primaryAction.disabled || deckSubmitPending,
                reason: deckSubmitPending ? undefined : view.primaryAction.reason,
                onRun: runPrimary,
              }
            : undefined}
        >
          <QuickGameDeckPicker
            youSetCode={quickLobby.players.find((p) => p.playerId === quickLobby.youPlayerId)?.setCode ?? null}
            youSetCodes={quickLobby.players.find((p) => p.playerId === quickLobby.youPlayerId)?.setCodes}
            format={quickLobby.format ?? null}
            disabled={view.you?.tone === 'ready'}
            tab={deckTab}
            onTabChange={setDeckTab}
            onValidityChange={setDeckValid}
            initialSavedDeckName={intent?.deckName}
            onSavedDeckNameChange={setSavedDeckName}
            initialExampleName={intent?.exampleName}
            onExampleNameChange={setExampleName}
            onSubmitPendingChange={setDeckSubmitPending}
          />
        </DeckPickerModal>
      )}

      {quickDeckSeat === 'ai' && quickLobby?.vsAi && !isMomir && (() => {
        const ai = view.players.find((player) => player.isAi)
        if (!ai) return null
        return (
          <QuickAiDeckModal
            playerName={ai.name}
            aiDeck={quickLobby.aiDeck ?? null}
            format={quickLobby.format ?? null}
            commanderRules={view.axes.rules === 'COMMANDER'}
            disabled={view.you?.tone === 'ready'}
            source={aiSource}
            onSourceChange={setAiSource}
            onClose={() => setQuickDeckSeat(null)}
          />
        )
      })()}

      {tournamentDeckMounted && lobbyState && view.kind === 'TOURNAMENT' && view.isWaiting &&
        lobbyState.settings.format === 'PREMADE_DECKS' && (
          <DeckPickerModal
            title="Your deck"
            hidden={!tournamentDeckOpen}
            onClose={() => setTournamentDeckOpen(false)}
          >
            <PremadeDeckPickerPanel
              lobbyState={lobbyState}
              playerId={playerId}
              initialSavedDeckName={intent?.deckName}
              onSavedDeckNameChange={setSavedDeckName}
              initialExampleName={intent?.exampleName}
              onExampleNameChange={setExampleName}
              autoSubmit={Boolean(intent?.deckName) || Boolean(intent?.exampleName)}
            />
          </DeckPickerModal>
      )}

      {aiDeckSeat && (() => {
        // Read the seat back out of the view each render rather than closing over it: the roster is
        // re-broadcast on every change, and a seat removed while its modal is open must close it.
        const seat = view.players.find((p) => p.playerId === aiDeckSeat && p.isAi && p.aiDeck)
        if (!seat) return null
        return (
          <LobbyAiDeckModal
            playerId={seat.playerId}
            playerName={seat.name}
            aiDeck={seat.aiDeck ?? null}
            format={lobbyState?.settings.deckFormat ?? null}
            onClose={() => setAiDeckSeat(null)}
          />
        )
      })()}

      {savingSetup && (
        <SaveSetupDialog
          defaultName={view.title}
          onCancel={() => setSavingSetup(false)}
          onSave={(name) => {
            saveSetup({ name, recipe: capture().recipe })
            setSavingSetup(false)
          }}
        />
      )}

      {pendingRecreate && (
        <RecreateConfirm
          spec={pendingRecreate}
          view={view}
          onCancel={() => setPendingRecreate(null)}
          onConfirm={() => { commands.recreate(pendingRecreate); setPendingRecreate(null) }}
        />
      )}
    </div>
  )
}

/**
 * The player list as numbered seats, grouped by team when the host is choosing them.
 *
 * Two problems this fixes, both only visible at 3+ players — the case the lobby was least designed
 * for and the one a group setup lands you in.
 *
 * **Seat numbers.** The Free-for-All attack rule offers "left only" and "right only" and its own
 * caption says they follow the seating order, but the seating order was nowhere on the screen. The
 * order the server uses is the order of `view.players`, so numbering the rows is all it takes.
 *
 * **Team grouping.** At a 6-player Team vs. Team the host had six rows of identical shape with a
 * colour-coded chip on each, and the only statement of whether the teams were *legal* was a sentence
 * inside a settings caption. `view.teams` already computes `size` and `balanced`; this surfaces them
 * where the decision is actually made. Grouping only happens in MANUAL mode — under RANDOM the teams
 * are rolled at game start, so any grouping shown here would be fiction.
 */
function seatRows(view: UnifiedLobbyView): Array<{
  player: UnifiedLobbyView['players'][number]
  seat: number
  teamHeader: { label: string; have: number; need: number; balanced: boolean } | null
}> {
  const seatOf = new Map(view.players.map((p, i) => [p.playerId, i + 1]))
  const teams = view.teams
  if (teams.mode !== 'MANUAL') {
    return view.players.map((player) => ({ player, seat: seatOf.get(player.playerId)!, teamHeader: null }))
  }

  const out: ReturnType<typeof seatRows> = []
  for (const team of [0, 1]) {
    const members = view.players.filter((p) => (teams.byPlayerId[p.playerId] ?? 0) === team)
    members.forEach((player, i) => {
      out.push({
        player,
        seat: seatOf.get(player.playerId)!,
        teamHeader: i === 0
          ? {
              label: `Team ${team + 1}`,
              have: members.length,
              need: teams.size,
              balanced: members.length === teams.size,
            }
          : null,
      })
    })
  }
  return out
}

/** Ranked is a 1v1-bracket concept server-side, so it refines **Event** rather than the lobby. */
function RankedRow({ on, onChange }: { on: boolean; onChange: (ranked: boolean) => void }) {
  return (
    <div className={styles.settingsRow}>
      <SettingsLabel topicId="ranked">Ranked</SettingsLabel>
      <div className={styles.variantGroup}>
        <div className={styles.settingsButtons}>
          <button
            type="button"
            onClick={() => onChange(false)}
            className={`${styles.settingsButton} ${!on ? styles.settingsButtonActive : ''}`}
            title="Casual — no rating change"
          >
            Casual
          </button>
          <button
            type="button"
            onClick={() => onChange(true)}
            className={`${styles.settingsButton} ${on ? styles.settingsButtonActive : ''}`}
            title="Ranked — adjusts each player's ELO"
          >
            Ranked
          </button>
        </div>
        {on && (
          <div className={styles.variantCaption}>
            Ranked games adjust each player's ELO. All players must be signed in for it to count —
            otherwise it just plays unranked.
          </div>
        )}
      </div>
    </div>
  )
}

function VisibilityRow({
  isPublic,
  onChange,
}: {
  isPublic: boolean
  onChange: (isPublic: boolean) => void
}) {
  return (
    <div className={styles.settingsRow}>
      <span className={styles.settingsLabel}>Visibility</span>
      <div className={styles.settingsButtons}>
        <button
          type="button"
          onClick={() => onChange(false)}
          className={`${styles.settingsButton} ${!isPublic ? styles.settingsButtonActive : ''}`}
        >
          Private
        </button>
        <button
          type="button"
          onClick={() => onChange(true)}
          className={`${styles.settingsButton} ${isPublic ? styles.settingsButtonActive : ''}`}
        >
          Public
        </button>
      </div>
    </div>
  )
}

function statusClass(tone: 'ready' | 'joined' | 'disconnected'): string {
  switch (tone) {
    case 'ready': return styles.playerStatusReady ?? ''
    case 'disconnected': return styles.playerStatusDisconnected ?? ''
    case 'joined': return styles.playerStatusJoined ?? ''
  }
}

/** Your own seat's deck chip: the deck's own name where the picker knows it, and an explicit Change. */
function ownDeckLabel(status: string, ready: boolean, localName: string | null): string {
  if (!localName) return ready ? status : `${status} · Change`
  return ready ? `✓ Ready · ${localName}` : `${localName} · Change`
}

function capitalize(value: string): string {
  return value.charAt(0).toUpperCase() + value.slice(1)
}

function MomirCrest() {
  return (
    <div
      aria-hidden
      style={{
        flex: 'none',
        width: 52,
        height: 52,
        backgroundColor: 'var(--accent-teal, #6fd3c0)',
        WebkitMaskImage: `url(${momirVigUrl})`,
        maskImage: `url(${momirVigUrl})`,
        WebkitMaskSize: 'contain',
        maskSize: 'contain',
        WebkitMaskRepeat: 'no-repeat',
        maskRepeat: 'no-repeat',
        WebkitMaskPosition: 'center',
        maskPosition: 'center',
        opacity: 0.9,
      }}
    />
  )
}

/**
 * The stop sign in front of a cross-kind axis switch (plan § 4b v1).
 *
 * A recreate is cheap when you are alone in a lobby you just opened — the common case — and
 * expensive once anyone has joined, so the dialog counts them rather than warning in the abstract.
 */
function RecreateConfirm({
  spec,
  view,
  onCancel,
  onConfirm,
}: {
  spec: RecreateSpec
  view: UnifiedLobbyView
  onCancel: () => void
  onConfirm: () => void
}) {
  const others = view.players.filter((p) => !p.isYou && !p.isAi).length
  const ais = view.players.filter((p) => p.isAi).length

  return (
    <div className={styles.confirmBackdrop} role="dialog" aria-modal="true" onClick={onCancel}>
      <div className={styles.confirmPanel} onClick={(e) => e.stopPropagation()}>
        <div className={styles.confirmTitle}>Start a new lobby?</div>
        <p className={styles.confirmBody}>
          “{recreateTargetLabel(spec)}” runs on a different lobby to this one, so switching opens a
          fresh one.
        </p>
        <ul className={styles.confirmCosts}>
          <li>Your invite code changes — you'll need to re-share it.</li>
          {others > 0 && (
            <li>
              {others === 1 ? '1 player who has' : `${others} players who have`} joined will be
              dropped.
            </li>
          )}
          {ais > 0 && <li>{ais === 1 ? 'The AI seat' : `${ais} AI seats`} will need adding again.</li>}
          {view.kind === 'TOURNAMENT' && <li>Set selection and any submitted decks are reset.</li>}
        </ul>
        <div className={styles.confirmActions}>
          <button type="button" onClick={onCancel} className={styles.leaveButton}>Cancel</button>
          <button type="button" onClick={onConfirm} className={styles.startButton} data-testid="confirm-recreate">
            Switch
          </button>
        </div>
      </div>
    </div>
  )
}

/**
 * The quick lobby's deck picker, with the submission plumbing it has always needed.
 *
 * Submissions are throttled *and* deduped: the picker fires several times per keystroke, and it
 * re-emits its current deck on every re-render — including the ones a server broadcast causes — so
 * without the dedupe an unchanged deck would be resent, which server-side clears your `ready` flag
 * and triggers another broadcast. That was a real spam loop.
 */
function QuickGameDeckPicker({
  youSetCode,
  youSetCodes,
  format,
  disabled,
  tab,
  onTabChange,
  onValidityChange,
  initialSavedDeckName,
  onSavedDeckNameChange,
  initialExampleName,
  onExampleNameChange,
  onSubmitPendingChange,
}: {
  youSetCode: string | null
  youSetCodes: readonly string[] | undefined
  format: string | null
  disabled: boolean
  tab: DeckPickerTab | undefined
  onTabChange: (tab: DeckPickerTab) => void
  onValidityChange: (valid: boolean) => void
  /** A saved setup's deck, preselected once the library hydrates. */
  initialSavedDeckName: string | undefined
  onSavedDeckNameChange: (name: string | null) => void
  initialExampleName: string | undefined
  onExampleNameChange: (name: string | null) => void
  /** Whether a debounced submission is waiting to be sent. */
  onSubmitPendingChange: (pending: boolean) => void
}) {
  const submitDeck = useGameStore((s) => s.submitQuickGameLobbyDeck)
  const setSetCode = useGameStore((s) => s.setQuickGameLobbySetCode)
  const availableSets = useGameStore((s) => s.availableSets)

  const pendingDeckRef = useRef<Record<string, number> | null>(null)
  const pendingCommanderRef = useRef<string | null>(null)
  const pendingSideboardRef = useRef<Record<string, number>>({})
  const lastSubmittedKeyRef = useRef<string | null>(null)
  const debounceRef = useRef<number | null>(null)

  const handleDeckChange = useCallback(
    (
      deckList: Record<string, number>,
      commander?: string | null,
      sideboard?: Record<string, number>,
    ) => {
      // The commander and the sideboard ride in the dedupe key, so swapping either on otherwise-
      // identical deck contents still resubmits — editing only the sideboard is a real change.
      const board = sideboard ?? {}
      const key = `${serializeDeck(deckList)}|${commander ?? ''}|${serializeDeck(board)}`
      if (key === lastSubmittedKeyRef.current) return
      pendingDeckRef.current = deckList
      pendingCommanderRef.current = commander ?? null
      pendingSideboardRef.current = board
      if (debounceRef.current !== null) window.clearTimeout(debounceRef.current)
      onSubmitPendingChange(true)
      debounceRef.current = window.setTimeout(() => {
        onSubmitPendingChange(false)
        const pending = pendingDeckRef.current
        if (!pending) return
        const pendingCmdr = pendingCommanderRef.current
        const pendingSide = pendingSideboardRef.current
        const pendingKey = `${serializeDeck(pending)}|${pendingCmdr ?? ''}|${serializeDeck(pendingSide)}`
        if (pendingKey === lastSubmittedKeyRef.current) return
        lastSubmittedKeyRef.current = pendingKey
        submitDeck(pending, pendingCmdr, pendingSide)
      }, 250)
    },
    [submitDeck, onSubmitPendingChange],
  )

  // Flush any pending deck on unmount so the user's last edit isn't dropped.
  useEffect(() => {
    return () => {
      if (debounceRef.current !== null) window.clearTimeout(debounceRef.current)
      const pending = pendingDeckRef.current
      const pendingCmdr = pendingCommanderRef.current
      const pendingSide = pendingSideboardRef.current
      const pendingKey = pending
        ? `${serializeDeck(pending)}|${pendingCmdr ?? ''}|${serializeDeck(pendingSide)}`
        : null
      if (pending && pendingKey !== lastSubmittedKeyRef.current) {
        // Recorded like any other submission, so a remount (StrictMode's, or the picker's) that
        // re-emits the same deck doesn't send it again — once the game has started, that second
        // send lands on a lobby that no longer exists.
        lastSubmittedKeyRef.current = pendingKey
        submitDeck(pending, pendingCmdr, pendingSide)
      }
    }
  }, [submitDeck])

  return (
    <DeckPicker
      onDeckChange={handleDeckChange}
      onValidityChange={onValidityChange}
      onSetCodesChange={setSetCode}
      initialSetCode={youSetCode}
      {...(youSetCodes ? { initialSetCodes: youSetCodes } : {})}
      availableSets={availableSets}
      disabled={disabled}
      format={format}
      tab={tab}
      onTabChange={onTabChange}
      initialSavedDeckName={initialSavedDeckName}
      onSavedDeckNameChange={onSavedDeckNameChange}
      initialExampleName={initialExampleName}
      onExampleNameChange={onExampleNameChange}
    />
  )
}

/**
 * Name a setup before saving it.
 *
 * Deliberately a real dialog rather than a `window.prompt`: this is the moment someone commits to
 * replaying a lobby they may have spent a few minutes configuring, and the default name is worth
 * showing selected and editable rather than as a browser chrome string.
 */
function SaveSetupDialog({
  defaultName,
  onCancel,
  onSave,
}: {
  defaultName: string
  onCancel: () => void
  onSave: (name: string) => void
}) {
  const [name, setName] = useState(defaultName)
  const trimmed = name.trim()

  return (
    <div className={styles.confirmBackdrop} role="dialog" aria-modal="true" onClick={onCancel}>
      <div className={styles.confirmPanel} onClick={(e) => e.stopPropagation()}>
        <div className={styles.confirmTitle}>Save this setup</div>
        <p className={styles.confirmBody}>
          It comes back on the home screen as one click — sets, packs, timer, ban list, cube and the
          deck you picked.
        </p>
        <input
          className={styles.settingsSelect}
          style={{ width: '100%' }}
          value={name}
          autoFocus
          onChange={(e) => setName(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter' && trimmed) onSave(trimmed) }}
          aria-label="Setup name"
        />
        <div className={styles.confirmActions}>
          <button type="button" onClick={onCancel} className={styles.leaveButton}>Cancel</button>
          <button
            type="button"
            onClick={() => onSave(trimmed)}
            disabled={!trimmed}
            className={styles.startButton}
            data-testid="confirm-save-setup"
          >
            Save
          </button>
        </div>
      </div>
    </div>
  )
}

/**
 * Embedded deck picker for the Premade Decks tournament format. Each player picks and submits
 * their deck right in the lobby; the host can only start once everybody has.
 */
function PremadeDeckPickerPanel({
  lobbyState,
  playerId,
  initialSavedDeckName,
  onSavedDeckNameChange,
  initialExampleName,
  onExampleNameChange,
  autoSubmit,
}: {
  lobbyState: LobbyState
  playerId: string | null
  initialSavedDeckName: string | undefined
  onSavedDeckNameChange: (name: string | null) => void
  initialExampleName: string | undefined
  onExampleNameChange: (name: string | null) => void
  /** Submit the preselected deck by itself, once, as soon as it resolves and validates. */
  autoSubmit: boolean
}) {
  const submitLobbyDeck = useGameStore((s) => s.submitLobbyDeck)
  const unsubmitLobbyDeck = useGameStore((s) => s.unsubmitLobbyDeck)

  const me = lobbyState.players.find((p) => p.playerId === playerId)
  const hasSubmitted = !!me?.deckSubmitted

  const [pendingDeck, setPendingDeck] = useState<Record<string, number>>({})
  const [pendingCommander, setPendingCommander] = useState<string | null>(null)
  const [pendingSideboard, setPendingSideboard] = useState<Record<string, number>>({})
  const [isValid, setIsValid] = useState(false)
  const autoSubmitted = useRef(false)

  const handleDeckChange = useCallback(
    (deck: Record<string, number>, commander?: string | null, sideboard?: Record<string, number>) => {
      setPendingDeck(deck)
      setPendingCommander(commander ?? null)
      setPendingSideboard(sideboard ?? {})
    },
    [],
  )

  const deckFormat = lobbyState.settings.deckFormat
  // Whether this submission needs a commander follows the lobby's Rules axis, not its deck legality:
  // the server's deck-submit path keys on `usesCommanderRules`, so deriving it from the legality here
  // would ask for a commander the server doesn't want (Commander-legal decks under Standard rules) or
  // — worse — not ask for one it requires.
  const isCommanderShape = rulesFromLobbySettings(lobbyState.settings) === 'COMMANDER'
  const totalCards = Object.values(pendingDeck).reduce((a, b) => a + b, 0)
  const needsCommander = isCommanderShape && !pendingCommander
  const canSubmit = isValid && totalCards >= 40 && !needsCommander

  useEffect(() => {
    if (!autoSubmit || autoSubmitted.current || hasSubmitted || !canSubmit) return
    autoSubmitted.current = true
    submitLobbyDeck(pendingDeck, isCommanderShape ? pendingCommander : null, pendingSideboard)
  }, [autoSubmit, hasSubmitted, canSubmit, submitLobbyDeck, pendingDeck, isCommanderShape, pendingCommander, pendingSideboard])

  if (hasSubmitted) {
    return (
      <div className={styles.deckSubmittedCard} role="status">
        <div className={styles.deckSubmittedIcon} aria-hidden>✓</div>
        <div className={styles.deckSubmittedBody}>
          <span className={styles.deckSubmittedTitle}>Deck submitted</span>
          <span className={styles.deckSubmittedSubtitle}>Waiting for the host to start.</span>
        </div>
        <button onClick={unsubmitLobbyDeck} className={styles.deckSubmittedEditButton}>Edit deck</button>
      </div>
    )
  }

  return (
    <div className={styles.settingsPanel}>
      <div className={styles.settingsRow} style={{ alignItems: 'flex-start', flexDirection: 'column', gap: 12 }}>
        <span className={styles.settingsLabel}>Your Deck</span>
        {deckFormat && (
          <span className={styles.formatRestrictionNotice}>
            <span className={styles.formatRestrictionBadge}>{labelForFormat(deckFormat)}</span>
            <span>Only cards legal in this format will be accepted.</span>
          </span>
        )}
        <DeckPicker
          tabs={['saved', 'examples', 'paste']}
          onDeckChange={handleDeckChange}
          onValidityChange={setIsValid}
          format={deckFormat ?? null}
          initialSavedDeckName={initialSavedDeckName}
          onSavedDeckNameChange={onSavedDeckNameChange}
          initialExampleName={initialExampleName}
          onExampleNameChange={onExampleNameChange}
        />
        <button
          onClick={() => submitLobbyDeck(pendingDeck, isCommanderShape ? pendingCommander : null, pendingSideboard)}
          disabled={!canSubmit}
          title={
            canSubmit ? undefined
              : needsCommander
                ? 'Pick a deck with a designated commander to play this format'
                : 'Pick a valid deck of at least 40 cards'
          }
          className={styles.startButton}
        >
          Submit Deck
        </button>
      </div>
    </div>
  )
}

/**
 * Stable key for a deck list, used to dedupe submissions. Sorted by name so two equal decks always
 * serialize the same regardless of insertion order in the picker.
 */
function serializeDeck(deck: Record<string, number>): string {
  return Object.entries(deck)
    .filter(([, n]) => n > 0)
    .sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0))
    .map(([name, n]) => `${name}=${n}`)
    .join('|')
}
