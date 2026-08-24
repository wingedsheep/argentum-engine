package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.OptionalRedirectEffectContinuation
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import java.util.UUID

/**
 * The "**you may**" half of an optional damage-redirection shield (Blood of the Martyr:
 * "Until end of turn, if damage would be dealt to any creature, you may have that damage dealt to
 * you instead.").
 *
 * ## Why a pre-pass
 * Damage is applied by pure code deep inside loops — a whole combat damage step is one simultaneous
 * batch (CR 510.2), and `DamageUtils.dealDamageToTarget` has no decision point inside it. So the
 * choice is made *before* any of the damage is dealt, which is where the rules put it too: a
 * replacement effect applies as the event *would* happen rather than after the fact (CR 614.1), so
 * the whole simultaneous batch is settled question by question before any of it is dealt.
 *
 * A caller about to deal damage hands [check] every [Instance] it is about to deal. If any of them is
 * covered by an optional shield and hasn't been answered yet, it gets back a [Check.Ask] holding a
 * yes/no decision to raise; once the answer is recorded the caller re-runs and asks about the next
 * instance, until [check] answers [Check.Ready] and the damage is dealt with every answer already in
 * [GameState.optionalDamageRedirectChoices].
 *
 * ## Per instance, per shield
 * The key is shield + source + recipient, so a sweeper that hits four creatures asks four times: the
 * shield's controller can soak the damage aimed at their own blocker and let the damage aimed at an
 * opponent's creature through. That granularity is the point of the card — the shield covers *every*
 * creature, not just the controller's. When two optional shields cover the same instance, declining
 * the first offers the second, in the order the damage pipeline consults them (CR 616.1).
 *
 * ## Declined by default
 * [DamageUtils.checkDamageRedirection] treats an unanswered instance as declined. A damage path that
 * doesn't run this pre-pass therefore deals its damage normally instead of silently redirecting it
 * onto a player who was never asked.
 */
object OptionalDamageRedirect {

    /** One instance of damage a caller is about to deal: one source, one recipient. */
    data class Instance(
        val sourceId: EntityId?,
        val targetId: EntityId,
        val amount: Int
    )

    /**
     * The outcome of asking whether the damage a caller is about to deal still needs a choice.
     *
     * Both arms carry a state: [Ask] has the decision pending on it, [Ready] is the caller's own
     * state with any stale answers pruned off. Callers must continue from the state they get back,
     * never from the one they passed in.
     */
    sealed interface Check {
        /** One instance still needs an answer. Raise [decision]; record it under [choiceKey]; re-run. */
        data class Ask(
            val state: GameState,
            val decision: YesNoDecision,
            val choiceKey: String
        ) : Check

        /** Every instance an optional shield covers has been answered — deal the damage. */
        data class Ready(val state: GameState) : Check
    }

    /**
     * Identity of one (shield, damage instance) pair. The amount is deliberately not part of it:
     * prevention shields and damage-modifying effects can still change the amount between the
     * question and the damage, and the answer is about the instance, not its size.
     */
    fun choiceKey(shieldId: EntityId, sourceId: EntityId?, targetId: EntityId): String =
        "$shieldId|${sourceId ?: "none"}|$targetId"

    /**
     * Every optional redirection shield that covers [targetId], in the order
     * [DamageUtils.checkDamageRedirection] would consider them. Mandatory shields are invisible
     * here — they are applied without asking anybody.
     */
    fun optionalShieldsFor(state: GameState, targetId: EntityId): List<ActiveFloatingEffect> =
        state.floatingEffects.filter { floating ->
            val mod = floating.effect.modification
            mod is SerializableModification.RedirectNextDamage &&
                mod.optional &&
                redirectShieldCovers(state, floating, mod, targetId)
        }

    /**
     * Whether a redirection shield applies to damage about to be dealt to [targetId].
     *
     * A `creaturesOnly` shield protects a *class* and is read off projected state, so a creature that
     * arrived after the shield was created is covered, an animated land counts while it is one, and a
     * player never is. Any other shield protects the entities it was created with (an empty list
     * meaning "anything").
     */
    fun redirectShieldCovers(
        state: GameState,
        floating: ActiveFloatingEffect,
        mod: SerializableModification.RedirectNextDamage,
        targetId: EntityId
    ): Boolean = if (mod.creaturesOnly) {
        state.projectedState.isCreature(targetId)
    } else {
        floating.effect.affectedEntities.isEmpty() || targetId in floating.effect.affectedEntities
    }

