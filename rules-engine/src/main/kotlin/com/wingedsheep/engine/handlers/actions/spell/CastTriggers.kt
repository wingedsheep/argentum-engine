package com.wingedsheep.engine.handlers.actions.spell

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.event.PendingTrigger
import com.wingedsheep.engine.event.TriggerContext
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.mechanics.mana.GrantedKeywordResolver
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CantBeCounteredComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderRegistryComponent
import com.wingedsheep.engine.state.components.player.GrantedSpellKeywordsComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider
import com.wingedsheep.sdk.scripting.effects.StormCopyEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetRequirement

/** The spell just cast, as the cast triggers and riders see it. */
internal class CastSpellOnStack(
    val action: CastSpell,
    val cardDef: CardDefinition?,
    val cardComponent: CardComponent,
    /** The requirements its targets were chosen against — a copy may choose new ones from them. */
    val targetRequirements: List<TargetRequirement>,
)

/**
 * The abilities that trigger on, and the one-shot riders consumed by, the casting of a spell — the
 * things the cast itself sets off rather than an event the settle boundary detects:
 *
 * - copy triggers: storm (CR 702.40), conspire (CR 702.78), casualty (CR 702.153);
 * - riders carried by the mana that paid (Cavern of Souls, Path of Ancestry, Pyromancer's Goggles,
 *   Carnelian Orb of Dragonkind);
 * - "the next spell you cast" riders — copies (Howl of the Horde), can't-be-countered (Mistrise
 *   Village), affinity (Don & Raph), free cast (World War Hulk I).
 */
