package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.CounterUnlessDiscardContinuation
import com.wingedsheep.engine.core.CounterUnlessPaysLifeContinuation
import com.wingedsheep.engine.core.CounterUnlessPaysManaSelectionContinuation
import com.wingedsheep.engine.core.CounterUnlessSacrificeContinuation
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ManaSourceOption
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.effects.WardCounterEffect
import kotlin.reflect.KClass

/**
 * Executor for WardCounterEffect.
 *
 * When a ward trigger resolves:
 * 1. Find the spell/ability that targeted the warded permanent (via targetingSourceEntityId).
 * 2. If it has already left the stack, do nothing.
 * 3. Branch on the ward cost:
 *    - WardCost.Mana → SelectManaSourcesDecision (canDecline=true)
 *    - WardCost.Life → YesNoDecision ("Pay N life?")
 *    - WardCost.Discard → YesNoDecision ("Discard N card(s)?"); on Yes, runs the
 *      standard discard pipeline (random or player's choice).
 *    - WardCost.Sacrifice → SelectCardsDecision over the controller's matching permanents
 *      (min 0, max N); selecting N pays and the spell resolves, declining counters it.
 *    - WardCost.Composite → pay each component cost in order (CR 702.21a; "Ward—{2}, Pay 2
 *      life"). Each component reuses the per-component flow above and carries the remaining
 *      components so the resumer charges the next one after a successful payment. Declining or
 *      being unable to pay any component counters the spell/ability immediately.
 *    If the controller can't possibly pay, counter immediately.
 *
 * The per-component handlers are reusable from
 * [com.wingedsheep.engine.handlers.continuations.ManaPaymentContinuationResumer] (via
 * [chargeWardCost]) so a composite cost can resume into its next component after each payment
 * without re-deriving the ward source/controller context.
 */
class WardCounterEffectExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<WardCounterEffect> {
    override val effectType: KClass<WardCounterEffect> = WardCounterEffect::class

    override fun execute(
        state: GameState,
        effect: WardCounterEffect,
        context: EffectContext
    ): EffectResult {
        val spellEntityId = context.targetingSourceEntityId
            ?: return EffectResult.success(state)

        if (!state.stack.contains(spellEntityId)) {
            return EffectResult.success(state)
        }

        val container = state.getEntity(spellEntityId)
            ?: return EffectResult.success(state)

        val payingPlayerId = container.get<SpellOnStackComponent>()?.casterId
            ?: container.get<ActivatedAbilityOnStackComponent>()?.controllerId
            ?: container.get<TriggeredAbilityOnStackComponent>()?.controllerId
            ?: return EffectResult.success(state)

        return chargeWardCost(
            state = state,
            cardRegistry = cardRegistry,
            cost = effect.cost,
            remainingParts = emptyList(),
            spellEntityId = spellEntityId,
            container = container,
            payingPlayerId = payingPlayerId,
            wardSourceId = context.sourceId,
            controllerId = context.controllerId
        )
    }

