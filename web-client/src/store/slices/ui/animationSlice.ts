/**
 * Animation sub-slice — handles card selection, hover, all 5 animation types,
 * revealed cards/hand, match intro, and auto-tap preview.
 */
import type {
  SliceCreator,
  EntityId,
  DrawAnimation,
  DamageAnimation,
  RevealAnimation,
  CoinFlipAnimation,
  TargetReselectedAnimation,
  MatchIntro,
} from '../types'

export interface AnimationSliceState {
  selectedCardId: EntityId | null
  hoveredCardId: EntityId | null
  hoverPosition: { x: number; y: number } | null
  autoTapPreview: readonly EntityId[] | null
  revealedHandCardIds: readonly EntityId[] | null
  revealedCardsInfo: {
    cardIds: readonly EntityId[]
    cardNames: readonly string[]
    imageUris: readonly (string | null)[]
    source: string | null
    isYourReveal: boolean
  } | null
  drawAnimations: readonly DrawAnimation[]
  damageAnimations: readonly DamageAnimation[]
  revealAnimations: readonly RevealAnimation[]
  coinFlipAnimations: readonly CoinFlipAnimation[]
  targetReselectedAnimations: readonly TargetReselectedAnimation[]
  matchIntro: MatchIntro | null
}

export interface AnimationSliceActions {
  selectCard: (cardId: EntityId | null) => void
  hoverCard: (cardId: EntityId | null, position?: { x: number; y: number }) => void
  updateHoverPosition: (position: { x: number; y: number }) => void
  setAutoTapPreview: (preview: readonly EntityId[] | null) => void
  showRevealedHand: (cardIds: readonly EntityId[]) => void
  dismissRevealedHand: () => void
  showRevealedCards: (cardIds: readonly EntityId[], cardNames: readonly string[], imageUris: readonly (string | null)[], source: string | null, isYourReveal: boolean) => void
  dismissRevealedCards: () => void
  addDrawAnimation: (animation: DrawAnimation) => void
  removeDrawAnimation: (id: string) => void
  addDamageAnimation: (animation: DamageAnimation) => void
  removeDamageAnimation: (id: string) => void
  addRevealAnimation: (animation: RevealAnimation) => void
  removeRevealAnimation: (id: string) => void
  addCoinFlipAnimation: (animation: CoinFlipAnimation) => void
  removeCoinFlipAnimation: (id: string) => void
  addTargetReselectedAnimation: (animation: TargetReselectedAnimation) => void
  removeTargetReselectedAnimation: (id: string) => void
  setMatchIntro: (intro: MatchIntro) => void
  clearMatchIntro: () => void
}

export type AnimationSlice = AnimationSliceState & AnimationSliceActions

export const createAnimationSlice: SliceCreator<AnimationSlice> = (set, get) => ({
  selectedCardId: null,
  hoveredCardId: null,
  hoverPosition: null,
  autoTapPreview: null,
  revealedHandCardIds: null,
  revealedCardsInfo: null,
  drawAnimations: [],
  damageAnimations: [],
  revealAnimations: [],
  coinFlipAnimations: [],
  targetReselectedAnimations: [],
  matchIntro: null,

  // Card selection actions
  selectCard: (cardId) => {
    set({ selectedCardId: cardId })
  },

  hoverCard: (cardId, position) => {
    let autoTapPreview: readonly EntityId[] | null = null
    if (cardId) {
      const { legalActions, pendingDecision } = get()
      if (!pendingDecision) {
        const castAction = legalActions.find(
          (a) => a.action.type === 'CastSpell' && a.action.cardId === cardId
        )
        if (castAction?.autoTapPreview) {
          autoTapPreview = castAction.autoTapPreview
        } else {
          const turnFaceUpAction = legalActions.find(
            (a) => a.action.type === 'TurnFaceUp' && a.action.sourceId === cardId
          )
          if (turnFaceUpAction?.autoTapPreview) {
            autoTapPreview = turnFaceUpAction.autoTapPreview
          }
        }
      }
    }
    set({ hoveredCardId: cardId, hoverPosition: position ?? null, autoTapPreview })
  },

  updateHoverPosition: (position) => {
    set({ hoverPosition: position })
  },

  setAutoTapPreview: (preview) => {
    set({ autoTapPreview: preview })
  },

  // Revealed cards actions
  showRevealedHand: (cardIds) => {
    set({ revealedHandCardIds: cardIds })
  },

  dismissRevealedHand: () => {
    set({ revealedHandCardIds: null })
  },

  showRevealedCards: (cardIds, cardNames, imageUris, source, isYourReveal) => {
    set({ revealedCardsInfo: { cardIds, cardNames, imageUris, source, isYourReveal } })
  },

  dismissRevealedCards: () => {
    set({ revealedCardsInfo: null })
  },

  // Animation actions
  addDrawAnimation: (animation) => {
    set((state) => ({
      drawAnimations: [...state.drawAnimations, animation],
    }))
  },

  removeDrawAnimation: (id) => {
    set((state) => ({
      drawAnimations: state.drawAnimations.filter((a) => a.id !== id),
    }))
  },

  addDamageAnimation: (animation) => {
    set((state) => ({
      damageAnimations: [...state.damageAnimations, animation],
    }))
  },

  removeDamageAnimation: (id) => {
    set((state) => ({
      damageAnimations: state.damageAnimations.filter((a) => a.id !== id),
    }))
  },

  addRevealAnimation: (animation) => {
    set((state) => ({
      revealAnimations: [...state.revealAnimations, animation],
    }))
  },

  removeRevealAnimation: (id) => {
    set((state) => ({
      revealAnimations: state.revealAnimations.filter((a) => a.id !== id),
    }))
  },

  addCoinFlipAnimation: (animation) => {
    set((state) => ({
      coinFlipAnimations: [...state.coinFlipAnimations, animation],
    }))
  },

  removeCoinFlipAnimation: (id) => {
    set((state) => ({
      coinFlipAnimations: state.coinFlipAnimations.filter((a) => a.id !== id),
    }))
  },

  addTargetReselectedAnimation: (animation) => {
    set((state) => ({
      targetReselectedAnimations: [...state.targetReselectedAnimations, animation],
    }))
  },

  removeTargetReselectedAnimation: (id) => {
    set((state) => ({
      targetReselectedAnimations: state.targetReselectedAnimations.filter((a) => a.id !== id),
    }))
  },

  setMatchIntro: (intro) => {
    set({ matchIntro: intro })
  },

  clearMatchIntro: () => {
    set({ matchIntro: null })
  },
})