internal class CastTriggers(
    private val predicateEvaluator: PredicateEvaluator,
    private val grantedKeywordResolver: GrantedKeywordResolver,
    private val stackResolver: StackResolver,
) {

    /**
     * A triggered ability of the spell itself, put on the stack above it: "when you cast this
     * spell" shapes, sourced by and triggered by the spell.
     */
    private fun selfTrigger(
        state: GameState,
        spell: CastSpellOnStack,
        key: String,
        effect: Effect,
        description: String,
    ): PendingTrigger {
        val action = spell.action
        val ability = TriggeredAbility(
            id = AbilityId("cast_$key"),
            trigger = EventPattern.SpellCastEvent(player = Player.You),
            binding = TriggerBinding.SELF,
            effect = effect,
            activeZones = setOf(Zone.STACK),
            descriptionOverride = description
        )
        val self = state.objectRef(action.cardId)
        return PendingTrigger(
            ability = ability,
            sourceId = action.cardId,
            objectReferences = ObjectReferenceEnvironment(captured = true, origin = self, source = self, triggering = self),
            sourceName = spell.cardComponent.name,
            controllerId = action.playerId,
            triggerContext = TriggerContext(triggeringEntityId = action.cardId, triggeringPlayerId = action.playerId)
        )
    }

    private fun copyEffect(spell: CastSpellOnStack, spellEffect: Effect, copyCount: Int) = StormCopyEffect(
        copyCount = copyCount,
        spellEffect = spellEffect,
        spellTargetRequirements = spell.targetRequirements,
        spellName = spell.cardComponent.name
    )

    /**
     * Storm, conspire and casualty — each "copy this spell" trigger, reusing [StormCopyEffect] so
     * the retargeting, modal-copy, and stack-object clone plumbing is shared. A face-down spell has
     * no abilities (CR 708.2), so it triggers none.
     *
     * Storm triggers once per instance (CR 702.40b): the card's printed keyword, each matching
     * [GrantedSpellKeywordsComponent] grant (Ral's emblem), and each matching
     * `GrantKeywordToOwnSpells` static the caster controls (Prismari, the Inspiration). It triggers
     * even when it copies zero times (CR 702.40a) — the executor is a no-op at copyCount 0, but the
     * trigger must still land on the stack so "whenever an ability triggers" effects see it.
     *
     * Conspire and casualty are reflexive: "When you do, copy it and you may choose new targets for
     * the copy." — present only when their optional additional cost was paid.
     */
    fun copyTriggers(state: GameState, spell: CastSpellOnStack, stormCount: Int): List<PendingTrigger> {
        val action = spell.action
        val cardDef = spell.cardDef
        val spellEffect = cardDef?.script?.spellEffect
        if (action.castFaceDown || spellEffect == null) return emptyList()
        val name = spell.cardComponent.name

        val conspire = if (action.conspiredCreatures.isNotEmpty()) {
            listOf(selfTrigger(state, spell, "conspire", copyEffect(spell, spellEffect, 1), "Conspire — copy $name"))
        } else emptyList()
        val casualty = if (action.casualtyCreature != null) {
            listOf(selfTrigger(state, spell, "casualty", copyEffect(spell, spellEffect, 1), "Casualty — copy $name"))
        } else emptyList()
        val storm = List(stormInstances(state, action, cardDef)) {
            selfTrigger(state, spell, "storm", copyEffect(spell, spellEffect, stormCount), "Storm — copy $name $stormCount time(s)")
        }
        return conspire + casualty + storm
    }

    private fun stormInstances(state: GameState, action: CastSpell, cardDef: CardDefinition): Int {
        val grants = state.getEntity(action.playerId)?.get<GrantedSpellKeywordsComponent>()?.grants ?: emptyList()
        val evalContext = PredicateContext(controllerId = action.playerId)
        val componentGrants = grants.count { grant ->
            grant.keyword == Keyword.STORM &&
                predicateEvaluator.matches(state, state.projectedState, action.cardId, grant.spellFilter, evalContext)
        }
        // Each matching permanent is a separate instance of storm (CR 702.40b), so count them all
        // rather than short-circuiting.
        val staticGrants = grantedKeywordResolver.countGrants(state, action.playerId, cardDef, Keyword.STORM)
        val printed = if (cardDef.hasKeyword(Keyword.STORM)) 1 else 0
        return printed + componentGrants + staticGrants
    }

    // ---------------------------------------------------------------------------------------------
    // Mana riders
    // ---------------------------------------------------------------------------------------------

    /**
     * Applies every rider carried by the mana that paid for this spell. Some riders mutate the spell
     * directly (Cavern's MakesSpellUncounterable stamps a component) while others queue a triggered
     * ability above the spell (Path of Ancestry's conditional scry). A list, not a set: two
     * rider-carrying mana must fire the rider twice.
     */
    fun applyManaRiders(state: GameState, spell: CastSpellOnStack, riders: List<ManaSpellRider>): Pair<GameState, List<PendingTrigger>> {
        var newState = state
        val triggers = mutableListOf<PendingTrigger>()
        for (rider in riders) {
            val (afterRider, riderTriggers) = applyManaRider(newState, spell, rider)
            newState = afterRider
            triggers.addAll(riderTriggers)
        }
        return newState to triggers
    }

    private fun applyManaRider(state: GameState, spell: CastSpellOnStack, rider: ManaSpellRider): Pair<GameState, List<PendingTrigger>> =
        when (rider) {
            is ManaSpellRider.MakesSpellUncounterable ->
                makeUncounterableWhenSpent(state, spell, rider.spellFilter) to emptyList()
            is ManaSpellRider.ScryOnSharedTypeWithCommander ->
                state to scryOnSharedTypeWithCommander(state, spell, rider.amount)
            is ManaSpellRider.CopySpellWhenSpent ->
                state to copySpellWhenSpent(state, spell, rider.spellFilter)
            is ManaSpellRider.GrantsKeywordWhenSpent ->
                grantKeywordWhenSpent(state, spell, rider.keyword, rider.spellFilter, rider.duration) to emptyList()
        }

    private fun spellMatches(state: GameState, action: CastSpell, filter: GameObjectFilter): Boolean =
        predicateEvaluator.matches(state, state.projectedState, action.cardId, filter, PredicateContext(controllerId = action.playerId))

    /**
     * Cavern of Souls' / Boseiju's rider: if the cast spell matches [spellFilter], it can't be
     * countered. Matched at payment time against the spell's cast characteristics, like the other
     * filtered riders — Boseiju's {C} spent on a creature spell leaves that spell counterable.
     */
    private fun makeUncounterableWhenSpent(state: GameState, spell: CastSpellOnStack, spellFilter: GameObjectFilter): GameState {
        if (!spellMatches(state, spell.action, spellFilter)) return state
        return state.updateEntity(spell.action.cardId) { c -> c.with(CantBeCounteredComponent) }
    }

    /**
     * Carnelian Orb of Dragonkind's rider: if the cast spell matches [spellFilter], float an
     * end-of-turn grant of [keyword] keyed to the spell. Otherwise no-op (the mana paid for
     * something else).
     *
     * Unlike the copy / scry riders this queues nothing onto the stack — "it gains haste until end
     * of turn" is a continuous effect the printed card applies without a triggered ability. The
     * grant is keyed to the spell's entity id, which a permanent spell keeps as it resolves onto the
     * battlefield, so the keyword is live the instant the permanent exists — exactly what haste
     * needs.
     *
     * The spell is matched against its stack characteristics at payment time rather than at
     * resolution. That's what the printed rulings require: mana spent on a non-Dragon spell that
     * *becomes* a Dragon later in the turn grants nothing.
     *
     * The floating effect's source is the spell itself, not the mana's producer — the producer may
     * already have left the battlefield, and the source is only read for the effect's display name.
     *
     * A [Duration.Permanent] grant (Hall of the Bandit Lord) is frozen onto the spell's
     * [SpellOnStackComponent.entryKeywordGrants] instead; `PermanentEntry` grants it as the spell
     * becomes a permanent. A floating effect keyed to the stack object would outlive a countered
     * spell and reach the same card if it were later put onto the battlefield.
     */
    private fun grantKeywordWhenSpent(
        state: GameState,
        spell: CastSpellOnStack,
        keyword: String,
        spellFilter: GameObjectFilter,
        duration: Duration,
    ): GameState {
        val action = spell.action
        if (!spellMatches(state, action, spellFilter)) return state
        if (duration == Duration.Permanent) {
            return state.updateEntity(action.cardId) { c ->
                val onStack = c.get<SpellOnStackComponent>() ?: return@updateEntity c
                c.with(onStack.copy(entryKeywordGrants = onStack.entryKeywordGrants + keyword))
            }
        }
        return state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.GrantKeyword(keyword),
            affectedEntities = setOf(action.cardId),
            duration = Duration.EndOfTurn,
            context = EffectContext(sourceId = action.cardId, controllerId = action.playerId)
        )
    }

    /**
     * Pyromancer's Goggles' rider: if the cast spell matches [spellFilter], queue a copy trigger
     * above the spell. Otherwise no-op (the {R} was spent on something else).
     *
     * The trigger resolves *before* the spell it copies, which is the printed behavior — the copy is
     * put onto the stack above the original and resolves first (CR 707.10). The copy's controller
     * may choose new targets, handled by [CopyTargetSpellEffect]'s own retarget pause. Matching
     * reads color/type off the spell's [CardComponent] — projected *battlefield* state doesn't apply
     * to an object on the stack — and happens now, at payment time, not at trigger resolution.
     */
    private fun copySpellWhenSpent(state: GameState, spell: CastSpellOnStack, spellFilter: GameObjectFilter): List<PendingTrigger> {
        if (!spellMatches(state, spell.action, spellFilter)) return emptyList()
        return listOf(
            selfTrigger(
                state, spell, "copy_when_spent",
                CopyTargetSpellEffect(target = EffectTarget.TriggeringEntity),
                "Copy ${spell.cardComponent.name}. You may choose new targets for the copy."
            )
        )
    }

    /**
     * Path of Ancestry's rider: if the cast spell is a creature spell that shares a creature type
     * with any of the controller's commanders, queue a scry trigger above the spell. Otherwise no-op.
     *
     * Subtypes are read from base [CardComponent] for both the spell (it's on the stack, not the
     * battlefield, so projected battlefield state doesn't apply) and for each commander (looked up
     * via [CommanderRegistryComponent]). This matches the printed ruling that the commander's
     * creature types are checked at the moment the mana is spent.
     */
    private fun scryOnSharedTypeWithCommander(state: GameState, spell: CastSpellOnStack, amount: Int): List<PendingTrigger> {
        val cardComponent = spell.cardComponent
        if (!cardComponent.typeLine.isCreature) return emptyList()
        val spellSubtypes = cardComponent.typeLine.subtypes.mapTo(mutableSetOf()) { it.value.lowercase() }
        if (spellSubtypes.isEmpty()) return emptyList()

        val registry = state.getEntity(spell.action.playerId)?.get<CommanderRegistryComponent>() ?: return emptyList()
        val sharesType = registry.commanderIds.any { commanderId ->
            val commanderCard = state.getEntity(commanderId)?.get<CardComponent>() ?: return@any false
            commanderCard.typeLine.subtypes.any { it.value.lowercase() in spellSubtypes }
        }
        if (!sharesType) return emptyList()
        return listOf(selfTrigger(state, spell, "path_of_ancestry_scry", Patterns.Library.scry(amount), "Scry $amount"))
    }

    // ---------------------------------------------------------------------------------------------
    // "The next spell you cast" riders
    // ---------------------------------------------------------------------------------------------

    /**
     * Consumes every "next spell you cast" rider the spell matches. Each rider's filter is evaluated
     * with the rider's *own* source in the context, so a filter that reads a characteristic off the
     * permanent that created it resolves against that permanent at cast time. A copy rider puts its
     * copy trigger on the stack straight away; the returned result is a failure only if that fails.
     */
    fun consumeNextSpellRiders(state: GameState, spell: CastSpellOnStack, events: List<GameEvent>): ExecutionResult {
        val (afterCopies, copyEvents) = when (val copies = consumeCopyRiders(state, spell)) {
            null -> state to emptyList()
            else -> if (copies.outcome !is Outcome.Done) return copies else copies.newState to copies.events
        }
        var newState = afterCopies
        newState = consumeUncounterableRiders(newState, spell)
        newState = consumeAffinityRiders(newState, spell)
        newState = consumeFreeCastRiders(newState, spell)
        return ExecutionResult.success(newState, events + copyEvents)
    }

    /**
     * Pending spell copies (e.g., Howl of the Horde). Each entry carries its own spellFilter
     * (instant or sorcery by default, but e.g. "creature" is expressible). Face-down spells have no
     * characteristics, so they never match. Returns null when nothing matched.
     */
    private fun consumeCopyRiders(state: GameState, spell: CastSpellOnStack): ExecutionResult? {
        val action = spell.action
        if (action.castFaceDown) return null
        val matchingCopies = state.pendingSpellCopies.filter { pending ->
            if (pending.controllerId != action.playerId) return@filter false
            // Loki Laufeyson's "with mana value less than or equal to Loki's power" is
            // `manaValueAtMostDynamic(sourcePower())`, checked as the spell is cast, not when the
            // rider was created. If the source has since left the battlefield,
            // `lastKnownSourceSnapshot` carries what it last was there (stamped at departure by
            // ZoneTransitionService), so the cap reads its last-known power rather than its printed
            // one — CR 608.2h. Null while the source is still in play, which is the common case.
            val copyEvalContext = PredicateContext(
                controllerId = action.playerId,
                sourceId = pending.sourceId,
                lastKnownSourceSnapshot = pending.lastKnownSourceSnapshot
            )
            predicateEvaluator.matches(state, state.projectedState, action.cardId, pending.spellFilter, copyEvalContext)
        }
        if (matchingCopies.isEmpty()) return null
        val totalCopies = matchingCopies.sumOf { it.copies }
        // Remove consumed pending copies (keep persistent ones like The Mirari Conjecture Ch. III,
        // and any non-matching entries waiting for a different spell type).
        val newState = state.copy(
            pendingSpellCopies = state.pendingSpellCopies.filter { pending -> pending.persistent || pending !in matchingCopies }
        )
        val spellEffect = spell.cardDef?.script?.spellEffect
        if (spellEffect == null || totalCopies <= 0) return ExecutionResult.success(newState)

        // sourceId must point to the spell being copied (action.cardId), not the originating
        // permanent (e.g., Howl of the Horde). StormCopyEffectExecutor uses sourceId to clone the
        // SpellOnStackComponent via putSpellCopy; the originating permanent may be in the graveyard
        // by the time the trigger resolves.
        val self = newState.objectRef(action.cardId)
        val copyAbility = TriggeredAbilityOnStackComponent(
            sourceId = action.cardId,
            objectReferences = ObjectReferenceEnvironment(captured = true, origin = self, source = self),
            sourceName = spell.cardComponent.name,
            controllerId = action.playerId,
            effect = copyEffect(spell, spellEffect, totalCopies),
            description = "Copy ${spell.cardComponent.name} $totalCopies time(s)"
        )
        return stackResolver.putTriggeredAbility(newState, copyAbility)
    }

    /**
     * "Next spell can't be countered" riders (e.g., Mistrise Village). The first matching cast
     * stamps the spell uncounterable and consumes every matching entry; later spells aren't
     * protected. Unlike the copy rider, face-down spells aren't excluded — a face-down spell is
     * still "the next spell you cast", and the default Any filter matches it.
     */
    private fun consumeUncounterableRiders(state: GameState, spell: CastSpellOnStack): GameState {
        val matching = state.pendingUncounterableSpells.filter { spellMatchesRider(state, spell, it.controllerId, it.sourceId, it.spellFilter) }
        if (matching.isEmpty()) return state
        return state
            .copy(pendingUncounterableSpells = state.pendingUncounterableSpells.filter { it !in matching })
            .updateEntity(spell.action.cardId) { c -> c.with(CantBeCounteredComponent) }
    }

    /**
     * "Next spell has affinity for X" riders (Don & Raph). The cost reduction was already applied
     * by the cost calculator while these riders were present; consuming them here means only the
     * *next* matching spell is affected.
     */
    private fun consumeAffinityRiders(state: GameState, spell: CastSpellOnStack): GameState {
        val matching = state.pendingNextSpellAffinities.filter { spellMatchesRider(state, spell, it.controllerId, it.sourceId, it.spellFilter) }
        if (matching.isEmpty()) return state
        return state.copy(pendingNextSpellAffinities = state.pendingNextSpellAffinities.filter { it !in matching })
    }

    /**
     * "The next matching spell you cast this turn can be cast without paying its mana cost" riders
     * (World War Hulk I). The permission was already offered while the rider was present. Not gated
     * on `useWithoutPayingManaCost`: the printed text names a spell ("the next red or green creature
     * spell you cast this turn"), so a matching spell cast for full price is that spell and spends
     * the rider — the same contract as the affinity rider.
     */
    private fun consumeFreeCastRiders(state: GameState, spell: CastSpellOnStack): GameState {
        val matching = state.pendingFreeCastSpells.filter { spellMatchesRider(state, spell, it.controllerId, it.sourceId, it.spellFilter) }
        if (matching.isEmpty()) return state
        return state.copy(pendingFreeCastSpells = state.pendingFreeCastSpells.filter { it !in matching })
    }

    /** The rider's own sourceId goes into the context so `EffectTarget.Self` resolves to it. */
    private fun spellMatchesRider(
        state: GameState,
        spell: CastSpellOnStack,
        riderController: com.wingedsheep.sdk.model.EntityId,
        riderSource: com.wingedsheep.sdk.model.EntityId?,
        spellFilter: GameObjectFilter,
    ): Boolean {
        val action = spell.action
        if (riderController != action.playerId) return false
        val context = PredicateContext(controllerId = action.playerId, sourceId = riderSource)
        return predicateEvaluator.matches(state, state.projectedState, action.cardId, spellFilter, context)
    }
}
