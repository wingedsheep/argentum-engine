package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.CounterUnlessCollectEvidenceContinuation
import com.wingedsheep.engine.core.CounterUnlessDiscardContinuation
import com.wingedsheep.engine.core.CounterUnlessPaysLifeContinuation
import com.wingedsheep.engine.core.CounterUnlessPaysManaSelectionContinuation
import com.wingedsheep.engine.core.CounterUnlessPlayerCountersContinuation
import com.wingedsheep.engine.core.CounterUnlessSacrificeContinuation
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.WardCostChoiceContinuation
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ManaSourceOption
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.WaterbendPermanentChoice
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.costs.CollectEvidenceResolver
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.SacrificeImmunity
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.legalactions.TapForGenericPermanentData
import com.wingedsheep.engine.mechanics.mana.TapForGeneric
import com.wingedsheep.engine.legalactions.utils.CostEnumerationUtils
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
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
 *    - WardCost.CollectEvidence → SelectCardsDecision over the controller's graveyard with a
 *      `minTotalManaValue` floor (CR 701.59's sum gate); an empty selection declines.
 *    - WardCost.PlayerCounters → YesNoDecision ("Get five poison counters?"); on Yes, places the
 *      counters on the paying player (CR 122.1). Always payable.
 *    - WardCost.Composite → pay each component cost in order (CR 702.21a; "Ward—{2}, Pay 2
 *      life"). Each component reuses the per-component flow above and carries the remaining
 *      components so the resumer charges the next one after a successful payment. Declining or
 *      being unable to pay any component counters the spell/ability immediately.
 *    - WardCost.Choice → ChooseOptionDecision over the options the payer can actually pay, plus a
 *      trailing "Counter spell" decline. The chosen option is then charged through the branch above
 *      for its own shape, so the disjunction adds a picker and nothing else.
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

        val payingPlayerId = TargetResolutionUtils.stackObjectController(state, spellEntityId)
            ?: return EffectResult.success(state)

        // Resolve any DynamicLife component to a fixed Life amount at ward-resolution time
        // (CR 702.21b): "Ward—Pay life equal to ~" reads the live value now — e.g. Raubahn's
        // power, using last-known information if Raubahn has left the battlefield (handled by
        // EntityReference.Source's LkiPolicy). All downstream payment / continuation machinery
        // then operates on a plain WardCost.Life, so no other branch needs to change.
        val resolvedCost = resolveDynamicLife(state, effect.cost, context)

        return chargeWardCost(
            state = state,
            cardRegistry = cardRegistry,
            cost = resolvedCost,
            remainingParts = emptyList(),
            spellEntityId = spellEntityId,
            container = container,
            payingPlayerId = payingPlayerId,
            wardSourceId = context.sourceId,
            controllerId = context.controllerId
        )
    }

    /**
     * Replace every [WardCost.DynamicLife] (including those nested in a [WardCost.Composite] or a
     * [WardCost.Choice]) with a fixed [WardCost.Life] by evaluating its
     * [com.wingedsheep.sdk.scripting.values.DynamicAmount] against the current state, clamped to
     * >= 0 (CR 702.21b — the amount is read when the ward ability *resolves*, which is now). Doing
     * it here rather than at payment time also keeps the value stable across a disjunction's picker
     * prompt. Other cost shapes pass through unchanged.
     */
    private fun resolveDynamicLife(
        state: GameState,
        cost: WardCost,
        context: EffectContext
    ): WardCost = when (cost) {
        is WardCost.DynamicLife ->
            WardCost.Life(DynamicAmountEvaluator().evaluate(state, cost.amount, context).coerceAtLeast(0))
        is WardCost.Composite ->
            WardCost.Composite(cost.parts.map { resolveDynamicLife(state, it, context) })
        is WardCost.Choice ->
            WardCost.Choice(cost.options.map { resolveDynamicLife(state, it, context) })
        else -> cost
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
                    cost.manaCost, cost.waterbend, remainingParts, wardSourceId, controllerId
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
                    cost.filter, cost.count, remainingParts, wardSourceId, controllerId
                )
                is WardCost.CollectEvidence -> handleCollectEvidenceCost(
                    state, cardRegistry, spellEntityId, payingPlayerId,
                    cost.amount, remainingParts, wardSourceId, controllerId
                )
                is WardCost.PlayerCounters -> handlePlayerCountersCost(
                    state, spellEntityId, payingPlayerId,
                    cost.counterType, cost.amount, remainingParts, wardSourceId, controllerId
                )
                is WardCost.Choice -> handleChoiceCost(
                    state, cardRegistry, spellEntityId, payingPlayerId,
                    cost.options, remainingParts, wardSourceId, controllerId
                )
                is WardCost.Composite -> {
                    require(cost.parts.isNotEmpty()) { "WardCost.Composite must have at least one part" }
                    chargeWardCost(
                        state, cardRegistry, cost.parts.first(), cost.parts.drop(1) + remainingParts,
                        spellEntityId, container, payingPlayerId, wardSourceId, controllerId
                    )
                }
                // The dynamic life amount is resolved to a fixed WardCost.Life by execute()'s
                // resolveDynamicLife before any cost reaches here (including inside a Composite),
                // so this branch is unreachable; charge a 0-life cost defensively if it ever isn't.
                is WardCost.DynamicLife -> handleLifeCost(
                    state, cardRegistry, spellEntityId, container, payingPlayerId,
                    0, remainingParts, wardSourceId, controllerId
                )
            }
        }

        /**
         * Can [payingPlayerId] pay [cost] right now?
         *
         * The single source of truth for "unpayable ward cost → counter without a prompt"
         * (CR 702.21a). Each per-cost handler consults it before prompting, and
         * [WardCost.Choice] uses it to offer only the options the payer can actually take, so a
         * disjunction can never advertise a leg the payer would then fail to pay.
         *
         * [WardCost.PlayerCounters] is always payable — a player can always get counters — and a
         * [WardCost.Composite] is payable only if *every* part is, mirroring its AND semantics.
         * The composite check is a snapshot: paying an earlier part can in principle make a later
         * one unpayable, and the per-part handler still counters at that point.
         */
        private fun canPayWardCost(
            state: GameState,
            cardRegistry: CardRegistry,
            cost: WardCost,
            payingPlayerId: EntityId,
            controllerId: EntityId?
        ): Boolean = when (cost) {
            is WardCost.Mana -> canAffordManaCost(
                state, cardRegistry, payingPlayerId, ManaCost.parse(cost.manaCost), cost.waterbend
            )
            // CR 119.4 — a player can pay only life they have.
            is WardCost.Life -> state.lifeTotal(payingPlayerId) >= cost.amount
            // Resolved to a fixed Life before it ever reaches here; treat as free defensively.
            is WardCost.DynamicLife -> true
            is WardCost.Discard ->
                eligibleDiscardCount(state, payingPlayerId, cost.filter) >= cost.count
            is WardCost.Sacrifice ->
                !SacrificeImmunity.appliesTo(state, payingPlayerId, controllerId) &&
                    sacrificeCandidates(state, payingPlayerId, cost.filter).size >= cost.count
            // CR 701.59b fails closed — a graveyard that can't reach the total means the payer
            // can't choose to collect evidence at all. Same resolver gate [handleCollectEvidenceCost]
            // applies, so the two can't drift.
            is WardCost.CollectEvidence ->
                CollectEvidenceResolver.candidates(state, payingPlayerId).canReach(cost.amount)
            is WardCost.PlayerCounters -> true
            is WardCost.Composite ->
                cost.parts.all { canPayWardCost(state, cardRegistry, it, payingPlayerId, controllerId) }
            is WardCost.Choice ->
                cost.options.any { canPayWardCost(state, cardRegistry, it, payingPlayerId, controllerId) }
        }

        /**
         * Ward—Get N [counterType] counters (e.g. The Serpent Society's "Ward—Get five poison
         * counters", CR 122.1 — a counter is a marker placed on an object *or player*).
         *
         * Always payable, so this is a plain yes/no with no can-pay gate. On Yes the counters are
         * placed through the ordinary `AddCountersEffect` executor targeting the payer, so counter
         * replacement effects and `CountersAddedEvent` behave exactly as for any other source of
         * player counters (and the poison state-based action, CR 122.1f, follows from that).
         */
        private fun handlePlayerCountersCost(
            state: GameState,
            spellEntityId: EntityId,
            payingPlayerId: EntityId,
            counterType: String,
            amount: Int,
            remainingParts: List<WardCost>,
            wardSourceId: EntityId?,
            controllerId: EntityId?
        ): EffectResult {
            val label = WardCost.PlayerCounters(counterType, amount).clause
                .replaceFirstChar { it.uppercase() }
            val decisionId = java.util.UUID.randomUUID().toString()
            val decision = YesNoDecision(
                id = decisionId,
                playerId = payingPlayerId,
                prompt = "$label or your spell will be countered",
                context = DecisionContext(
                    sourceId = wardSourceId,
                    sourceName = "Ward",
                    phase = DecisionPhase.RESOLUTION
                ),
                yesText = label,
                noText = "Counter spell"
            )

            val continuation = CounterUnlessPlayerCountersContinuation(
                decisionId = decisionId,
                payingPlayerId = payingPlayerId,
                spellEntityId = spellEntityId,
                counterType = counterType,
                amount = amount,
                controllerId = controllerId,
                remainingWardParts = remainingParts,
                wardSourceId = wardSourceId
            )

            val stateWithContinuation = state.withPendingDecision(decision).pushContinuation(continuation)

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

        /**
         * Ward—[option] or [option] (e.g. Titania, Rugged Rumbler's "Ward—Discard a card or pay
         * {2}") — the disjunction. Only the options [canPayWardCost] says the payer can take are
         * offered, plus a trailing "Counter spell" decline; if none is payable the spell is countered without
         * a prompt. Picking one charges it through its own ordinary handler, so this branch adds a
         * picker and no payment logic of its own.
         *
         * The shape mirrors `CostPaymentService.choicePrompt` (the same "payable options plus a
         * trailing decline, with the reduced list stored on the continuation" pattern), with one
         * deliberate difference: the decline is labelled "Counter spell" rather than "Don't pay",
         * because declining a ward cost has a consequence worth naming — and it matches the
         * `noText` the existing yes/no ward prompts already use.
         */
        private fun handleChoiceCost(
            state: GameState,
            cardRegistry: CardRegistry,
            spellEntityId: EntityId,
            payingPlayerId: EntityId,
            options: List<WardCost>,
            remainingParts: List<WardCost>,
            wardSourceId: EntityId?,
            controllerId: EntityId?
        ): EffectResult {
            require(options.isNotEmpty()) { "WardCost.Choice must have at least one option" }

            val payable = options.filter {
                canPayWardCost(state, cardRegistry, it, payingPlayerId, controllerId)
            }
            if (payable.isEmpty()) {
                return counterSpellOrAbility(state, cardRegistry, spellEntityId)
            }

            val labels = payable.map { it.clause.replaceFirstChar { ch -> ch.uppercase() } } +
                "Counter spell"
            val decisionId = java.util.UUID.randomUUID().toString()
            val decision = ChooseOptionDecision(
                id = decisionId,
                playerId = payingPlayerId,
                prompt = "Choose one to pay for ward, or your spell will be countered",
                context = DecisionContext(
                    sourceId = wardSourceId,
                    sourceName = "Ward",
                    phase = DecisionPhase.RESOLUTION
                ),
                options = labels
            )

            // Store the reduced (payable-only) option list so the resumer maps the chosen index
            // directly — the same trick CostPaymentService uses for PayCost.Choice.
            val continuation = WardCostChoiceContinuation(
                decisionId = decisionId,
                payingPlayerId = payingPlayerId,
                spellEntityId = spellEntityId,
                options = payable,
                controllerId = controllerId,
                remainingWardParts = remainingParts,
                wardSourceId = wardSourceId
            )

            val stateWithContinuation = state.withPendingDecision(decision).pushContinuation(continuation)

            return EffectResult.paused(
                stateWithContinuation,
                decision,
                listOf(
                    DecisionRequestedEvent(
                        decisionId = decisionId,
                        playerId = payingPlayerId,
                        decisionType = "CHOOSE_OPTION",
                        prompt = decision.prompt
                    )
                )
            )
        }

        /**
         * Permanents [payingPlayerId] controls that can pay a Ward—Sacrifice cost. Computed
         * against **projected** state so subtypes granted by continuous effects (Ygra, Eater of
         * All making every other creature a Food) count.
         */
        private fun sacrificeCandidates(
            state: GameState,
            payingPlayerId: EntityId,
            filter: GameObjectFilter
        ): List<EntityId> = BattlefieldFilterUtils.findMatchingOnBattlefield(
            state, filter.youControl(), PredicateContext(controllerId = payingPlayerId)
        )

        /**
         * Cards in [payingPlayerId]'s hand that could pay a Ward—Discard cost. When the ward names
         * a card type (Saruman of Many Colors' "Discard an enchantment, instant, or sorcery
         * card"), only matching cards count. The caster spends the spell as part of casting, so
         * the spell itself is not in hand here.
         */
        private fun eligibleDiscardCount(
            state: GameState,
            payingPlayerId: EntityId,
            filter: GameObjectFilter?
        ): Int {
            if (filter == null) return state.getHand(payingPlayerId).size
            val predicateContext = PredicateContext(controllerId = payingPlayerId)
            val predicateEvaluator = PredicateEvaluator()
            return state.getHand(payingPlayerId).count { cardId ->
                predicateEvaluator.matches(state, state.projectedState, cardId, filter, predicateContext)
            }
        }

        /**
         * Whether [payingPlayerId] can produce [manaCost]. With [waterbend] (Avatar: The Last
         * Airbender) they may also tap untapped artifacts and creatures, each paying {1} — the
         * same eligibility discovery and affordability check the activated-ability / spell
         * waterbend surfaces use.
         *
         * [manaSolver] and [waterbendPermanents] are passed in by callers that already built them
         * (the ward—mana handler needs both anyway to render its decision), so the affordability
         * check costs no extra solve or battlefield scan; callers that only ask the question build
         * them on demand.
         */
        private fun canAffordManaCost(
            state: GameState,
            cardRegistry: CardRegistry,
            payingPlayerId: EntityId,
            manaCost: ManaCost,
            waterbend: Boolean,
            manaSolver: ManaSolver = ManaSolver(cardRegistry),
            waterbendPermanents: List<TapForGenericPermanentData>? = null
        ): Boolean {
            if (manaSolver.canPay(state, payingPlayerId, manaCost)) return true
            if (!waterbend) return false
            val costUtils = costEnumerationUtils(cardRegistry)
            val permanents = waterbendPermanents
                ?: costUtils.findTapForGenericPermanents(state, payingPlayerId, TapForGeneric.WATERBEND)
            return costUtils.canAffordWithTapForGeneric(state, payingPlayerId, manaCost, permanents)
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
            count: Int,
            remainingParts: List<WardCost>,
            wardSourceId: EntityId?,
            controllerId: EntityId?
        ): EffectResult {
            // Can't possibly pay → counter immediately. This also covers Sigarda, Host of Herons:
            // the ward trigger is an ability the warded permanent's controller controls, so a
            // protected caster can't choose to pay a sacrifice cost it imposes.
            if (!canPayWardCost(
                    state, cardRegistry, WardCost.Sacrifice(filter, count), payingPlayerId, controllerId
                )
            ) {
                return counterSpellOrAbility(state, cardRegistry, spellEntityId)
            }

            val validPermanents = sacrificeCandidates(state, payingPlayerId, filter)

            val fodderLabel = filter.description
            val prompt = if (count == 1) {
                "Sacrifice a $fodderLabel or your spell will be countered"
            } else {
                "Sacrifice $count ${fodderLabel}s or your spell will be countered"
            }

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

        /**
         * Ward—Collect evidence N (CR 701.59 — Axebane Ferox's "Ward—Collect evidence 4").
         *
         * Two things separate this from every other ward cost:
         *
         * 1. **The constraint is a sum, not a count.** The player exiles *any number* of cards from
         *    their graveyard whose mana values total at least [amount], and over-paying is legal, so
         *    the prompt is a variable-size selection (min 0 — declining — up to the whole graveyard)
         *    carrying `minTotalManaValue`. `DecisionValidators` enforces that floor on any non-empty
         *    submission, so the resumer only ever sees a decline or a legal collection.
         * 2. **CR 701.59b fails closed.** A graveyard that can't reach [amount] means the controller
         *    *can't choose to collect evidence* at all, so the spell is countered with no prompt
         *    rather than offering a payment they would have to refuse.
         *    [CollectEvidenceResolver.canCollect] is that gate, shared with every other context.
         *
         * When the graveyard totals exactly [amount] there is nothing to choose (every card must go),
         * but the *decision* still matters — declining is a real option for a ward cost, unlike the
         * mandatory [com.wingedsheep.engine.handlers.effects.player.CollectEvidenceExecutor] — so the
         * prompt is raised regardless.
         */
        private fun handleCollectEvidenceCost(
            state: GameState,
            cardRegistry: CardRegistry,
            spellEntityId: EntityId,
            payingPlayerId: EntityId,
            amount: Int,
            remainingParts: List<WardCost>,
            wardSourceId: EntityId?,
            controllerId: EntityId?
        ): EffectResult {
            val candidates = CollectEvidenceResolver.candidates(state, payingPlayerId)
            if (!candidates.canReach(amount)) {
                return counterSpellOrAbility(state, cardRegistry, spellEntityId)
            }

            val decisionResult = DecisionHandler().createCardSelectionDecision(
                state = state,
                playerId = payingPlayerId,
                sourceId = wardSourceId,
                sourceName = "Ward",
                prompt = "Collect evidence $amount (exile cards with total mana value $amount or " +
                    "greater from your graveyard) or your spell will be countered",
                options = candidates.cards,
                minSelections = 0,
                maxSelections = candidates.cards.size,
                ordered = false,
                phase = DecisionPhase.RESOLUTION,
                minTotalManaValue = amount
            )

            val continuation = CounterUnlessCollectEvidenceContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                payingPlayerId = payingPlayerId,
                spellEntityId = spellEntityId,
                amount = amount,
                controllerId = controllerId,
                remainingWardParts = remainingParts,
                wardSourceId = wardSourceId
            )

            return EffectResult.paused(
                decisionResult.state.pushContinuation(continuation),
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
            // Not enough eligible cards in hand → counter immediately.
            if (!canPayWardCost(
                    state, cardRegistry, WardCost.Discard(count, random, filter), payingPlayerId, controllerId
                )
            ) {
                return counterSpellOrAbility(state, cardRegistry, spellEntityId)
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
            waterbend: Boolean,
            remainingParts: List<WardCost>,
            wardSourceId: EntityId?,
            controllerId: EntityId?
        ): EffectResult {
            val manaCost = ManaCost.parse(manaCostString)

            val manaSolver = ManaSolver(cardRegistry)

            // Ward—Waterbend (Avatar: The Last Airbender): the controller may tap their untapped
            // artifacts and creatures to help pay the generic, each paying {1}. Reuse the same
            // eligibility discovery as the activated-ability/spell waterbend surfaces so the rule
            // stays single-sourced. Found once and shared with the affordability check below.
            val waterbendPermanents = if (waterbend) {
                costEnumerationUtils(cardRegistry)
                    .findTapForGenericPermanents(state, payingPlayerId, TapForGeneric.WATERBEND)
            } else {
                emptyList()
            }

            if (!canAffordManaCost(
                    state, cardRegistry, payingPlayerId, manaCost, waterbend,
                    manaSolver, waterbendPermanents
                )
            ) {
                return counterSpellOrAbility(state, cardRegistry, spellEntityId)
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

            val waterbendOptions = waterbendPermanents.map {
                WaterbendPermanentChoice(it.entityId, it.name, it.isCreature)
            }

            val decisionId = java.util.UUID.randomUUID().toString()
            val payPrompt = if (waterbend) {
                "Pay $manaCost for ward (tap artifacts/creatures to help) or your spell will be countered"
            } else {
                "Pay $manaCost for ward or your spell will be countered"
            }
            val decision = SelectManaSourcesDecision(
                id = decisionId,
                playerId = payingPlayerId,
                prompt = payPrompt,
                context = DecisionContext(
                    sourceId = wardSourceId,
                    sourceName = "Ward",
                    phase = DecisionPhase.RESOLUTION
                ),
                availableSources = sourceOptions,
                requiredCost = manaCost.toString(),
                autoPaySuggestion = autoPaySuggestion,
                canDecline = true,
                waterbendPermanents = waterbendOptions
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
                wardSourceId = wardSourceId,
                waterbend = waterbend
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
            // CR 810.9a — life paid as a cost comes out of the team's shared total; the can-pay
            // check reads it through canPayWardCost.
            if (!canPayWardCost(
                    state, cardRegistry, WardCost.Life(lifeCost), payingPlayerId, controllerId
                )
            ) {
                return counterSpellOrAbility(state, cardRegistry, spellEntityId)
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

        /**
         * Build a [CostEnumerationUtils] from a [CardRegistry] — used to reuse the shared
         * waterbend eligibility discovery / affordability check (the same surface activated-ability
         * and spell waterbend use) when a Ward—Waterbend cost is being paid.
         */
        private fun costEnumerationUtils(cardRegistry: CardRegistry) =
            CostEnumerationUtils(
                ManaSolver(cardRegistry),
                CostCalculator(cardRegistry),
                PredicateEvaluator(),
                cardRegistry
            )

        private fun counterSpellOrAbility(
            state: GameState,
            cardRegistry: CardRegistry,
            entityId: EntityId
        ): EffectResult = EffectResult.from(
            StackResolver(cardRegistry = cardRegistry).counterSpellOrAbility(state, entityId)
        )
    }
}
