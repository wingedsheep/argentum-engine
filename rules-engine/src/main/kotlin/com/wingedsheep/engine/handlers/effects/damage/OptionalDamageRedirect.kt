package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.OptionalRedirectEffectContinuation
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageUnpreventableThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect

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
     * Both arms carry the caller's state with stale answers pruned off; [Ask] additionally
     * describes the question that the caller must suspend for. Callers must continue from the state they get back,
     * never from the one they passed in.
     */
    sealed interface Check {
        /** One instance still needs an answer. Suspend for [question]; record it under [choiceKey]; re-run. */
        data class Ask(
            val state: GameState,
            val question: (String) -> YesNoDecision,
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
    fun optionalShieldsFor(
        state: GameState,
        targetId: EntityId,
        predicateEvaluator: PredicateEvaluator
    ): List<ActiveFloatingEffect> =
        state.floatingEffects.filter { floating ->
            val mod = floating.effect.modification
            mod is SerializableModification.RedirectNextDamage &&
                mod.optional &&
                redirectShieldCovers(state, floating, mod, targetId, predicateEvaluator)
        }

    /**
     * Whether a redirection shield applies to damage about to be dealt to [targetId].
     *
     * A filter shield protects a *class* and is read off projected state from its controller's side,
     * so a creature that arrived after the shield was created is covered, an animated land counts
     * while it is one, a player only when the shield lists it, and never the shield's own recipient — "all damage
     * that would be dealt to creatures you control is dealt to that creature instead" leaves the
     * damage already headed for that creature where it is. Any other shield protects the entities
     * it was created with (an empty list meaning "anything").
     *
     * A shield whose **recipient** has since left the battlefield covers nothing: there is no
     * object left for the damage to be dealt to "instead", so the original recipient takes it
     * (CR 614.9; Razia, Boros Archangel's ruling "if either target creature leaves the battlefield
     * before damage is dealt, that damage won't be redirected"). A player recipient never leaves.
     */
    fun redirectShieldCovers(
        state: GameState,
        floating: ActiveFloatingEffect,
        mod: SerializableModification.RedirectNextDamage,
        targetId: EntityId,
        predicateEvaluator: PredicateEvaluator
    ): Boolean {
        val projected = state.projectedState
        fun validRecipient(id: EntityId): Boolean = id in state.turnOrder ||
            (id in state.getBattlefield() && (projected.isCreature(id) || projected.isPlaneswalker(id) || projected.isBattle(id)))
        if (mod.chosenSource != null) {
            // Damage already headed for the shield's own recipient has nowhere else to go. Without
            // this an every-recipient shield (Reverberation) would redirect its redirected damage again.
            if (targetId == mod.redirectToId) return false
            if (mod.protectedRef != null && !state.isCurrentObject(mod.protectedRef)) return false
            if (!validRecipient(targetId)) return false
        }
        // The recipient as an object: one that left and came back is a new object (CR 400.7).
        if (mod.redirectToRef != null && !state.isCurrentObject(mod.redirectToRef)) return false
        if (state.getEntity(mod.redirectToId) == null) return false
        // CR 614.9: damage isn't redirected to a permanent that has left the battlefield or is no
        // longer a battle, creature or planeswalker — Gideon's Sacrifice's "(if it's still a creature
        // or planeswalker)", and every other redirection shield's unprinted equivalent.
        if (!validRecipient(mod.redirectToId)) return false
        val filter = mod.protectedFilter
        return if (filter != null) {
            // The listed targets and the filtered class together: Gideon's Sacrifice's "you and
            // permanents you control" lists the player and filters the permanents.
            targetId != mod.redirectToId && (targetId in floating.effect.affectedEntities ||
                targetId in state.getBattlefield() && predicateEvaluator.matches(
                    state, projected, targetId, filter,
                    PredicateContext(controllerId = floating.controllerId, sourceId = floating.sourceId)
                ))
        } else {
            floating.effect.affectedEntities.isEmpty() || targetId in floating.effect.affectedEntities
        }
    }

    /**
     * Check the damage a caller is about to deal against the optional shields in play, and against
     * mandatory shields that could chain it to more than one recipient (the affected player then
     * picks which, CR 616.1e).
     *
     * Answers left over from an earlier damage event are pruned here — a "yes" recorded for damage
     * that a shield counter ended up eating must not be reused for the next event that happens to
     * pair the same source with the same recipient.
     */
    fun check(state: GameState, instances: List<Instance>, predicateEvaluator: PredicateEvaluator): Check {
        // Every (instance, covering shield) pair this event could raise, in the order the damage
        // pipeline will consider them. Their keys are what the answers are filed under, so they are
        // also what keeps an answer from being pruned as stale mid-event.
        val pairs = instances
            .filter { it.amount > 0 }
            .flatMap { instance ->
                optionalShieldsFor(state, instance.targetId, predicateEvaluator).map { shield -> shield to instance }
            }
        // Instances whose mandatory shields can chain to more than one final recipient (two Gideon's
        // Sacrifices that chose different permanents): the affected player picks the order (CR 616.1e).
        val routeChoices = instances
            .filter { it.amount > 0 }
            // A recipient whose damage can't be redirected away (Whippoorwill) has no route to pick.
            .filter { state.getEntity(it.targetId)?.has<DamageUnpreventableThisTurnComponent>() != true }
            .mapNotNull { instance ->
                val routes = routesFor(state, instance, predicateEvaluator)
                if (routes.size < 2) null else instance to routes
            }
        val pruned = prune(
            state,
            pairs.map { (shield, instance) -> choiceKey(shield.id, instance.sourceId, instance.targetId) }.toSet() +
                routeChoices.flatMap { (instance, routes) -> routeKeys(instance, routes) }
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
        } ?: return chooseRoutes(pruned, routeChoices)

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

        val question = { decisionId: String -> YesNoDecision(
            id = decisionId,
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
        ) }
        return Check.Ask(pruned, question, key)
    }

    /**
     * One way a damage instance can end up after its mandatory redirection shields have applied:
     * the permanent or player finally dealt the damage, and each (shield, recipient-at-that-point)
     * hop that got it there.
     */
    private data class Route(val recipient: EntityId, val hops: List<Pair<EntityId, EntityId>>)

    /**
     * Every distinct final recipient [instance] can reach through the mandatory, non-source-keyed
     * redirection shields in play — one route per recipient, the first in timestamp order kept.
     * After the chosen shield applies, the others that now apply to the modified event are
     * considered again (CR 616.1f), each at most once (CR 614.5), so this branches over every
     * covering shield at every hop. Shields are few, so the search stays tiny.
     */
    private fun routesFor(state: GameState, instance: Instance, predicateEvaluator: PredicateEvaluator): List<Route> {
        fun walk(targetId: EntityId, applied: Set<EntityId>): List<Route> {
            val covering = state.floatingEffects.filter { floating ->
                val mod = floating.effect.modification
                mod is SerializableModification.RedirectNextDamage && !mod.optional && mod.chosenSource == null &&
                    floating.id !in applied && redirectShieldCovers(state, floating, mod, targetId, predicateEvaluator)
            }
            if (covering.isEmpty()) return listOf(Route(targetId, emptyList()))
            return covering.flatMap { shield ->
                val to = (shield.effect.modification as SerializableModification.RedirectNextDamage).redirectToId
                walk(to, applied + shield.id).map { it.copy(hops = listOf(shield.id to targetId) + it.hops) }
            }
        }
        if (state.floatingEffects.none { it.effect.modification is SerializableModification.RedirectNextDamage }) return emptyList()
        // An optional ("you may") or source-keyed shield on the same damage answers through its own
        // path; a route chosen here would override it, so such an instance keeps timestamp order.
        val otherKindCovers = state.floatingEffects.any { floating ->
            val mod = floating.effect.modification
            mod is SerializableModification.RedirectNextDamage && (mod.optional || mod.chosenSource != null) &&
                redirectShieldCovers(state, floating, mod, instance.targetId, predicateEvaluator)
        }
        if (otherKindCovers) return emptyList()
        return walk(instance.targetId, emptySet()).distinctBy { it.recipient }
    }

    private fun routeKey(instance: Instance, recipient: EntityId): String =
        "route|${instance.sourceId ?: "none"}|${instance.targetId}|$recipient"

    /** The answers and hop marks a route choice files under — live for as long as the instance is. */
    private fun routeKeys(instance: Instance, routes: List<Route>): List<String> =
        routes.dropLast(1).map { routeKey(instance, it.recipient) } +
            routes.flatMap { route -> route.hops.map { (shield, at) -> choiceKey(shield, instance.sourceId, at) } }

    /**
     * Settle each multi-route instance: ask the affected player about the routes one at a time ("deal
     * it to A?" — the last one needs no question), then mark the chosen route's hops so
     * [DamageUtils.checkDamageRedirection] applies exactly those shields, in that order.
     */
    private fun chooseRoutes(state: GameState, routeChoices: List<Pair<Instance, List<Route>>>): Check {
        val working = state
        val spent = mutableSetOf<String>()
        val hops = mutableListOf<Pair<String, Boolean>>()
        for ((instance, routes) in routeChoices) {
            val answers = working.optionalDamageRedirectChoices
            var chosen: Route? = null
            for ((index, route) in routes.withIndex()) {
                if (index == routes.lastIndex) { chosen = route; break }
                when (answers[routeKey(instance, route.recipient)]) {
                    true -> { chosen = route; break }
                    false -> continue
                    null -> return Check.Ask(working, routeQuestion(working, instance, routes.drop(index)), routeKey(instance, route.recipient))
                }
            }
            val route = chosen ?: continue
            // The answers are spent once the route is settled: a later event with the same source,
            // recipient and shields (a double striker's second hit) asks again. The chosen route's
            // hops carry the decision into the damage pipeline, which consumes them as it applies.
            // Stripped only once every instance is settled — an Ask for a later instance must keep
            // this one's answer.
            spent += routeKeys(instance, routes)
            hops += route.hops.map { (shield, at) -> choiceKey(shield, instance.sourceId, at) to true }
        }
        return Check.Ready(working.copy(optionalDamageRedirectChoices = working.optionalDamageRedirectChoices - spent + hops))
    }

    /** "Which permanent is dealt it?" as a yes/no over the first of [remaining], buttons named by recipient. */
    private fun routeQuestion(state: GameState, instance: Instance, remaining: List<Route>): (String) -> YesNoDecision {
        val decider = if (instance.targetId in state.turnOrder) instance.targetId
            else state.projectedState.getController(instance.targetId) ?: instance.targetId
        fun nameOf(id: EntityId): String =
            if (id == decider) "you" else state.getEntity(id)?.get<CardComponent>()?.name ?: "that permanent"
        val sourceName = instance.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "A source"
        val shieldSource = state.floatingEffects.firstOrNull { it.id == remaining.first().hops.firstOrNull()?.first }
        val first = nameOf(remaining.first().recipient)
        return { decisionId -> YesNoDecision(
            id = decisionId,
            playerId = decider,
            prompt = "$sourceName would deal ${instance.amount} damage to ${nameOf(instance.targetId)} — " +
                "which one is dealt it instead?",
            context = DecisionContext(
                sourceId = shieldSource?.sourceId,
                sourceName = shieldSource?.sourceName ?: "Damage redirection",
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = first.replaceFirstChar { it.uppercase() },
            noText = if (remaining.size == 2) nameOf(remaining[1].recipient).replaceFirstChar { it.uppercase() } else "Somewhere else"
        ) }
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
     * val (readyState, pause) = OptionalDamageRedirect.beforeDealing(state, instances, effect, context, predicateEvaluator)
     * if (pause != null) return pause
     * // …deal the damage from readyState…
     * ```
     */
    fun beforeDealing(
        state: GameState,
        instances: List<Instance>,
        effect: Effect,
        context: EffectContext,
        predicateEvaluator: PredicateEvaluator
    ): Pair<GameState, EffectResult?> = when (val check = check(state, instances, predicateEvaluator)) {
        is Check.Ready -> check.state to null
        is Check.Ask -> {
            val paused = check.state.suspendForDecision(
                check.question,
                OptionalRedirectEffectContinuation(
                    choiceKey = check.choiceKey,
                    effect = effect,
                    effectContext = context
                )
            )
            paused.state to EffectResult.from(paused)
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
