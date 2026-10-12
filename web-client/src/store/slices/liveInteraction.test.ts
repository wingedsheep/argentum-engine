import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ClientGameState, LegalActionInfo, PendingDecision, StateDeltaUpdateMessage, StateUpdateMessage } from '@/types'
import type { CombatState, GameStore } from './types'
import type { GameWebSocket } from '@/network/websocket'

vi.stubGlobal('localStorage', { getItem: () => null, setItem: () => {}, removeItem: () => {} })
vi.stubGlobal('window', { addEventListener: () => {}, removeEventListener: () => {} })

const { useGameStore } = await import('../gameStore')
const { createGameplayHandlers } = await import('./handlers/gameplayHandlers')
const { setWebSocket } = await import('./shared')
const { entityId, Phase, Step } = await import('@/types')

const ME = entityId('me')
const SPELL = entityId('spell')
const MANA = entityId('mana')
const TARGET = entityId('target')
const send = vi.fn()
const handlers = createGameplayHandlers(useGameStore.setState, useGameStore.getState)
const position: ClientGameState = {
  viewingPlayerId: ME,
  cards: {},
  zones: [],
  players: [],
  currentPhase: Phase.PRECOMBAT_MAIN,
  currentStep: Step.PRECOMBAT_MAIN,
  activePlayerId: ME,
  priorityPlayerId: ME,
  turnNumber: 1,
  isGameOver: false,
  winnerId: null,
  combat: null,
}
const offered: LegalActionInfo = {
  actionType: 'CastSpell',
  description: 'Cast Test spell',
  action: { type: 'CastSpell', playerId: ME, cardId: SPELL },
  requiresTargets: true,
  targetCount: 1,
  validTargets: [TARGET],
  manaCostString: '{1}',
  availableManaSources: [{ entityId: MANA, name: 'Mana source', producesColorless: true }],
  autoTapPreview: [MANA],
}

function receive(
  epoch: string,
  delta = false,
  pendingDecision?: PendingDecision,
  legalActions: readonly LegalActionInfo[] = [offered],
): void {
  if (delta) {
    const message: StateDeltaUpdateMessage = {
      type: 'stateDeltaUpdate', delta: { players: [] }, events: [], legalActions, interactionEpoch: epoch,
      ...(pendingDecision ? { pendingDecision } : {}),
    }
    handlers.onStateDeltaUpdate(message)
  } else {
    const message: StateUpdateMessage = {
      type: 'stateUpdate', state: position, events: [], legalActions, interactionEpoch: epoch,
      ...(pendingDecision ? { pendingDecision } : {}),
    }
    handlers.onStateUpdate(message)
  }
}

beforeEach(() => {
  send.mockClear()
  setWebSocket({ send, onStateVersionReceived: vi.fn(), requestResync: vi.fn() } as unknown as GameWebSocket)
  useGameStore.setState({ sessionId: 'live-test', playerId: ME, interactionEpoch: null, gameOverState: null })
  receive('original')
})