    /**
     * Check the damage a caller is about to deal against the optional shields in play.
     *
     * Answers left over from an earlier damage event are pruned here — a "yes" recorded for damage
     * that a shield counter ended up eating must not be reused for the next event that happens to
     * pair the same source with the same recipient.
     */
    fun check(state: GameState, instances: List<Instance>): Check {
        // Every (instance, covering shield) pair this event could raise, in the order the damage
        // pipeline will consider them. Their keys are what the answers are filed under, so they are
        // also what keeps an answer from being pruned as stale mid-event.
        val pairs = instances
            .filter { it.amount > 0 }
            .flatMap { instance ->
                optionalShieldsFor(state, instance.targetId).map { shield -> shield to instance }
            }
        val pruned = prune(
            state,
            pairs.map { (shield, instance) -> choiceKey(shield.id, instance.sourceId, instance.targetId) }.toSet()
        )

        // The first pair still to be answered. A shield already answered "yes" settles its whole
        // instance (that damage is spoken for); a "no" falls through to the next shield covering the
        // same instance, exactly as the pipeline does.
        val settledInstances = pairs
            .filter { (shield, instance) ->
                pruned.optionalDamageRedirectChoices[choiceKey(shield.id, instance.sourceId, instance.targetId)] == true
            }
            .map { (_, instance) -> instance }
            .toSet()
        val unanswered = pairs.firstOrNull { (shield, instance) ->
            instance !in settledInstances &&
                choiceKey(shield.id, instance.sourceId, instance.targetId) !in pruned.optionalDamageRedirectChoices
        } ?: return Check.Ready(pruned)

        val (shield, instance) = unanswered
        val key = choiceKey(shield.id, instance.sourceId, instance.targetId)
        val shieldName = shield.sourceName ?: "Damage redirection"
        val recipientName = pruned.getEntity(instance.targetId)?.get<CardComponent>()?.name ?: "that creature"
        val sourceName = instance.sourceId
            ?.let { pruned.getEntity(it)?.get<CardComponent>()?.name }
            ?: "A source"
        val redirectToId = (shield.effect.modification as SerializableModification.RedirectNextDamage).redirectToId
        val redirectName = if (redirectToId == shield.controllerId) {
            "you"
        } else {
            pruned.getEntity(redirectToId)?.get<CardComponent>()?.name ?: "the redirection target"
        }

        val decision = YesNoDecision(
            id = UUID.randomUUID().toString(),
            playerId = shield.controllerId,
            prompt = "$sourceName would deal ${instance.amount} damage to $recipientName — " +
                "have that damage dealt to $redirectName instead?",
            context = DecisionContext(
                sourceId = shield.sourceId,
                sourceName = shieldName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = "Redirect it",
            noText = "Let it through"
        )
        return Check.Ask(pruned.withPendingDecision(decision), decision, key)
    }

    /**
     * Executor-side shorthand for [check]: returns the state to deal damage from, plus a paused
     * [EffectResult] to return immediately when a question is still outstanding.
     *
     * The paused result carries an [OptionalRedirectEffectContinuation] that records the answer and
     * re-runs [effect] in [context], so the executor keeps being re-entered — asking about one more
     * instance each time — until every instance is settled and it deals its damage in one go.
     *
     * ```
     * val (readyState, pause) = OptionalDamageRedirect.beforeDealing(state, instances, effect, context)
     * if (pause != null) return pause
     * // …deal the damage from readyState…
     * ```
     */
    fun beforeDealing(
        state: GameState,
        instances: List<Instance>,
        effect: Effect,
        context: EffectContext
    ): Pair<GameState, EffectResult?> = when (val check = check(state, instances)) {
        is Check.Ready -> check.state to null
        is Check.Ask -> {
            val paused = check.state.pushContinuation(
                OptionalRedirectEffectContinuation(
                    decisionId = check.decision.id,
                    choiceKey = check.choiceKey,
                    effect = effect,
                    effectContext = context
                )
            )
            check.state to EffectResult.paused(paused, check.decision)
        }
    }

    /** Record an answer so the re-run deals the damage instead of asking again. */
    fun record(state: GameState, choiceKey: String, choice: Boolean): GameState =
        state.copy(optionalDamageRedirectChoices = state.optionalDamageRedirectChoices + (choiceKey to choice))

    /**
     * Read and consume the answer for one damage instance. Returns the state without that answer and
     * the answer itself — null when the instance was never asked about, which reads as declined.
     */
    fun consume(state: GameState, choiceKey: String): Pair<GameState, Boolean?> {
        val choice = state.optionalDamageRedirectChoices[choiceKey] ?: return state to null
        return state.copy(
            optionalDamageRedirectChoices = state.optionalDamageRedirectChoices - choiceKey
        ) to choice
    }

    /** Drop answers that belong to no instance of the event about to be dealt. */
    private fun prune(state: GameState, liveKeys: Set<String>): GameState {
        if (state.optionalDamageRedirectChoices.isEmpty()) return state
        val kept = state.optionalDamageRedirectChoices.filterKeys { it in liveKeys }
        if (kept.size == state.optionalDamageRedirectChoices.size) return state
        return state.copy(optionalDamageRedirectChoices = kept)
    }
}
