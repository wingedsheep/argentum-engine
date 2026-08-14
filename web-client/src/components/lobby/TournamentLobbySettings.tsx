/**
 * The knobs that only a tournament-backed lobby has.
 *
 * `LobbyScreen` owns everything both lobby kinds share — the axes, visibility, ranked, the player
 * list, the actions row. What's left is genuinely specific to the tournament implementation: a
 * card pool has sets, boosters and a ban list; a draft has timers; a team table has team setup.
 * Keeping them here rather than behind `view.kind === 'TOURNAMENT'` checks scattered through the
 * screen is what keeps the screen readable as one thing.
 *
 * Everything in this file is a faithful move out of the old `LobbyOverlay`, except that the rows
 * are ordered by what they belong to rather than by the order they were added.
 */
import { useEffect, useState } from 'react'
import { useGameStore } from '@/store/gameStore'
import type { LobbyState } from '@/store/slices/types'
import { teamColor } from '@/styles/seatColors'
import { BanListEditor } from '../ui/BanListEditor'
import { CubePanel } from './CubePanel'
import { SetSelector, nextRandomSetCode } from '../ui/SetSelector'
import { SettingsLabel } from '../ui/SettingsLabel'
import { COMMANDER_PRESETS, effectiveCommanderPreset } from './axes'
import type { UnifiedLobbyView } from './lobbyViewModel'
import type { GroupId } from './settingsGroups'
import styles from '../ui/GameUI.module.css'

/**
 * The tournament-only rows belonging to one settings group.
 *
 * Everything below is a faithful move from the single flat list this file used to render; what
 * changed is that each row now declares which axis it refines, and `LobbyScreen` asks for one group
 * at a time. The mapping is not a new taxonomy — it is what the conditions in this file already
 * said. The Commander deckbuild knobs were already gated on `axes.rules === 'COMMANDER'`, the
 * matchup count on `axes.event === 'ROUND_ROBIN'`, and the attack rule on a Free-for-All *table*.
 *
 * | group | rows |
 * |---|---|
 * | Cards | cube · card pool · sets · booster mix · ban list · packs · pick timer · cards per pick |
 * | Rules | preset · min deck size · singleton |
 * | Table | teams · attack |
 * | Event | games per matchup |
 * | This lobby | AI assistance |
 *
 * There is no Seats row to place: the lobby already holds as many players as its shape allows and
 * people join until it is full.
 */
