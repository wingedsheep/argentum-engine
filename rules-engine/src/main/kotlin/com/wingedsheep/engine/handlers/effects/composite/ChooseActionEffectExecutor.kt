package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.engine.mechanics.CompelledActionImmunity
import kotlin.reflect.KClass

/**
 * Executor for [ChooseActionEffect].
 *
 * Presents a player with labeled options and executes the chosen effect.
 * Infeasible options are filtered out. If at most one could be feasible as far as every player
 * can tell, there's no question: that one auto-executes when it really is feasible, and otherwise
 * nothing happens. When two or more could be, the question is asked, offering only the options
 * that really are feasible, so hidden cards never decide whether it is asked (#3340).
 */
class ChooseActionEffectExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
    cardRegistry: CardRegistry,
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<ChooseActionEffect> {

    private val manaSolver = ManaSolver(cardRegistry, predicateEvaluator)

    override val effectType: KClass<ChooseActionEffect> = ChooseActionEffect::class

    override fun execute(
        state: GameState,
        effect: ChooseActionEffect,
        context: EffectContext
    ): EffectResult {
        // Resolve who makes the choice. State-aware so relational references such as
        // EffectTarget.TargetController (the controller of the chosen permanent) resolve — used by
        // "[do X to a permanent] unless its controller [accepts an avoidance]" choices routed to the
        // targeted permanent's controller rather than the ability's controller.
        val choosingPlayerId = context.resolvePlayerTarget(effect.player, state)
            ?: return EffectResult.error(state, "Could not resolve player for ChooseActionEffect")

        // Filter to feasible choices
        // Inside a counter-kind loop, `{counter}` in a label names the kind being visited
        // ("Put another {counter} counter on it" — Dramatist's Puppet), so each per-kind
        // prompt says which kind it is about.
        val labelled = effect.choices.map { choice ->
            context.chosenCounterType?.let { kind -> choice.copy(label = choice.label.replace("{counter}", kind.printed)) }
                ?: choice
        }
        // Whether to ask reads only what every player can tell (#3340); which options the
        // question offers reads the truth. So "sacrifice an artifact or discard a nonland card"
        // with an artifact and a hand of lands is asked, offering only the sacrifice, a forced
        // pick answered without a prompt.
        // An opponent's "sacrifice or discard or …" can't be answered by an action the chooser is
        // protected from (Tamiyo, Collector of Tales) — that option isn't offered. Public.
        val cause = context.effectControllerId ?: context.controllerId
        val allowed = labelled.filterNot { choice ->
            val action = CompelledActionImmunity.actionCompelledBy(choice.effect) ?: return@filterNot false
            CompelledActionImmunity.appliesTo(state, choosingPlayerId, cause, action, predicateEvaluator)
        }
        val publiclyFeasible = allowed.filter { choice ->
            checkFeasibility(state, choosingPlayerId, choice.feasibilityCheck, predicateEvaluator, manaSolver, publicOnly = true)
        }
        val feasibleChoices = publiclyFeasible.filter { choice ->
            isFeasible(state, choosingPlayerId, choice.feasibilityCheck)
        }

        if (publiclyFeasible.size <= 1) {
            // No question either way. A lone option the hidden cards rule out does nothing, as an
            // impossible instruction does.
            val only = feasibleChoices.singleOrNull() ?: return EffectResult.success(state)
            return effectExecutor(state, only.effect, context)
        }

        // Asked, though hidden cards rule every option out: the one answer does nothing.
        val offered = feasibleChoices.ifEmpty { listOf(EffectChoice(NOTHING_POSSIBLE, Effects.Nothing)) }

        // Present options to the choosing player
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val decision = { decisionId: String -> ChooseOptionDecision(
            id = decisionId,
            playerId = choosingPlayerId,
            prompt = "Choose one for ${sourceName ?: "ability"}",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = offered.map { it.label }
        ) }

        val continuation = ChooseActionContinuation(
            choosingPlayerId = choosingPlayerId,
            sourceName = sourceName,
            choices = offered,
            context = context
        )

        return EffectResult.from(state.suspendForDecision(decision, continuation))
    }

    private fun isFeasible(
        state: GameState,
        playerId: com.wingedsheep.sdk.model.EntityId,
        check: FeasibilityCheck?
    ): Boolean = checkFeasibility(state, playerId, check, predicateEvaluator, manaSolver)
}

/**
 * Check whether a [FeasibilityCheck] is satisfied for the given player.
 * Shared by [ChooseActionEffectExecutor], [GatedEffectExecutor], and [ReflexiveTriggerEffectExecutor].
 *
 * With [publicOnly], the answer is whether the check *might* hold as far as every player can tell,
 * which is what decides whether a question is asked at all (#3340). The only difference is a
 * filtered count of a hidden zone ("a nonland card in your hand"): which cards match is hidden, so
 * only the zone's size is read. Without it, the answer is the truth, which decides what the
 * question offers. Every `false` in public mode is also `false` in full mode.
 *
 * [FeasibilityCheck.CanPayMana] reads [ManaSolver.canPay] either way. That is #3340's group C.
 */
internal fun checkFeasibility(
    state: GameState,
    playerId: com.wingedsheep.sdk.model.EntityId,
    check: FeasibilityCheck?,
    predicateEvaluator: PredicateEvaluator,
    manaSolver: ManaSolver,
    publicOnly: Boolean = false
): Boolean {
    if (check == null) return true

    return when (check) {
        is FeasibilityCheck.HasCardsInZone if publicOnly && check.zone.isHidden ->
            state.getZone(ZoneKey(playerId, check.zone)).size >= check.count
        is FeasibilityCheck.ControlsPermanentMatching -> {
            val matching = BattlefieldFilterUtils.findMatchingOnBattlefield(
                state,
                check.filter.youControl(),
                PredicateContext(controllerId = playerId),
                predicateEvaluator = predicateEvaluator
            )
            matching.size >= check.count
        }
        is FeasibilityCheck.HasCardsInZone -> {
            val zoneKey = ZoneKey(playerId, check.zone)
            val cards = state.getZone(zoneKey)
            if (check.filter == com.wingedsheep.sdk.scripting.GameObjectFilter.Any) {
                cards.size >= check.count
            } else {
                val context = PredicateContext(controllerId = playerId)
                cards.count { cardId ->
                    predicateEvaluator.matches(state, state.projectedState, cardId, check.filter, context)
                } >= check.count
            }
        }
        is FeasibilityCheck.CanPayMana -> manaSolver.canPay(state, playerId, check.cost)
    }
}

/** The one answer to a choose-one question whose every option hidden cards rule out. */
internal const val NOTHING_POSSIBLE = "None of these is possible"