    companion object {
        /**
         * Charge a single ward [cost] against [payingPlayerId], carrying [remainingParts] (the
         * not-yet-paid components of an enclosing [WardCost.Composite]) so the continuation
         * resumer can charge the next component once this one is paid. Pass `emptyList()` for an
         * atomic (non-composite) ward cost.
         *
         * A [WardCost.Composite] unrolls into "charge `parts.first()` with `remainingParts =
         * parts.drop(1)`". The components themselves must be atomic ward costs (the SDK forbids
         * nesting another Composite).
         */
        fun chargeWardCost(
            state: GameState,
            cardRegistry: CardRegistry,
            cost: WardCost,
            remainingParts: List<WardCost>,
            spellEntityId: EntityId,
            container: ComponentContainer,
            payingPlayerId: EntityId,
            wardSourceId: EntityId?,
            controllerId: EntityId?
        ): EffectResult {
            return when (cost) {
                is WardCost.Mana -> handleManaCost(
                    state, cardRegistry, spellEntityId, container, payingPlayerId,
                    cost.manaCost, remainingParts, wardSourceId, controllerId
                )
                is WardCost.Life -> handleLifeCost(
                    state, cardRegistry, spellEntityId, container, payingPlayerId,
                    cost.amount, remainingParts, wardSourceId, controllerId
                )
                is WardCost.Discard -> handleDiscardCost(
                    state, cardRegistry, spellEntityId, container, payingPlayerId,
                    cost.count, cost.random, cost.filter, remainingParts, wardSourceId, controllerId
                )
                is WardCost.Sacrifice -> handleSacrificeCost(
                    state, cardRegistry, spellEntityId, container, payingPlayerId,
                    cost.filter, remainingParts, wardSourceId, controllerId
                )
                is WardCost.Composite -> {
                    require(cost.parts.isNotEmpty()) { "WardCost.Composite must have at least one part" }
                    chargeWardCost(
                        state, cardRegistry, cost.parts.first(), cost.parts.drop(1) + remainingParts,
                        spellEntityId, container, payingPlayerId, wardSourceId, controllerId
                    )
                }
            }
        }

        /**
         * Ward—Sacrifice [filter] (e.g. "Sacrifice a Food").
         *
         * Valid sacrifice fodder is computed against projected state via
         * [BattlefieldFilterUtils.findMatchingOnBattlefield], so subtypes granted by continuous
         * effects (Ygra, Eater of All making every other creature a Food) count. If the paying
         * player controls no qualifying permanent they cannot pay, so the spell is countered
         * immediately. Otherwise they pick which permanent(s) to sacrifice (declining → counter).
         */
        private fun handleSacrificeCost(
            state: GameState,
            cardRegistry: CardRegistry,
            spellEntityId: EntityId,
            container: ComponentContainer,
            payingPlayerId: EntityId,
            filter: GameObjectFilter,
            remainingParts: List<WardCost>,
            wardSourceId: EntityId?,
            controllerId: EntityId?
        ): EffectResult {
            val count = 1
            val validPermanents = BattlefieldFilterUtils.findMatchingOnBattlefield(
                state, filter.youControl(), PredicateContext(controllerId = payingPlayerId)
            )

            // Can't possibly pay → counter immediately.
            if (validPermanents.size < count) {
                return counterSpellOrAbility(state, cardRegistry, spellEntityId, container)
            }

            val fodderLabel = filter.description
            val prompt = "Sacrifice ${if (count == 1) "a" else "$count"} $fodderLabel or your spell will be countered"

            val decisionResult = DecisionHandler().createCardSelectionDecision(
                state = state,
                playerId = payingPlayerId,
                sourceId = wardSourceId,
                sourceName = "Ward",
                prompt = prompt,
                options = validPermanents,
                minSelections = 0,
                maxSelections = count,
                ordered = false,
                phase = DecisionPhase.RESOLUTION,
                useTargetingUI = true
            )

            val continuation = CounterUnlessSacrificeContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                payingPlayerId = payingPlayerId,
                spellEntityId = spellEntityId,
                filter = filter,
                count = count,
                controllerId = controllerId,
                remainingWardParts = remainingParts,
                wardSourceId = wardSourceId
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

            return EffectResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                decisionResult.events
            )
        }

        private fun handleDiscardCost(
            state: GameState,
            cardRegistry: CardRegistry,
            spellEntityId: EntityId,
            container: ComponentContainer,
            payingPlayerId: EntityId,
            count: Int,
            random: Boolean,
            filter: GameObjectFilter?,
            remainingParts: List<WardCost>,
            wardSourceId: EntityId?,
            controllerId: EntityId?
        ): EffectResult {
            // Not enough eligible cards in hand → counter immediately. When the ward names a
            // card type (Saruman of Many Colors: "Discard an enchantment, instant, or sorcery
            // card"), only matching cards count toward the can-pay check.
            // (The caster spends the spell as part of casting, so the spell itself is not in hand here.)
            val eligibleCount = if (filter == null) {
                state.getHand(payingPlayerId).size
            } else {
                val predicateContext = PredicateContext(controllerId = payingPlayerId)
                val predicateEvaluator = PredicateEvaluator()
                state.getHand(payingPlayerId).count { cardId ->
                    predicateEvaluator.matches(state, state.projectedState, cardId, filter, predicateContext)
                }
            }
            if (eligibleCount < count) {
                return counterSpellOrAbility(state, cardRegistry, spellEntityId, container)
            }

            val cardsLabel = if (filter != null) {
                if (count == 1) "a ${filter.description}" else "$count ${filter.description} cards"
            } else {
                if (count == 1) "a card" else "$count cards"
            }
            val randomSuffix = if (random) " at random" else ""
            val decisionId = java.util.UUID.randomUUID().toString()
            val decision = YesNoDecision(
                id = decisionId,
                playerId = payingPlayerId,
                prompt = "Discard $cardsLabel$randomSuffix or your spell will be countered",
                context = DecisionContext(
                    sourceId = wardSourceId,
                    sourceName = "Ward",
                    phase = DecisionPhase.RESOLUTION
                ),
                yesText = "Discard $cardsLabel$randomSuffix",
                noText = "Counter spell"
            )

            val continuation = CounterUnlessDiscardContinuation(
                decisionId = decisionId,
                payingPlayerId = payingPlayerId,
                spellEntityId = spellEntityId,
                count = count,
                random = random,
                filter = filter,
                controllerId = controllerId,
                remainingWardParts = remainingParts,
                wardSourceId = wardSourceId
            )

            val stateWithDecision = state.withPendingDecision(decision)
            val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

            return EffectResult.paused(
                stateWithContinuation,
                decision,
                listOf(
                    DecisionRequestedEvent(
                        decisionId = decisionId,
                        playerId = payingPlayerId,
                        decisionType = "YES_NO",
                        prompt = decision.prompt
                    )
                )
            )
        }

