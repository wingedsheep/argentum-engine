package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LandTappedForManaEvent
import com.wingedsheep.engine.core.ManaAddedEvent
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.chosenColor
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.AdditionalSourceTriggers
import com.wingedsheep.sdk.scripting.DampLandManaProduction
import com.wingedsheep.sdk.scripting.TappedForManaType

/**
 * Everything that happens *after* a mana ability's own effect has resolved: the Damping Sphere
 * replacement, the triggered mana abilities that key off the tap, the "a land was tapped for mana"
 * event, and the any-color tap bonuses.
 *
 * There are two ways into this. A mana ability whose produced color is already known resolves in
 * one step inside `ActivateAbilityHandler`. One that must ask ("{T}: Add one mana of any color"
 * activated without a `manaColorChoice` — the path the AI and the gym always take) pauses for a
 * color decision and finishes inside
 * [com.wingedsheep.engine.handlers.continuations.ColorChoiceContinuationResumer]. Both call the
 * same two entry points here, in the same order, so the pausing path is not a lossy subset of the
 * synchronous one — Damping Sphere, Fertile Ground, Overabundance's damage rider and Twinflame
 * Travelers' doubling all apply either way.
 */
class ManaAbilityResolutionPipeline(
    private val cardRegistry: CardRegistry,
    private val conditionEvaluator: ConditionEvaluator,
    private val effectExecutorRegistry: EffectExecutorRegistry,
    private val predicateEvaluator: PredicateEvaluator = PredicateEvaluator(),
    private val dynamicAmountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
) {

    private val tappedForManaBonusResolver =
        TappedForManaBonusResolver(cardRegistry, dynamicAmountEvaluator)

    /** The state after [applyLandManaDampening], and whether the replacement actually applied. */
    data class Dampening(val state: GameState, val dampened: Boolean)

    private data class AdditionalManaResult(
        val state: GameState,
        val events: List<GameEvent>
    )

    /**
     * Damping Sphere: "If a land is tapped for two or more mana, it produces {C} instead of any
     * other type and amount." Compares the tapper's pool across the mana ability's resolution
     * ([stateBeforeEffect] → [state]) rather than reading the effect, so it catches every shape of
     * multi-mana land — including one whose amount was only decided at resolution.
     *
     * The caller is responsible for reporting the replacement: when [Dampening.dampened] is true
     * the produced mana is one colorless, whatever the ability said it would be.
     */
    fun applyLandManaDampening(
        stateBeforeEffect: GameState,
        state: GameState,
        sourceCard: CardComponent?,
        tapperId: EntityId,
    ): Dampening {
        if (sourceCard?.typeLine?.isLand != true) return Dampening(state, false)
        if (!hasDampLandManaProduction(state)) return Dampening(state, false)

        val oldPool = stateBeforeEffect.getEntity(tapperId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        val newPool = state.getEntity(tapperId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        val totalManaProduced = (newPool.white - oldPool.white) +
            (newPool.blue - oldPool.blue) +
            (newPool.black - oldPool.black) +
            (newPool.red - oldPool.red) +
            (newPool.green - oldPool.green) +
            (newPool.colorless - oldPool.colorless)
        if (totalManaProduced < 2) return Dampening(state, false)

        // Replace with 1 colorless mana: revert to old pool + 1 colorless.
        // Restricted mana and mana-source provenance the player had floating before this
        // activation are preserved — Damping Sphere only replaces what the land just
        // produced, not what was already in the pool. The replacement colorless carries no
        // provenance (it comes from the replacement effect, not the land).
        val dampenedPool = ManaPoolComponent(
            white = oldPool.white,
            blue = oldPool.blue,
            black = oldPool.black,
            red = oldPool.red,
            green = oldPool.green,
            colorless = oldPool.colorless + 1,
            restrictedMana = oldPool.restrictedMana,
            manaBySubtype = oldPool.manaBySubtype,
            manaBySource = oldPool.manaBySource
        )
        return Dampening(state.updateEntity(tapperId) { it.with(dampenedPool) }, true)
    }

    /**
     * The tap payoffs, in resolution order: aura bonuses attached to the source
     * ([AdditionalManaOnTap] — Elvish Guidance), global "whenever a matching source is tapped for
     * mana" statics ([AdditionalManaOnSourceTap] — Lavaleaper, Badgermole Cub, Overabundance), the
     * [LandTappedForManaEvent] that Mana Flare-style triggers watch, and finally the any-color tap
     * bonuses (Fertile Ground), which may pause for a color decision.
     *
     * [manaEvent] describes what the ability itself produced; it gates the `whenProducing` clause
     * and supplies the color a mirror bonus copies. [carriedEvents] is the event list to append to
     * — the caller has already put [manaEvent] (or its dampened replacement) into it.
     */
    fun finishTapBonuses(
        state: GameState,
        sourceId: EntityId,
        sourceCard: CardComponent?,
        tapperId: EntityId,
        manaEvent: ManaAddedEvent?,
        carriedEvents: List<GameEvent>,
    ): ExecutionResult {
        val onTap = resolveAdditionalManaOnTap(state, sourceId, tapperId, carriedEvents)
        val onSourceTap = resolveAdditionalManaOnSourceTap(
            onTap.state, sourceId, tapperId, manaEvent, onTap.events
        )
        var allManaEvents = onSourceTap.events

        // Emit a "land tapped for mana" event so triggers like Overabundance / Mana Flare
        // ("whenever a player taps a land for mana") can fire. Manual-tap path only —
        // automatic cost payment adds mana via the solver without re-entering this pipeline.
        if (sourceCard?.typeLine?.isLand == true) {
            allManaEvents = allManaEvents + LandTappedForManaEvent(
                tapperId = tapperId,
                landId = sourceId,
                landName = sourceCard.name
            )
        }

        val anyColorBonuses = tappedForManaBonusResolver.collect(onSourceTap.state, sourceId, tapperId)
        return tappedForManaBonusResolver.drive(onSourceTap.state, anyColorBonuses, allManaEvents)
    }

    /**
     * After a mana ability resolves on a permanent, check for auras attached to it
     * that have [AdditionalManaOnTap] (e.g., Elvish Guidance). These are triggered mana
     * abilities that resolve immediately without using the stack.
     */
    private fun resolveAdditionalManaOnTap(
        state: GameState,
        sourceId: EntityId,
        controllerId: EntityId,
        existingEvents: List<GameEvent>
    ): AdditionalManaResult {
        var currentState = state
        val events = existingEvents.toMutableList()

        // Find all auras attached to the source permanent
        for (entityId in currentState.getBattlefield()) {
            val container = currentState.getEntity(entityId) ?: continue
            val attachedTo = container.get<AttachedToComponent>()
            if (attachedTo?.targetId != sourceId) continue

            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue

            // Check each static ability for AdditionalManaOnTap
            for (staticAbility in cardDef.script.staticAbilities) {
                val additionalMana = staticAbility as? AdditionalManaOnTap ?: continue

                // The controller of the enchanted land gets the mana
                val landController = currentState.getEntity(sourceId)
                    ?.get<ControllerComponent>()?.playerId ?: controllerId

                val context = EffectContext(
                    sourceId = entityId,
                    controllerId = landController,
                    targets = emptyList(),
                    xValue = null
                )

                val amount = dynamicAmountEvaluator.evaluate(currentState, additionalMana.amount, context)
                if (amount <= 0) continue

                // Resolve the color: if the ability specifies null, read the aura's chosen color.
                // If no color is chosen (e.g., somehow on battlefield without a choice), skip.
                val manaColor = additionalMana.color
                    ?: container.chosenColor()
                    ?: continue

                // Triggered mana ability — apply AdditionalSourceTriggers doublers
                // (e.g., Twinflame Travelers) so the bonus fires N+1 times.
                val auraController = container.get<ControllerComponent>()?.playerId ?: landController
                val extraFirings = countAdditionalSourceTriggerDoublers(currentState, entityId, auraController)
                val firings = 1 + extraFirings
                repeat(firings) {
                    currentState = currentState.updateEntity(landController) { c ->
                        val pool = c.get<ManaPoolComponent>() ?: ManaPoolComponent()
                        c.with(pool.add(manaColor, amount))
                    }

                    events.add(ManaAddedEvent(
                        playerId = landController,
                        sourceId = entityId,
                        sourceName = card.name,
                        white = if (manaColor == Color.WHITE) amount else 0,
                        blue = if (manaColor == Color.BLUE) amount else 0,
                        black = if (manaColor == Color.BLACK) amount else 0,
                        red = if (manaColor == Color.RED) amount else 0,
                        green = if (manaColor == Color.GREEN) amount else 0,
                        colorless = 0
                    ))
                }
            }
        }

        return AdditionalManaResult(currentState, events)
    }

    /**
     * After a permanent's mana ability resolves, check for [AdditionalManaOnSourceTap]
     * statics anywhere on the battlefield whose `sourceFilter` matches the tapped source.
     * Each match adds bonus mana to the tapping player's pool.
     *
     * Filter matching uses projected state so animated creature-lands and typeshifted
     * lands count under their projected types (Rule 613.1). The static-ability source's
     * controller is read from projected state so control-changing effects (Annex,
     * Ray of Command) correctly transfer the "you tap" condition along with the permanent.
     *
     * Triggered mana ability — resolves immediately without using the stack (Rule 605).
     */
    private fun resolveAdditionalManaOnSourceTap(
        state: GameState,
        sourceId: EntityId,
        tappingPlayerId: EntityId,
        manaEvent: ManaAddedEvent?,
        existingEvents: List<GameEvent>
    ): AdditionalManaResult {
        state.getEntity(sourceId) ?: return AdditionalManaResult(state, existingEvents)

        // The mirror-color form (color = null) needs the actual produced color from manaEvent.
        // The fixed-color form does not.
        val producedColor: Color? = manaEvent?.let {
            when {
                it.white > 0 -> Color.WHITE
                it.blue > 0 -> Color.BLUE
                it.black > 0 -> Color.BLACK
                it.red > 0 -> Color.RED
                it.green > 0 -> Color.GREEN
                else -> null
            }
        }
        val producedColorless = manaEvent != null && producedColor == null && manaEvent.colorless > 0

        var currentState = state
        val events = existingEvents.toMutableList()

        // Printed statics on the battlefield, plus statics granted at runtime. The grant channel is
        // what lets a *spell* create one of these at all — High Tide has no permanent to print it
        // on and grants it to its controller for the turn.
        val holders: List<Pair<EntityId, AdditionalManaOnSourceTap>> =
            currentState.getBattlefield().flatMap { entityId ->
                val container = currentState.getEntity(entityId)
                val card = container?.get<CardComponent>()
                val cardDef = card?.let { cardRegistry.getCard(it.cardDefinitionId) }
                cardDef?.script?.staticAbilities.orEmpty()
                    .filterIsInstance<AdditionalManaOnSourceTap>()
                    .map { entityId to it }
            } + currentState.grantedStaticAbilities.mapNotNull { granted ->
                (granted.ability as? AdditionalManaOnSourceTap)?.let { granted.entityId to it }
            }

        for ((entityId, onSourceTap) in holders) {
            val container = currentState.getEntity(entityId)
            val card = container?.get<CardComponent>()
            run {

                // Gate on the produced-mana type ("tap a land for {C}" only fires on a colorless tap).
                if (!producedManaMatches(onSourceTap.whenProducing, producedColor, producedColorless)) continue

                // A grant held by a player entity has no controller of its own; the holder *is*
                // the player, which is the right "you" for any controller predicate in the filter.
                val staticController = currentState.projectedState.getController(entityId)
                    ?: entityId.takeIf { currentState.turnOrder.contains(it) }
                    ?: continue

                // Filter is evaluated from the static-ability controller's perspective so
                // `youControl` on the source filter means "controlled by you, the static
                // controller" — see AdditionalManaOnSourceTap kdoc.
                val filterContext = PredicateContext(controllerId = staticController, sourceId = entityId)
                if (!predicateEvaluator.matches(
                        currentState, currentState.projectedState, sourceId, onSourceTap.sourceFilter, filterContext
                    )) continue

                val effectContext = EffectContext(
                    sourceId = entityId,
                    controllerId = tappingPlayerId,
                    targets = emptyList(),
                    xValue = null
                )
                val bonusAmount = dynamicAmountEvaluator.evaluate(currentState, onSourceTap.amount, effectContext)
                if (bonusAmount <= 0) continue

                // Resolve the bonus color: explicit color wins; null means mirror the produced color.
                val bonusColor: Color? = onSourceTap.color ?: producedColor
                val bonusColorless = onSourceTap.color == null && bonusColor == null && producedColorless
                if (bonusColor == null && !bonusColorless) continue

                // Triggered mana abilities bypass the stack but are still triggered
                // abilities — so AdditionalSourceTriggers (Twinflame Travelers) doubles
                // them just like any other trigger. firings = 1 (natural) + N (doublers).
                val extraFirings = countAdditionalSourceTriggerDoublers(currentState, entityId, staticController)
                val firings = 1 + extraFirings
                repeat(firings) {
                    currentState = currentState.updateEntity(tappingPlayerId) { c ->
                        val pool = c.get<ManaPoolComponent>() ?: ManaPoolComponent()
                        val newPool = if (bonusColor != null) pool.add(bonusColor, bonusAmount)
                                      else pool.addColorless(bonusAmount)
                        c.with(newPool)
                    }

                    events.add(ManaAddedEvent(
                        playerId = tappingPlayerId,
                        sourceId = entityId,
                        sourceName = card?.name ?: "",
                        white = if (bonusColor == Color.WHITE) bonusAmount else 0,
                        blue = if (bonusColor == Color.BLUE) bonusAmount else 0,
                        black = if (bonusColor == Color.BLACK) bonusAmount else 0,
                        red = if (bonusColor == Color.RED) bonusAmount else 0,
                        green = if (bonusColor == Color.GREEN) bonusAmount else 0,
                        colorless = if (bonusColorless) bonusAmount else 0
                    ))

                    // Inline non-mana rider (Overabundance: "deals 1 damage to the player").
                    // Resolved with controllerId = the tapping player, sourceId = this static's
                    // source, so EffectTarget.Controller is the tapper and EffectTarget.Self is the
                    // enchantment. Riders here must not require player input (no stack).
                    val rider = onSourceTap.rider
                    if (rider != null) {
                        val riderResult = effectExecutorRegistry.execute(currentState, rider, effectContext)
                        currentState = riderResult.state
                        events.addAll(riderResult.events)
                    }
                }
            }
        }

        return AdditionalManaResult(currentState, events)
    }

    /**
     * Whether a tap that produced [producedColor] (colored) or [producedColorless] (colorless)
     * satisfies an [AdditionalManaOnSourceTap]'s [TappedForManaType] gate.
     */
    private fun producedManaMatches(
        whenProducing: TappedForManaType,
        producedColor: Color?,
        producedColorless: Boolean
    ): Boolean = when (whenProducing) {
        TappedForManaType.ANY -> true
        TappedForManaType.COLORLESS -> producedColorless
        TappedForManaType.COLORED -> producedColor != null
    }

    /**
     * Count how many [AdditionalSourceTriggers] doublers on the battlefield apply to a
     * triggered ability with source [triggerSourceId] controlled by [triggerControllerId].
     *
     * Triggered mana abilities ([AdditionalManaOnTap], [AdditionalManaOnSourceTap]) bypass
     * the stack and are resolved synchronously, so they never flow through the normal
     * `TriggerDetector` doubling pass. This helper lets the inline mana resolution paths
     * apply the same doubling logic as the trigger pipeline.
     *
     * Returns N — N additional firings on top of the natural one (so total firings = N + 1).
     */
    private fun countAdditionalSourceTriggerDoublers(
        state: GameState,
        triggerSourceId: EntityId,
        triggerControllerId: EntityId
    ): Int {
        val projected = state.projectedState
        var count = 0
        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>()) continue
            val controllerId = projected.getController(permanentId) ?: continue
            if (controllerId != triggerControllerId) continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = container.get<ClassLevelComponent>()?.currentLevel
            for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                if (ability !is AdditionalSourceTriggers) continue
                // Optional gate ("as long as ~ is equipped") — evaluated against the doubler source.
                val gate = ability.condition
                if (gate != null && !conditionEvaluator.evaluate(
                        state, gate, EffectContext(sourceId = permanentId, controllerId = controllerId)
                    )
                ) continue
                // `alsoSource` doubles the doubler's own triggers regardless of the filter.
                if (ability.alsoSource && permanentId == triggerSourceId) {
                    count++
                    continue
                }
                if (ability.excludeSelf && permanentId == triggerSourceId) continue
                if (!predicateEvaluator.matches(
                        state, projected, triggerSourceId, ability.sourceFilter,
                        PredicateContext(controllerId = controllerId, sourceId = permanentId)
                    )
                ) continue
                count++
            }
        }
        return count
    }

    /**
     * Check if any permanent on the battlefield has DampLandManaProduction static ability.
     */
    private fun hasDampLandManaProduction(state: GameState): Boolean {
        for (playerId in state.turnOrder) {
            for (entityId in state.getBattlefield(playerId)) {
                val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
                val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                if (cardDef.script.staticAbilities.any { it is DampLandManaProduction }) {
                    return true
                }
            }
        }
        return false
    }
}
