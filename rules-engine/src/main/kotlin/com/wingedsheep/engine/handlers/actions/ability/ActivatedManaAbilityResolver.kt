package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.AbilityActivatedEvent
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaAddedEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.handlers.effects.mana.ManaAbilityResolutionPipeline
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.chosenColor
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.MultiplyManaOnSourceTap
import com.wingedsheep.sdk.scripting.OverrideEnchantedLandManaColor
import com.wingedsheep.sdk.scripting.ReplaceLandManaColor
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaSpendOnChosenTypeEffect
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.effects.AddOneManaOfEachColorAmongEffect
import com.wingedsheep.engine.mechanics.mana.BorrowedManaAbilities
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Resolves a paid mana ability immediately, off the stack (CR 605.3): applies the mana
 * replacements that rewrite what the source produces, runs the effect, reports the mana added, and
 * hands the rest (dampening, tap bonuses, their possible color pause) to the shared
 * [ManaAbilityResolutionPipeline].
 */
internal class ActivatedManaAbilityResolver(
    private val cardRegistry: CardRegistry,
    conditionEvaluator: ConditionEvaluator,
    private val effectExecutorRegistry: EffectExecutorRegistry,
    private val predicateEvaluator: PredicateEvaluator
) {
    private val dynamicAmountEvaluator = predicateEvaluator.amounts
    /**
     * Everything that happens after a mana ability's effect resolves. Shared with
     * [com.wingedsheep.engine.handlers.continuations.ColorChoiceContinuationResumer], which
     * finishes the mana abilities that paused for a color choice.
     */
    private val manaPipeline = ManaAbilityResolutionPipeline(
        cardRegistry = cardRegistry,
        conditionEvaluator = conditionEvaluator,
        effectExecutorRegistry = effectExecutorRegistry,
        predicateEvaluator = predicateEvaluator,
        dynamicAmountEvaluator = dynamicAmountEvaluator,
    )

    /**
     * @param stateBeforeActivation the state the activation started from — dampening and the
     *   reported mana amounts are measured against it.
     * @param state the state after the ability's costs were paid and its activation recorded.
     * @param effect the ability's effect, text changes already applied.
     * @param events the activation's events so far (its cost side).
     * @param sacrificedSnapshots last-known information about permanents the cost sacrificed.
     */
    fun resolve(
        stateBeforeActivation: GameState,
        state: GameState,
        activation: Activation,
        effect: Effect,
        events: List<GameEvent>,
        sacrificedSnapshots: List<EntitySnapshot>,
    ): ExecutionResult {
        val action = activation.action
        val ability = activation.ability
        val sourceName = activation.sourceName
        val cardComponent = activation.cardComponent
        val costsTap = activation.effectiveCost.hasTapCost()
        var currentState = state
        var finalEffect = effect

        // A mana ability is still an *activated* ability (CR 605.3), so activating one is an
        // activation event like any other — Elrond, Moon-Reader's "whenever you activate an
        // ability of a creature" fires off a creature's "{T}: Add {G}" per its ruling. Mana
        // abilities resolve off the stack, so StackResolver never emits AbilityActivatedEvent
        // for them; emit it here for every mana ability, {T}-costed or not. Consumers pick
        // their own semantic off the flags: the default "isn't a mana ability" wording rejects
        // `isManaAbility`, the Antiquities "without {T} in its activation cost" template
        // (Haunting Wind / Powerleech / Artifact Possession) rejects `costsTap`, and the
        // unqualified wording accepts both.
        //
        // Emitted here rather than after resolution because the branch below has two pause
        // exits — an any-color effect (Birds of Paradise) and an any-color tap bonus (Fertile
        // Ground) — and the ability is already activated by this point: its costs are paid.
        // Adding it to the carried events now carries it out through those exits too.
        val manaAbilityActivatedEvent = AbilityActivatedEvent(
            sourceId = action.sourceId,
            sourceName = sourceName,
            controllerId = action.playerId,
            abilityEntityId = null,
            costsTap = costsTap,
            isManaAbility = true,
            isExhaust = ability.isExhaust
        )
        val activationEvents = events + manaAbilityActivatedEvent

        // Check for an attached aura that overrides the produced mana color
        // (e.g., Shimmerwilds Growth: "Enchanted land is the chosen color").
        val overrideColor = findEnchantedLandManaColorOverride(currentState, action.sourceId)
        if (overrideColor != null && finalEffect is AddManaEffect) {
            finalEffect = finalEffect.copy(color = overrideColor)
        }
        // Filter-based mana-color replacement (Pulse of Llanowar): a matched land produces
        // one mana of a color of its controller's choice instead of its normal mana. Swapping
        // the base effect for AddManaOfChoiceEffect routes the choice through the existing
        // any-color machinery (action.manaColorChoice on a manual tap, or a resolution-time
        // color decision if none was supplied).
        val colorReplacement = manaColorReplacementFor(currentState, action.sourceId)
        if (colorReplacement != null) {
            val fixed = colorReplacement.color
            finalEffect = when (val fe = finalEffect) {
                // A *fixed* colour (Deep Water: "it produces {U} instead of any other type")
                // needs no choice at all — rewrite the produced mana directly.
                is AddManaEffect ->
                    if (fixed != null) fe.copy(color = fixed)
                    else AddManaOfChoiceEffect(ManaColorSet.AnyColor, fe.amount)
                is AddColorlessManaEffect ->
                    if (fixed != null) AddManaEffect(fixed, fe.amount)
                    else AddManaOfChoiceEffect(ManaColorSet.AnyColor, fe.amount)
                else -> finalEffect
            }
        }
        // Multiplicative mana replacement (Virtue of Strength: "If you tap a basic land for
        // mana, it produces three times as much of that mana instead"). Scaling the resolving
        // effect's amount — rather than the pool afterwards — keeps restricted mana, riders and
        // per-source provenance intact, and makes the ManaAddedEvent below report the real
        // amount for free. Gated on {T} in the cost: you are only "tapping a permanent for
        // mana" when the mana ability's cost includes the tap symbol.
        if (costsTap) {
            val manaMultiplier = manaProductionMultiplierFor(currentState, action.sourceId)
            if (manaMultiplier > 1) {
                finalEffect = multiplyManaProduced(finalEffect, manaMultiplier)
            }
        }
        // Tapping a permanent you don't control under a "tap … for mana" grant (Piracy): the
        // grant's spending restriction rides on the mana, on top of any the ability already had.
        // Read against the pre-activation state — the grant keyed on who controlled the source then.
        if (costsTap) {
            val borrowedRestriction = BorrowedManaAbilities
                .grantFor(stateBeforeActivation, action.playerId, action.sourceId, predicateEvaluator)
                ?.restriction
            if (borrowedRestriction != null) {
                finalEffect = restrictManaProduced(finalEffect, borrowedRestriction)
            }
        }
        val context = EffectContext(
            sourceId = action.sourceId,
            objectReferences = activation.activationReferences.authorize(activationEvents),
            controllerId = action.playerId,
            granterId = activation.staticGranterId,
            targets = action.targets,
            // Thread the chosen X so X-based mana abilities produce the right amount
            // ("{X}, {T}, Sacrifice this: Add X mana..." — Wizard's Rockets). Without
            // this, DynamicAmount.XValue resolves to 0 and the ability adds no mana.
            xValue = action.xValue,
            // A mana ability resolves off the stack, so nothing else hands it the last-known
            // information its cost captured. Priest of Yawgmoth ("{T}, Sacrifice an artifact:
            // Add an amount of {B} equal to the sacrificed artifact's mana value") reads the
            // sacrificed permanent's mana value through EffectTarget.SacrificedAsCost after that
            // permanent is already in the graveyard (CR 113.7a); without the snapshots the
            // amount resolves to 0 and the ability produces nothing.
            sacrificedPermanents = sacrificedSnapshots,
            // Dropped when another player chooses the color at resolution (Spectral Searchlight) —
            // the activator has no say in it, whatever the client sent.
            manaColorChoice = action.manaColorChoice.takeUnless {
                com.wingedsheep.engine.mechanics.mana.ManaColorChoiceTiming
                    .chosenByAnotherPlayerAtResolution(finalEffect)
            },
            // Mana abilities resolve without a stack component, so their activation-time
            // provenance must enter the effect context here. The concrete id remains useful
            // even when this lookup branch could not prove a definition-scoped identity.
            abilityIdentity = activation.abilityLookup.definitionIdentity,
            activatedAbility = ability,
        )

        val effectResult = effectExecutorRegistry.execute(currentState, finalEffect, context).toExecutionResult()
        // A pause (e.g. choosing colors for "add X mana in any combination of colors") carries
        // the activation's own events out with it, so the settle boundary queues the triggers
        // they cause (Wizard's Rockets' dies trigger, Ceaseless Searblades' activation trigger)
        // until the ability finishes resolving.
        if (effectResult.outcome is Outcome.Paused) {
            return ExecutionResult.propagatePause(effectResult.state, activationEvents + effectResult.events)
        }
        if (effectResult.outcome !is Outcome.Done) {
            return effectResult
        }

        currentState = effectResult.newState

        // Check for Damping Sphere-style mana dampening on lands
        val dampening = manaPipeline.applyLandManaDampening(
            stateBeforeActivation, currentState, cardComponent, action.playerId
        )
        currentState = dampening.state

        // Emit ManaAddedEvent — if dampened, always emit 1 colorless
        val manaEvent: ManaAddedEvent? = if (dampening.dampened) {
            ManaAddedEvent(
                playerId = action.playerId,
                sourceId = action.sourceId,
                sourceName = sourceName,
                colorless = 1
            )
        } else when (val fe = finalEffect) {
            // A composite reports its first mana-producing member.
            is CompositeEffect -> fe.effects
                .firstOrNull { it.isReportedManaEffect() }
                ?.let { manaAddedEvent(it, stateBeforeActivation, currentState, action, activation, context) }
            else -> manaAddedEvent(fe, stateBeforeActivation, currentState, action, activation, context)
        }

        val eventsWithMana = if (manaEvent != null) activationEvents + manaEvent else activationEvents

        // Aura bonuses (Elvish Guidance), global "whenever a matching source is tapped for
        // mana" statics (Lavaleaper, Badgermole Cub, Overabundance), the land-tapped event, and
        // the any-color tap bonuses (Fertile Ground) — the last of which may pause for a color
        // decision. Shared with the color-choice resume path so both agree.
        //
        // Triggered abilities from the activation go on the stack at the settle boundary: the
        // cost-side events (a sacrificed source's dies trigger, the {T} TappedEvent for an
        // artifact-tap trigger), the mana ability's own resolution events (Rubble Rouser's
        // reflexive "when you do" half, which is not itself a mana ability, CR 605.1a), and the
        // land-tapped event Mana Flare-style triggers watch. Such triggered abilities still use
        // the stack even though the mana ability itself resolves off it.
        return manaPipeline.finishTapBonuses(
            currentState, action.sourceId, cardComponent, action.playerId,
            manaEvent, eventsWithMana + effectResult.events
        )
    }

    /** The mana effects whose production is reported as a [ManaAddedEvent]. */
    private fun Effect.isReportedManaEffect(): Boolean =
        this is AddManaEffect ||
            this is AddColorlessManaEffect ||
            this is AddManaOfChoiceEffect ||
            this is AddAnyColorManaSpendOnChosenTypeEffect

    /**
     * The [ManaAddedEvent] reporting what a (non-composite) mana [effect] produced, or null for an
     * effect whose production isn't reported here. Amounts are evaluated against [oldState], the
     * state the activation started from.
     */
    private fun manaAddedEvent(
        effect: Effect,
        oldState: GameState,
        newState: GameState,
        action: ActivateAbility,
        activation: Activation,
        context: EffectContext,
    ): ManaAddedEvent? = when (effect) {
        is AddManaEffect -> {
            val amount = dynamicAmountEvaluator.evaluate(oldState, effect.amount, context)
            coloredManaAddedEvent(action, activation.sourceName, effect.color, amount)
        }
        is AddColorlessManaEffect -> {
            val amount = dynamicAmountEvaluator.evaluate(oldState, effect.amount, context)
            ManaAddedEvent(
                playerId = action.playerId,
                sourceId = action.sourceId,
                sourceName = activation.sourceName,
                colorless = amount
            )
        }
        is AddManaOfChoiceEffect -> manaAddedEventFromPoolDelta(
            oldState, newState, action, activation.cardComponent
        )
        is AddAnyColorManaSpendOnChosenTypeEffect -> {
            val chosenColor = action.manaColorChoice ?: Color.GREEN
            val amount = dynamicAmountEvaluator.evaluate(oldState, effect.amount, context)
            coloredManaAddedEvent(action, activation.sourceName, chosenColor, amount)
        }
        else -> null
    }

    private fun coloredManaAddedEvent(
        action: ActivateAbility,
        sourceName: String,
        color: Color,
        amount: Int,
    ) = ManaAddedEvent(
        playerId = action.playerId,
        sourceId = action.sourceId,
        sourceName = sourceName,
        white = if (color == Color.WHITE) amount else 0,
        blue = if (color == Color.BLUE) amount else 0,
        black = if (color == Color.BLACK) amount else 0,
        red = if (color == Color.RED) amount else 0,
        green = if (color == Color.GREEN) amount else 0,
        colorless = 0
    )

    /**
     * Build a [ManaAddedEvent] by diffing the controller's mana pool before and after
     * the effect executed. Used for [AddManaOfChoiceEffect]: the executor already
     * resolved the color set, picked the color, and added the mana — we just need to
     * report what changed for client display.
     */
    private fun manaAddedEventFromPoolDelta(
        oldState: GameState,
        newState: GameState,
        action: ActivateAbility,
        cardComponent: CardComponent,
    ): ManaAddedEvent? {
        val oldPool = oldState.getEntity(action.playerId)
            ?.get<ManaPoolComponent>()
        val newPool = newState.getEntity(action.playerId)
            ?.get<ManaPoolComponent>()
            ?: return null
        return ManaAddedEvent(
            playerId = action.playerId,
            sourceId = action.sourceId,
            sourceName = nameVisibleToAll(oldState, action.sourceId, cardComponent.name),
            white = newPool.white - (oldPool?.white ?: 0),
            blue = newPool.blue - (oldPool?.blue ?: 0),
            black = newPool.black - (oldPool?.black ?: 0),
            red = newPool.red - (oldPool?.red ?: 0),
            green = newPool.green - (oldPool?.green ?: 0),
            colorless = newPool.colorless - (oldPool?.colorless ?: 0),
        ).takeIf { it.white + it.blue + it.black + it.red + it.green + it.colorless > 0 }
    }

    /**
     * If any aura attached to [sourceId] has an [OverrideEnchantedLandManaColor]
     * static ability, return the color the enchanted land's own mana abilities
     * should produce instead. `null` means no override (mana ability produces
     * normally). Multiple auras: last-wins (same aura only applies once).
     */
    private fun findEnchantedLandManaColorOverride(
        state: GameState,
        sourceId: EntityId
    ): Color? {
        var override: Color? = null
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val attachedTo = container.get<AttachedToComponent>()
            if (attachedTo?.targetId != sourceId) continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (staticAbility in cardDef.script.staticAbilities) {
                val o = staticAbility as? OverrideEnchantedLandManaColor ?: continue
                override = o.color
                    ?: container.chosenColor()
                    ?: continue
            }
        }
        return override
    }

    /**
     * The [ReplaceLandManaColor] static the land [landId] is subject to, if any — some permanent on
     * the battlefield has that static and its filter matches the tapped land from the static
     * controller's projected perspective. The land's produced mana is then replaced: with one mana
     * of a color of its controller's choice (Pulse of Llanowar), or with the static's fixed `color`
     * when it names one (Deep Water). Returns the static rather than a Boolean so the caller can
     * tell those two apart.
     */
    private fun manaColorReplacementFor(
        state: GameState,
        landId: EntityId
    ): ReplaceLandManaColor? {
        val grantsByEntity = state.grantedStaticAbilities.groupBy { it.entityId }
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val printed = cardRegistry.getCard(card.cardDefinitionId)?.script?.staticAbilities.orEmpty()
            // Granted statics too — a durational "{U}: … until end of turn" mana rule (Deep Water)
            // lives only in `grantedStaticAbilities`, since the layer projector doesn't carry them.
            val granted = grantsByEntity[entityId]?.map { it.ability }.orEmpty()
            for (staticAbility in if (granted.isEmpty()) printed else printed + granted) {
                val replacement = staticAbility as? ReplaceLandManaColor ?: continue
                val staticController = state.projectedState.getController(entityId) ?: continue
                val filterContext = PredicateContext(controllerId = staticController, sourceId = entityId)
                if (predicateEvaluator.matches(state, state.projectedState, landId, replacement.filter, filterContext)) {
                    return replacement
                }
            }
        }
        return null
    }

    /**
     * The combined [MultiplyManaOnSourceTap] factor applying to [sourceId] being tapped for mana
     * (Virtue of Strength: 3). Returns 1 when nothing on the battlefield multiplies this source.
     *
     * Instances stack **multiplicatively** — two Virtues of Strength make a basic land produce nine
     * times as much, per the printed ruling — so the factors are folded with `*`.
     *
     * Mirrors [manaColorReplacementFor]: each static's filter is evaluated from the
     * *static's own* projected controller, so `.youControl()` means "controlled by the player who
     * controls the Virtue", which for a mana ability is necessarily the tapping player.
     */
    private fun manaProductionMultiplierFor(
        state: GameState,
        sourceId: EntityId
    ): Int {
        var multiplier = 1
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (staticAbility in cardDef.script.staticAbilities) {
                val static = staticAbility as? MultiplyManaOnSourceTap ?: continue
                if (static.multiplier <= 1) continue
                val staticController = state.projectedState.getController(entityId) ?: continue
                val filterContext = PredicateContext(controllerId = staticController, sourceId = entityId)
                if (predicateEvaluator.matches(
                        state, state.projectedState, sourceId, static.sourceFilter, filterContext
                    )
                ) {
                    multiplier *= static.multiplier
                }
            }
        }
        return multiplier
    }

    /**
     * Scales the mana [effect] produces by [multiplier], leaving everything else about it — color,
     * restriction, riders, expiry — untouched. Recurses into a [CompositeEffect] so a mana ability
     * bundled with a side effect (pain, a counter) scales its mana half only.
     *
     * [AddOneManaOfEachColorAmongEffect] has no amount to scale (it is "one of each colour among
     * …"), so it is deliberately left alone rather than silently mis-scaled.
     */
    /**
     * Adds [restriction] to the mana [effect] produces, keeping any restriction it already carries
     * (both then apply). Recurses into a [CompositeEffect] like [multiplyManaProduced].
     *
     * [AddAnyColorManaSpendOnChosenTypeEffect] derives its restriction at resolution and has no
     * field to add to, so its mana is left as is.
     */
    private fun restrictManaProduced(effect: Effect, restriction: ManaRestriction): Effect = when (effect) {
        is AddManaEffect -> effect.copy(restriction = BorrowedManaAbilities.combine(effect.restriction, restriction))
        is AddColorlessManaEffect ->
            effect.copy(restriction = BorrowedManaAbilities.combine(effect.restriction, restriction))
        is AddManaOfChoiceEffect ->
            effect.copy(restriction = BorrowedManaAbilities.combine(effect.restriction, restriction))
        is AddDynamicManaEffect ->
            effect.copy(restriction = BorrowedManaAbilities.combine(effect.restriction, restriction))
        is AddOneManaOfEachColorAmongEffect ->
            effect.copy(restriction = BorrowedManaAbilities.combine(effect.restriction, restriction))
        is CompositeEffect -> effect.copy(effects = effect.effects.map { restrictManaProduced(it, restriction) })
        else -> effect
    }

    private fun multiplyManaProduced(effect: Effect, multiplier: Int): Effect = when (effect) {
        is AddManaEffect -> effect.copy(amount = DynamicAmount.Multiply(effect.amount, multiplier))
        is AddColorlessManaEffect -> effect.copy(amount = DynamicAmount.Multiply(effect.amount, multiplier))
        is AddManaOfChoiceEffect -> effect.copy(amount = DynamicAmount.Multiply(effect.amount, multiplier))
        is AddAnyColorManaSpendOnChosenTypeEffect ->
            effect.copy(amount = DynamicAmount.Multiply(effect.amount, multiplier))
        is AddDynamicManaEffect ->
            effect.copy(amountSource = DynamicAmount.Multiply(effect.amountSource, multiplier))
        is CompositeEffect -> effect.copy(effects = effect.effects.map { multiplyManaProduced(it, multiplier) })
        else -> effect
    }
}