describe('browser live action origins', () => {
  it('retains an action origin through payment, targeting, and same-timeline delta delivery', () => {
    const actionInfo = useGameStore.getState().legalActions[0]!
    expect(actionInfo.interactionEpoch).toBe('original')
    expect(offered).not.toHaveProperty('interactionEpoch')
    useGameStore.getState().startPipeline(actionInfo, { forceManualTap: true })
    const pipeline = useGameStore.getState().pipelineState
    const payment = useGameStore.getState().manaSelectionState
    expect(pipeline?.remainingPhases.map((phase) => phase.type)).toEqual(['manaSource', 'targeting'])
    expect(payment).not.toBeNull()

    receive('original', true)
    expect(useGameStore.getState().pipelineState).toBe(pipeline)
    expect(useGameStore.getState().manaSelectionState).toBe(payment)
    useGameStore.getState().advancePipeline({ type: 'manaSource', selectedSources: [MANA] })
    useGameStore.getState().addTarget(TARGET)
    useGameStore.getState().confirmTargeting('original')

    expect(send).toHaveBeenCalledOnce()
    expect(send).toHaveBeenCalledWith({
      type: 'submitAction',
      interactionEpoch: 'original',
      action: {
        ...offered.action,
        paymentStrategy: { type: 'Explicit', manaAbilitiesToActivate: [MANA], phyrexianLifePayments: [] },
        targets: [{ type: 'Permanent', entityId: TARGET }],
      },
    })
    expect(send.mock.calls[0]![0].action).not.toHaveProperty('interactionEpoch')
  })

  it('atomically replaces selections and refuses callbacks retaining the old action or origin', () => {
    const oldInfo = useGameStore.getState().legalActions[0]!
    useGameStore.getState().startPipeline(oldInfo, { forceManualTap: true })
    const observed: Array<{ epoch: string | null; selecting: boolean }> = []
    const unsubscribe = useGameStore.subscribe((state) => observed.push({
      epoch: state.interactionEpoch,
      selecting: state.pipelineState != null || state.manaSelectionState != null || state.targetingState != null,
    }))
    receive('replacement')
    unsubscribe()
    expect(observed.some((state) => state.epoch === 'replacement')).toBe(true)
    expect(observed.every((state) => state.epoch !== 'replacement' || !state.selecting)).toBe(true)

    // A queued confirmation and an old menu callback cannot acquire the replacement epoch.
    useGameStore.getState().advancePipeline({ type: 'manaSource', selectedSources: [MANA] })
    useGameStore.getState().confirmTargeting('original')
    useGameStore.getState().startPipeline(oldInfo)
    useGameStore.getState().submitAction(oldInfo.action, oldInfo.interactionEpoch)
    useGameStore.getState().submitAction(oldInfo.action, undefined)
    expect(send).not.toHaveBeenCalled()
    expect(useGameStore.getState().pipelineState).toBeNull()

    const freshInfo = useGameStore.getState().legalActions[0]!
    useGameStore.getState().startPipeline(freshInfo) // Auto-tap leaves just the targeting phase.
    useGameStore.getState().addTarget(TARGET)
    useGameStore.getState().confirmTargeting('replacement')
    expect(send).toHaveBeenCalledOnce()
    expect(send.mock.calls[0]![0].interactionEpoch).toBe('replacement')
  })

  it('drops a combat selection from the replaced timeline without relabeling it', () => {
    const combat: CombatState = {
      interactionEpoch: 'original', mode: 'declareBlockers', actingSeat: ME, stickyDefenderId: null,
      selectedAttackers: [], attackerTargets: {}, validAttackTargets: [], attackTargetsByAttacker: {},
      blockerAssignments: { [MANA]: [TARGET] }, validCreatures: [MANA], mandatoryAttackers: [],
      attackingCreatures: [TARGET], mustBeBlockedAttackers: [], blockerMaxBlockCounts: {}, bands: [],
    }
    useGameStore.getState().startCombat(combat)
    send.mockClear() // The initial blocker preview is not an engine submission.
    receive('replacement', true)
    expect(useGameStore.getState().combatState).toBeNull()
    useGameStore.getState().confirmCombat('original')
    useGameStore.getState().startCombat(combat)
    expect(useGameStore.getState().combatState).toBeNull()
    expect(send).not.toHaveBeenCalled()

    useGameStore.getState().startCombat({ ...combat, interactionEpoch: 'replacement' })
    send.mockClear()
    const freshCombat = useGameStore.getState().combatState
    useGameStore.getState().confirmCombat(combat.interactionEpoch)
    useGameStore.getState().cancelCombat(combat.interactionEpoch)
    expect(send).not.toHaveBeenCalled()
    expect(useGameStore.getState().combatState).toBe(freshCombat)
    useGameStore.getState().confirmCombat('replacement')
    expect(send).toHaveBeenCalledWith({
      type: 'submitAction', interactionEpoch: 'replacement',
      action: { type: 'DeclareBlockers', playerId: ME, blockers: { [MANA]: [TARGET] } },
    })
  })

  it('rejects a held decision callback after undo and after the next question', () => {
    const decision: PendingDecision = {
      type: 'YesNoDecision', id: 'original:question-7', playerId: entityId('controlled-player'),
      prompt: 'Use this ability?', context: { phase: 'RESOLUTION' }, yesText: 'Yes', noText: 'No',
    }
    receive('original', false, decision)
    const submit = useGameStore.getState().submitYesNoDecision
    // The component retains both its chosen payload and the prompt it rendered.
    const heldCallback = () => submit(decision.id, true)

    const restored = { ...decision, id: 'replacement:question-7' }
    receive('replacement', false, restored)
    heldCallback()
    expect(send).not.toHaveBeenCalled()
    expect(useGameStore.getState().pendingDecision).toBe(restored)

    const freshCallback = () => submit(restored.id, true)
    receive('replacement', true, restored)
    freshCallback()
    expect(send).toHaveBeenCalledExactlyOnceWith({
      type: 'submitAction', interactionEpoch: 'replacement',
      action: {
        type: 'SubmitDecision', playerId: restored.playerId,
        response: { type: 'YesNoResponse', decisionId: restored.id, choice: true },
      },
    })

    receive('replacement', false, { ...restored, id: 'replacement:question-8' })
    freshCallback()
    expect(send).toHaveBeenCalledOnce()
  })

  it('rejects a held distribution confirmation without consuming the replacement selection', () => {
    const decision: PendingDecision = {
      type: 'DistributeDecision', id: 'original:distribution', playerId: ME,
      prompt: 'Distribute one counter', context: { phase: 'RESOLUTION' },
      totalAmount: 1, targets: [TARGET], minPerTarget: 0,
    }
    receive('original', false, decision)
    const heldCallback = () => useGameStore.getState().confirmDistribute(decision.id)
    const restored = { ...decision, id: 'replacement:distribution' }
    receive('replacement', false, restored)
    useGameStore.getState().incrementDistribute(TARGET)
    const replacementSelection = useGameStore.getState().distributeState

    heldCallback()
    expect(send).not.toHaveBeenCalled()
    expect(useGameStore.getState().distributeState).toBe(replacementSelection)

    useGameStore.getState().confirmDistribute(restored.id)
    expect(send).toHaveBeenCalledExactlyOnceWith({
      type: 'submitAction', interactionEpoch: 'replacement',
      action: {
        type: 'SubmitDecision', playerId: ME,
        response: {
          type: 'DistributionResponse', decisionId: restored.id, distribution: { [TARGET]: 1 },
        },
      },
    })
  })


  it('rejects held mana confirmation after a replacement pipeline is already active', () => {
    const oldInfo = useGameStore.getState().legalActions[0]!
    useGameStore.getState().startPipeline(oldInfo, { forceManualTap: true })
    const oldSelection = useGameStore.getState().manaSelectionState!
    const executeAction = vi.fn()
    const heldCallback = () => useGameStore.getState().confirmManaSelection(
      'original', oldSelection, executeAction,
    )

    receive('replacement')
    const freshInfo = useGameStore.getState().legalActions[0]!
    useGameStore.getState().startPipeline(freshInfo, { forceManualTap: true })
    const freshSelection = useGameStore.getState().manaSelectionState!
    const freshPipeline = useGameStore.getState().pipelineState
    heldCallback()
    useGameStore.getState().cancelManaSelection('original')
    expect(useGameStore.getState().manaSelectionState).toBe(freshSelection)
    expect(useGameStore.getState().pipelineState).toBe(freshPipeline)
    expect(send).not.toHaveBeenCalled()
    expect(executeAction).not.toHaveBeenCalled()

    useGameStore.getState().confirmManaSelection('replacement', freshSelection, executeAction)
    expect(useGameStore.getState().manaSelectionState).toBeNull()
    expect(useGameStore.getState().pipelineState?.remainingPhases[0]?.type).toBe('targeting')
    useGameStore.getState().addTarget(TARGET)
    useGameStore.getState().confirmTargeting('replacement')
    expect(send).toHaveBeenCalledOnce()
    expect(send.mock.calls[0]![0].interactionEpoch).toBe('replacement')
  })


  it.each(['CrewVehicle', 'SaddleMount'] as const)('rejects held %s confirmation after a replacement selection starts', (type) => {
    const heldConfirm = () => useGameStore.getState().confirmTapForPowerSelection('original')
    const heldCancel = () => useGameStore.getState().cancelTapForPowerSelection('original')
    receive('replacement')
    const action = type === 'CrewVehicle'
      ? { type, playerId: ME, vehicleId: SPELL, crewCreatures: [] }
      : { type, playerId: ME, mountId: SPELL, saddleCreatures: [] }
    const actionInfo: LegalActionInfo = {
      actionType: type, description: type, action, interactionEpoch: 'replacement',
    }
    useGameStore.getState().startTapForPowerSelection({
      actionInfo, sourceId: SPELL, sourceName: 'Test permanent', verb: type,
      requiredPower: 1, selectedCreatures: [TARGET], validCreatures: [], alreadySaddled: false,
    })
    const replacementSelection = useGameStore.getState().tapForPowerSelectionState
    heldConfirm()
    heldCancel()
    expect(useGameStore.getState().tapForPowerSelectionState).toBe(replacementSelection)
    expect(send).not.toHaveBeenCalled()

    useGameStore.getState().confirmTapForPowerSelection('replacement')
    expect(useGameStore.getState().tapForPowerSelectionState).toBeNull()
    expect(send).toHaveBeenCalledWith({
      type: 'submitAction', interactionEpoch: 'replacement',
      action: { ...action, [type === 'CrewVehicle' ? 'crewCreatures' : 'saddleCreatures']: [TARGET] },
    })
  })

  const phaseConfirmations: Array<{
    name: string
    state: Record<string, unknown>
    confirm: (store: GameStore, epoch: string) => void
    cancel: (store: GameStore, epoch: string) => void
  }> = [
    { name: 'modal modes', state: { modalModeSelectionState: {} },
      confirm: (s, e) => s.confirmModalModeSelection(e, [0]), cancel: (s, e) => s.cancelModalModeSelection(e) },
    { name: 'X', state: { xSelectionState: { selectedX: 2 } },
      confirm: (s, e) => s.confirmXSelection(e), cancel: (s, e) => s.cancelXSelection(e) },
    { name: 'blight X', state: { blightVariableSelectionState: { selectedX: 2 } },
      confirm: (s, e) => s.confirmBlightVariableSelection(e), cancel: (s, e) => s.cancelBlightVariableSelection(e) },
    { name: 'pay X life', state: { payXLifeSelectionState: { selectedX: 2 } },
      confirm: (s, e) => s.confirmPayXLifeSelection(e), cancel: (s, e) => s.cancelPayXLifeSelection(e) },
    { name: 'convoke', state: { convokeSelectionState: { selectedCreatures: [{ entityId: TARGET, payingColor: null }] } },
      confirm: (s, e) => s.confirmConvokeSelection(e), cancel: (s, e) => s.cancelConvokeSelection(e) },
    { name: 'tap for generic', state: { tapForGenericSelectionState: { selectedPermanents: [TARGET] } },
      confirm: (s, e) => s.confirmTapForGenericSelection(e), cancel: (s, e) => s.cancelTapForGenericSelection(e) },
    { name: 'harmonize', state: { harmonizeSelectionState: { selectedCreature: TARGET, validCreatures: [{ entityId: TARGET, power: 2 }] } },
      confirm: (s, e) => s.confirmHarmonizeSelection(e), cancel: (s, e) => s.cancelHarmonizeSelection(e) },
    { name: 'delve', state: { delveSelectionState: { selectedCards: [TARGET], manaCost: '{2}' } },
      confirm: (s, e) => s.confirmDelveSelection(e), cancel: (s, e) => s.cancelDelveSelection(e) },
    { name: 'mana color', state: { manaColorSelectionState: {} },
      confirm: (s, e) => s.confirmManaColorSelection(e, 'GREEN'), cancel: (s, e) => s.cancelManaColorSelection(e) },
    { name: 'targets', state: { targetingState: { selectedTargets: [TARGET] } },
      confirm: (s, e) => s.confirmTargeting(e), cancel: (s, e) => s.cancelTargeting(e) },
    { name: 'damage distribution', state: { damageDistributionState: { distribution: { [TARGET]: 1 } } },
      confirm: (s, e) => s.confirmDamageDistribution(e), cancel: (s, e) => s.cancelDamageDistribution(e) },
    { name: 'counter distribution', state: { counterDistributionState: { distribution: { [TARGET]: { '+1/+1': 1 } }, requiredTotal: 1 } },
      confirm: (s, e) => s.confirmCounterDistribution(e), cancel: (s, e) => s.cancelCounterDistribution(e) },
  ]

  it.each(phaseConfirmations)('keeps replacement $name selections intact when an old callback fires', ({ state, confirm, cancel }) => {
    const heldConfirm = () => confirm(useGameStore.getState(), 'original')
    const heldCancel = () => cancel(useGameStore.getState(), 'original')
    receive('replacement')
    useGameStore.getState().startPipeline(useGameStore.getState().legalActions[0]!)
    const realAdvance = useGameStore.getState().advancePipeline
    const realCancel = useGameStore.getState().cancelPipeline
    const advance = vi.fn()
    const cancelPipeline = vi.fn()
    // Isolate submission ownership from the later strategy phases; retain each phase's selected payload.
    useGameStore.setState({ ...state, advancePipeline: advance, cancelPipeline } as Partial<GameStore>)
    try {
      const replacementState = useGameStore.getState()
      heldConfirm()
      heldCancel()
      expect(useGameStore.getState()).toBe(replacementState)
      expect(advance).not.toHaveBeenCalled()
      expect(cancelPipeline).not.toHaveBeenCalled()
      expect(send).not.toHaveBeenCalled()

      confirm(useGameStore.getState(), 'replacement')
      expect(advance).toHaveBeenCalledOnce()
    } finally {
      useGameStore.setState({ advancePipeline: realAdvance, cancelPipeline: realCancel })
    }
  })


  it('drops a half-built action once a same-timeline update stops offering it', () => {
    useGameStore.getState().startPipeline(useGameStore.getState().legalActions[0]!)
    useGameStore.getState().addTarget(TARGET)
    const targeting = useGameStore.getState().targetingState
    expect(targeting?.selectedTargets).toEqual([TARGET])

    // Still offered: an unrelated delta leaves the picks alone.
    receive('original', true)
    expect(useGameStore.getState().targetingState).toBe(targeting)

    // The game moved on (priority passed, the stack resolved) without undo: the epoch is the
    // same, but the spell is no longer castable, so its banner must not outlive it.
    receive('original', true, undefined, [])
    expect(useGameStore.getState().pipelineState).toBeNull()
    expect(useGameStore.getState().targetingState).toBeNull()
    expect(send).not.toHaveBeenCalled()
  })
})