        private fun handleManaCost(
            state: GameState,
            cardRegistry: CardRegistry,
            spellEntityId: EntityId,
            container: ComponentContainer,
            payingPlayerId: EntityId,
            manaCostString: String,
            remainingParts: List<WardCost>,
            wardSourceId: EntityId?,
            controllerId: EntityId?
        ): EffectResult {
            val manaCost = ManaCost.parse(manaCostString)

            val manaSolver = ManaSolver(cardRegistry)
            if (!manaSolver.canPay(state, payingPlayerId, manaCost)) {
                return counterSpellOrAbility(state, cardRegistry, spellEntityId, container)
            }

            val sources = manaSolver.findAvailableManaSources(state, payingPlayerId)
            val sourceOptions = sources.map { source ->
                ManaSourceOption(
                    entityId = source.entityId,
                    name = source.name,
                    producesColors = source.producesColors,
                    producesColorless = source.producesColorless,
                    requiresSacrifice = source.requiresSacrifice,
                    requiresTappingAnotherPermanent = source.tapPermanentsSubCost != null
                )
            }

            val solution = manaSolver.solve(state, payingPlayerId, manaCost)
            val autoPaySuggestion = solution?.sources?.map { it.entityId } ?: emptyList()

            val decisionId = java.util.UUID.randomUUID().toString()
            val decision = SelectManaSourcesDecision(
                id = decisionId,
                playerId = payingPlayerId,
                prompt = "Pay $manaCost for ward or your spell will be countered",
                context = DecisionContext(
                    sourceId = wardSourceId,
                    sourceName = "Ward",
                    phase = DecisionPhase.RESOLUTION
                ),
                availableSources = sourceOptions,
                requiredCost = manaCost.toString(),
                autoPaySuggestion = autoPaySuggestion,
                canDecline = true
            )

            val continuation = CounterUnlessPaysManaSelectionContinuation(
                decisionId = decisionId,
                payingPlayerId = payingPlayerId,
                spellEntityId = spellEntityId,
                manaCost = manaCost,
                availableSources = sourceOptions,
                autoPaySuggestion = autoPaySuggestion,
                controllerId = controllerId,
                remainingWardParts = remainingParts,
                wardSourceId = wardSourceId
            )

            val stateWithDecision = state.withPendingDecision(decision)
            val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

            return EffectResult.paused(
                stateWithContinuation,
                decision,
                listOf(
                    DecisionRequestedEvent(
                        decisionId = decisionId,
                        playerId = payingPlayerId,
                        decisionType = "SELECT_MANA_SOURCES",
                        prompt = decision.prompt
                    )
                )
            )
        }

        private fun handleLifeCost(
            state: GameState,
            cardRegistry: CardRegistry,
            spellEntityId: EntityId,
            container: ComponentContainer,
            payingPlayerId: EntityId,
            lifeCost: Int,
            remainingParts: List<WardCost>,
            wardSourceId: EntityId?,
            controllerId: EntityId?
        ): EffectResult {
            val currentLife = state.lifeTotal(payingPlayerId) // CR 810.9a — team's shared total
            if (currentLife < lifeCost) {
                // Can't pay — counter immediately.
                return counterSpellOrAbility(state, cardRegistry, spellEntityId, container)
            }

            val decisionId = java.util.UUID.randomUUID().toString()
            val decision = YesNoDecision(
                id = decisionId,
                playerId = payingPlayerId,
                prompt = "Pay $lifeCost life or your spell will be countered",
                context = DecisionContext(
                    sourceId = wardSourceId,
                    sourceName = "Ward",
                    phase = DecisionPhase.RESOLUTION
                ),
                yesText = "Pay $lifeCost life",
                noText = "Counter spell"
            )

            val continuation = CounterUnlessPaysLifeContinuation(
                decisionId = decisionId,
                payingPlayerId = payingPlayerId,
                spellEntityId = spellEntityId,
                lifeCost = lifeCost,
                controllerId = controllerId,
                remainingWardParts = remainingParts,
                wardSourceId = wardSourceId
            )

            val stateWithDecision = state.withPendingDecision(decision)
            val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

            return EffectResult.paused(
                stateWithContinuation,
                decision,
                listOf(
                    DecisionRequestedEvent(
                        decisionId = decisionId,
                        playerId = payingPlayerId,
                        decisionType = "YES_NO",
                        prompt = decision.prompt
                    )
                )
            )
        }

        private fun counterSpellOrAbility(
            state: GameState,
            cardRegistry: CardRegistry,
            entityId: EntityId,
            container: ComponentContainer
        ): EffectResult {
            val resolver = StackResolver(cardRegistry = cardRegistry)
            return EffectResult.from(if (container.has<SpellOnStackComponent>()) {
                resolver.counterSpell(state, entityId)
            } else {
                resolver.counterAbility(state, entityId)
            })
        }
    }
}