export function TournamentLobbySettings({
  group,
  view,
  lobbyState,
}: {
  group: GroupId
  view: UnifiedLobbyView
  lobbyState: LobbyState
}) {
  const updateLobbySettings = useGameStore((s) => s.updateLobbySettings)

  const s = lobbyState.settings
  const format = s.format
  const isSealed = format === 'SEALED'
  const isDraft = format === 'DRAFT'
  const isWinston = format === 'WINSTON_DRAFT'
  const isGridDraft = format === 'GRID_DRAFT'
  const isCommanderDraft = format === 'COMMANDER_DRAFT'
  const isPremade = format === 'PREMADE_DECKS'
  const isAnyDraft = isDraft || isWinston || isGridDraft || isCommanderDraft
  // The Commander deckbuild knobs (life preset, minimum deck size, singleton) tune a *pool-built*
  // 60-card Commander deck, which is what the server validates against — so they follow the Rules
  // axis rather than the pack format (a Commander game over ordinary draft packs wants them too),
  // minus the brought-deck case, where paper Commander's own construction rules apply instead.
  const isPoolBuiltCommander = view.axes.rules === 'COMMANDER' && !isPremade
  // A pod overrides the host's 1v1 life tuning with paper Commander's 40 (see
  // TournamentLobby.effectiveCommanderPreset), so the picker reports rather than offers.
  const effectivePreset = effectiveCommanderPreset(s.commanderPreset, s.gameMode)
  const podPreset = effectivePreset === 'POD'
  const isFfa = s.gameMode === 'FREE_FOR_ALL'
  const isCube = Boolean(s.cubeName)
  const [cardSource, setCardSource] = useState<'SETS' | 'CUBE'>(isCube ? 'CUBE' : 'SETS')
  useEffect(() => {
    setCardSource(isCube ? 'CUBE' : 'SETS')
  }, [isCube])

  const chooseCardSource = (source: 'SETS' | 'CUBE') => {
    setCardSource(source)
    if (source === 'SETS' && isCube) updateLobbySettings({ cubeCards: [] })
  }
  // Pool Play hands out the whole cube instead of dealing packs, so the pack-count controls are
  // meaningless while it's on. Matches TournamentLobby.isCubePoolPlay.
  const isPoolPlay = isCube && isSealed && Boolean(s.cubePoolPlay)

  const allSets = s.availableSets

  const toggleSet = (code: string) => {
    const removing = s.setCodes.includes(code)
    const next = removing ? s.setCodes.filter((c) => c !== code) : [...s.setCodes, code]
    const includedSetProducts = { ...s.includedSetProducts }
    if (removing) delete includedSetProducts[code]
    updateLobbySettings({ setCodes: next, includedSetProducts })
  }

  const toggleSetProduct = (setCode: string, productId: string) => {
    const current = s.includedSetProducts[setCode] ?? []
    const next = current.includes(productId)
      ? current.filter((id) => id !== productId)
      : [...current, productId]
    updateLobbySettings({
      includedSetProducts: { ...s.includedSetProducts, [setCode]: next },
    })
  }

  // "Random Set" in the picker: a deferred slot the server rolls to a complete, non-extension set
  // at game start. Suffixed codes keep multiple random slots distinct.
  const addRandomSet = () => {
    updateLobbySettings({ setCodes: [...s.setCodes, nextRandomSetCode(s.setCodes)] })
  }

  const perSetCounts = s.setCodes.length > 1 && !s.chaosBoosters
  // Booster Draft and Commander Draft count *packs*, capped at 6. Sealed and Winston count
  // boosters, capped at 16 — Winston hands out a shared pile, so it sits with sealed here even
  // though it is a draft everywhere else.
  const countsPacks = isDraft || isCommanderDraft
  const boosterCap = countsPacks ? 6 : 16

  return (
    <>
      {/* Team setup (2HG — CR 810; Team vs. Team — CR 808). */}
      {group === 'TABLE' && view.teams.mode !== 'NONE' && (
        <div className={styles.settingsRow}>
          <SettingsLabel topicId="table-two-headed-giant">Teams</SettingsLabel>
          <div className={styles.variantGroup}>
            <div className={styles.settingsButtons}>
              <button
                onClick={() => updateLobbySettings({ randomTeams: true })}
                className={`${styles.settingsButton} ${view.teams.mode === 'RANDOM' ? styles.settingsButtonActive : ''}`}
                title="Shuffle the players into two even teams when the game starts (re-rolled each game)"
              >
                Random
              </button>
              <button
                onClick={() => updateLobbySettings({ randomTeams: false })}
                className={`${styles.settingsButton} ${view.teams.mode === 'MANUAL' ? styles.settingsButtonActive : ''}`}
                title="Set the teams by hand — click each player's team chip below"
              >
                Choose teams
              </button>
            </div>
            <div className={styles.variantCaption}>
              {view.teams.mode === 'RANDOM'
                ? 'Teams are randomised at game start, fresh every game.'
                : view.teams.balanced
                  ? 'Click a player’s team chip below to move them between teams.'
                  : `Click each player’s team chip below — each team needs exactly ${view.teams.size} player${view.teams.size === 1 ? '' : 's'}.`}
            </div>
          </div>
        </div>
      )}

      {/* Free-for-All attack rule (CR 802/803) — only relevant once 3+ players share one table. */}
      {group === 'TABLE' && isFfa && (
        <div className={styles.settingsRow}>
          <SettingsLabel topicId="table-free-for-all">Attack</SettingsLabel>
          <div className={styles.variantGroup}>
            <div className={styles.settingsButtons}>
              {([
                ['MULTIPLE', 'Any opponent', 'Each creature may attack any opponent (CR 802)'],
                ['LEFT', 'Left only', 'Each creature may attack only the player to your left (CR 803)'],
                ['RIGHT', 'Right only', 'Each creature may attack only the player to your right (CR 803)'],
              ] as const).map(([mode, label, title]) => (
                <button
                  key={mode}
                  onClick={() => updateLobbySettings({ attackMode: mode })}
                  className={`${styles.settingsButton} ${(s.attackMode ?? 'MULTIPLE') === mode ? styles.settingsButtonActive : ''}`}
                  title={title}
                >
                  {label}
                </button>
              ))}
            </div>
            <div className={styles.variantCaption}>
              Who each creature may attack. "Left"/"right" follow the seating order.
            </div>
          </div>
        </div>
      )}

      {/* Sets and cubes are alternative pack sources. Keep them in one row so the relationship is
          explicit; a lobby with no cube starts on Sets. */}
      {group === 'CARDS' && !isPremade && (
        <div className={styles.settingsRow} style={{ alignItems: 'flex-start' }}>
          <span style={{ paddingTop: 7 }}>
            <SettingsLabel topicId="cards-cube">Card source</SettingsLabel>
          </span>
          <div className={styles.cardSourceControl}>
            <div className={styles.settingsButtons}>
              <button
                type="button"
                aria-pressed={cardSource === 'SETS'}
                onClick={() => chooseCardSource('SETS')}
                className={`${styles.settingsButton} ${cardSource === 'SETS' ? styles.settingsButtonActive : ''}`}
              >
                Sets
              </button>
              <button
                type="button"
                aria-pressed={cardSource === 'CUBE'}
                onClick={() => chooseCardSource('CUBE')}
                className={`${styles.settingsButton} ${cardSource === 'CUBE' ? styles.settingsButtonActive : ''}`}
              >
                Cube
              </button>
            </div>

            {cardSource === 'CUBE' ? (
              <CubePanel
                settings={s}
                playerCount={view.players.length}
                updateLobbySettings={updateLobbySettings}
              />
            ) : (
              <SetSelector
                sets={allSets}
                selectedCodes={s.setCodes}
                onToggleSet={toggleSet}
                onSelectRandom={addRandomSet}
                selectedProducts={s.includedSetProducts}
                onToggleProduct={toggleSetProduct}
                accent={isAnyDraft ? 'draft' : 'sealed'}
              />
            )}
          </div>
        </div>
      )}

      {/* Pool Play (cube Sealed only): no draft at all — everyone builds from the whole cube. */}
      {group === 'CARDS' && isCube && isSealed && (
        <div className={styles.settingsRow}>
          <SettingsLabel topicId="cube-pool-play">Card pool</SettingsLabel>
          <div className={styles.variantGroup}>
            <div className={styles.settingsButtons}>
              <button
                onClick={() => updateLobbySettings({ cubePoolPlay: false })}
                className={`${styles.settingsButton} ${!s.cubePoolPlay ? styles.settingsButtonActive : ''}`}
                title="Deal each player their own sealed pool from the cube"
              >
                Sealed packs
              </button>
              <button
                onClick={() => updateLobbySettings({ cubePoolPlay: true })}
                className={`${styles.settingsButton} ${s.cubePoolPlay ? styles.settingsButtonActive : ''}`}
                title="Pool Play: every player builds from the entire cube, up to 4 copies of any card"
              >
                Pool Play
              </button>
            </div>
            <div className={styles.variantCaption}>
              {s.cubePoolPlay
                ? 'Every player builds from the whole cube at once, up to 4 copies of any card. Nothing is dealt, so the cube can be any size and no two players compete for a card.'
                : 'Each player opens their own packs dealt from the cube. No card appears twice across the table.'}
            </div>
          </div>
        </div>
      )}

      {group === 'CARDS' && !isPremade && cardSource === 'SETS' && (
        <>
          {/* Chaos boosters — only meaningful with >1 set and a booster-based format. */}
          {!isGridDraft && s.setCodes.length > 1 && (
            <div className={styles.settingsRow}>
              <span className={styles.settingsLabel}>Booster mix</span>
              <div className={styles.variantGroup}>
                <div className={styles.settingsButtons}>
                  <button
                    onClick={() => updateLobbySettings({ chaosBoosters: false })}
                    className={`${styles.settingsButton} ${!s.chaosBoosters ? styles.settingsButtonActive : ''}`}
                  >
                    Per set
                  </button>
                  <button
                    onClick={() => updateLobbySettings({ chaosBoosters: true })}
                    className={`${styles.settingsButton} ${s.chaosBoosters ? styles.settingsButtonActive : ''}`}
                  >
                    Chaos
                  </button>
                </div>
                <div className={styles.variantCaption}>
                  {s.chaosBoosters
                    ? 'Each booster mixes cards from all selected sets.'
                    : 'Each booster contains cards from a single set.'}
                </div>
              </div>
            </div>
          )}

          <BanListEditor
            setCodes={s.setCodes}
            bannedCardNames={s.bannedCardNames ?? []}
            onChange={(names) => updateLobbySettings({ bannedCardNames: names })}
          />
        </>
      )}

      {/* Booster/pack counts. Grid Draft uses fixed counts, Premade generates none, and Pool Play
          deals no packs at all — so it gets no pack count rather than an inert one. */}
      {group === 'CARDS' && !isPremade && !isGridDraft && !isPoolPlay && (
        perSetCounts ? (
          <div className={styles.settingsRow} style={{ flexDirection: 'column', alignItems: 'stretch', gap: 8 }}>
            <span className={styles.settingsLabel}>{boosterCountLabel(isWinston, countsPacks)}</span>
            <div className={styles.boosterDistribution}>
              {s.setCodes.map((code) => {
                const setName = s.setNames[s.setCodes.indexOf(code)] ?? code
                const dist = s.boosterDistribution
                const count = dist[code] ?? 0
                const total = Object.values(dist).reduce((a, b) => a + b, 0)
                return (
                  <div key={code} className={styles.boosterDistributionRow}>
                    <span className={styles.boosterDistributionSetName}>{setName}</span>
                    <div className={styles.boosterDistributionControls}>
                      <button
                        className={styles.boosterDistributionBtn}
                        disabled={count <= 0}
                        onClick={() => updateLobbySettings({
                          boosterDistribution: { ...dist, [code]: count - 1 },
                          boosterCount: total - 1,
                        })}
                      >-</button>
                      <span className={styles.boosterDistributionCount}>{count}</span>
                      <button
                        className={styles.boosterDistributionBtn}
                        disabled={total >= boosterCap}
                        onClick={() => updateLobbySettings({
                          boosterDistribution: { ...dist, [code]: count + 1 },
                          boosterCount: total + 1,
                        })}
                      >+</button>
                    </div>
                  </div>
                )
              })}
              <div className={styles.boosterDistributionTotal}>
                <span style={{ flex: 1 }}>Total</span>
                <span className={styles.boosterDistributionTotalCount}>
                  {Object.values(s.boosterDistribution).reduce((a, b) => a + b, 0)}
                  {countsPacks ? ' packs' : ' boosters'}
                </span>
              </div>
            </div>
          </div>
        ) : (
          <div className={styles.settingsRow}>
            <span className={styles.settingsLabel}>{boosterCountLabel(isWinston, countsPacks)}</span>
            <select
              value={s.boosterCount}
              onChange={(e) => updateLobbySettings({ boosterCount: Number(e.target.value) })}
              className={styles.settingsSelect}
            >
              {Array.from({ length: boosterCap }, (_, i) => i + 1).map((n) => (
                <option key={n} value={n}>{n}</option>
              ))}
            </select>
          </div>
        )
      )}

      {/* Draft timing and pick size. */}
      {group === 'CARDS' && isAnyDraft && (
        <div className={styles.settingsRow}>
          <span className={styles.settingsLabel}>{isWinston ? 'Turn timer (seconds)' : 'Pick timer (seconds)'}</span>
          <select
            value={s.pickTimeSeconds}
            onChange={(e) => updateLobbySettings({ pickTimeSeconds: Number(e.target.value) })}
            className={styles.settingsSelect}
          >
            {[30, 45, 60, 90, 120].map((n) => <option key={n} value={n}>{n}s</option>)}
          </select>
        </div>
      )}
      {group === 'CARDS' && (isDraft || isCommanderDraft) && (
        <div className={styles.settingsRow}>
          <span className={styles.settingsLabel}>Cards per pick</span>
          <div className={styles.settingsButtons}>
            {[1, 2].map((n) => (
              <button
                key={n}
                onClick={() => updateLobbySettings({ picksPerRound: n })}
                className={`${styles.settingsButton} ${s.picksPerRound === n ? `${styles.settingsButtonActive} ${styles.settingsButtonDraft}` : ''}`}
              >
                {n}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Commander preset + deck-shape knobs — a Commander game built from a generated pool. */}
      {group === 'RULES' && isPoolBuiltCommander && (
        <>
          <div className={styles.settingsRow}>
            <span className={styles.settingsLabel}>Preset</span>
            <div className={styles.variantGroup}>
              <div className={styles.settingsButtons}>
                {(['BRAWL', 'COMMANDER'] as const).map((preset) => (
                  <button
                    key={preset}
                    onClick={() => updateLobbySettings({ commanderPreset: preset })}
                    disabled={podPreset}
                    className={`${styles.settingsButton} ${effectivePreset === preset ? styles.settingsButtonActive : ''}`}
                    title={COMMANDER_PRESETS[preset].hint}
                  >
                    {COMMANDER_PRESETS[preset].label}
                  </button>
                ))}
                {/* The pod preset is the table's, not the host's — shown as the live selection, not a choice. */}
                {podPreset && (
                  <button
                    disabled
                    className={`${styles.settingsButton} ${styles.settingsButtonActive}`}
                    title={COMMANDER_PRESETS.POD.hint}
                  >
                    {COMMANDER_PRESETS.POD.label}
                  </button>
                )}
              </div>
              {podPreset && (
                <div className={styles.variantCaption}>
                  A pod plays paper multiplayer Commander: 40 life each. The 25/30 presets only pace a
                  1v1 bracket, so this table ignores them.
                </div>
              )}
            </div>
          </div>
          <div className={styles.settingsRow}>
            <span className={styles.settingsLabel}>Min deck size</span>
            <select
              value={s.deckSizeMin}
              onChange={(e) => updateLobbySettings({ deckSizeMin: Number(e.target.value) })}
              className={styles.settingsSelect}
            >
              {[40, 50, 60, 75, 100].map((n) => <option key={n} value={n}>{n}</option>)}
            </select>
          </div>
          <div className={styles.settingsRow}>
            <span className={styles.settingsLabel}>Singleton</span>
            <div className={styles.settingsButtons}>
              <button
                onClick={() => updateLobbySettings({ allowDuplicates: true })}
                className={`${styles.settingsButton} ${s.allowDuplicates ? styles.settingsButtonActive : ''}`}
                title="Allow multiple copies of the same card (drafted Commander default)"
              >
                Duplicates OK
              </button>
              <button
                onClick={() => updateLobbySettings({ allowDuplicates: false })}
                className={`${styles.settingsButton} ${!s.allowDuplicates ? styles.settingsButtonActive : ''}`}
                title="Paper-Commander singleton — max 1 of any non-basic card"
              >
                Singleton
              </button>
            </div>
          </div>
        </>
      )}

      {/* No Seats row: the lobby holds as many as its shape allows and people join until it is full.
          The cap was never a quorum — `startBlockReason` counts the players actually present, so the
          host starts when everyone has arrived — which made it a number to predict and then correct.
          It follows the shape server-side (`LobbyHandler.seatCapFor`), and the header's `n / max`
          says where it currently stands. */}

      {/* Only a bracket has matchups. */}
      {group === 'EVENT' && view.axes.event === 'ROUND_ROBIN' && (
        <div className={styles.settingsRow}>
          <span className={styles.settingsLabel}>Games per matchup</span>
          <select
            value={s.gamesPerMatch ?? 1}
            onChange={(e) => updateLobbySettings({ gamesPerMatch: Number(e.target.value) })}
            className={styles.settingsSelect}
          >
            {[1, 2, 3, 4, 5].map((n) => <option key={n} value={n}>{n}</option>)}
          </select>
        </div>
      )}

      {group === 'LOBBY' && (
      <div className={styles.settingsRow}>
        <span className={styles.settingsLabel} title="Lets players use Suggest Pick and Auto-build during this event">
          AI assistance
        </span>
        <div className={styles.settingsButtons}>
          <button
            onClick={() => updateLobbySettings({ aiAssistEnabled: false })}
            className={`${styles.settingsButton} ${!s.aiAssistEnabled ? styles.settingsButtonActive : ''}`}
          >
            Off
          </button>
          <button
            onClick={() => updateLobbySettings({ aiAssistEnabled: true })}
            className={`${styles.settingsButton} ${s.aiAssistEnabled ? styles.settingsButtonActive : ''}`}
          >
            On
          </button>
        </div>
      </div>
      )}

    </>
  )
}

function boosterCountLabel(isWinston: boolean, countsPacks: boolean): string {
  if (isWinston) return 'Boosters (total)'
  return countsPacks ? 'Packs per player' : 'Boosters per player'
}

/** Team chip for the player list — colour-coded, and clickable for the host in manual mode. */
export function TeamChip({
  team,
  editable,
  onClick,
}: {
  /** null = teams are randomised at game start. */
  team: number | null
  editable: boolean
  onClick: () => void
}) {
  const c = team === null ? null : teamColor(team)
  const style = {
    fontSize: 10,
    fontWeight: 800,
    letterSpacing: '0.05em',
    textTransform: 'uppercase' as const,
    color: c?.bright ?? 'rgba(226, 232, 240, 0.7)',
    border: `1px solid ${c?.base ?? 'rgba(148, 163, 184, 0.45)'}`,
    background: c?.soft ?? 'rgba(148, 163, 184, 0.12)',
    borderRadius: 4,
    padding: '1px 6px',
  }
  const label = team === null ? 'Random' : `Team ${team + 1}`
  return editable && team !== null ? (
    <button onClick={onClick} style={{ ...style, cursor: 'pointer' }} title="Click to move this player to the other team">
      {label}
    </button>
  ) : (
    <span style={style}>{label}</span>
  )
}
