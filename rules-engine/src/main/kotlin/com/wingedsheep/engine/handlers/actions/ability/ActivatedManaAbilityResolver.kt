package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.AbilityActivatedEvent
import com.wingedsheep.engine.handlers.effects.mana.ManaProvenanceTracker
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
import com.wingedsheep.engine.handlers.effects.mana.manaColorsAdded
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.activeManaSpendingScope
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.chosenColor
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaSymbol
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
        manaSpent: com.wingedsheep.engine.mechanics.mana.SpentMana,
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
        // Filter-based mana replacement (Pulse of Llanowar, Deep Water, Quarum Trench Gnomes): a
        // matched land produces other mana instead of its normal mana. Every replacement the land
        // is subject to applies in turn, each to the previous one's output — fixed ones in
        // battlefield order, free-choice ones last (ManaSolver.replacedLandMana folds the same way),
        // so Pulse of Llanowar over a Gnomes-hit Plains still offers every colour.
        for (colorReplacement in manaColorReplacementsFor(currentState, action.sourceId).sortedBy { it.produces == null }) {
            finalEffect = replaceProducedMana(finalEffect, colorReplacement)
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
            // A mana ability doesn't use the stack; its effect is a program this resolver starts.
            programOrigin = com.wingedsheep.engine.handlers.effects.composite.EffectProgramInterpreter.startedBy(ActivatedManaAbilityResolver::class),
            chosenModes = action.chosenModes.orEmpty(),
            modeTargetsOrdered = action.modeTargetsOrdered,
            sourceId = action.sourceId,
            objectReferences = activation.activationReferences.authorize(activationEvents),
            controllerId = action.playerId,
            granterId = activation.staticGranterId,
            targets = action.targets,
            // Use the announced X, including values defined by a variable cost selection.
            // The submitted action may not carry that derived value.
            xValue = activation.effectiveXValue,
            // A mana ability resolves off the stack, so nothing else hands it the last-known
            // information its cost captured. Priest of Yawgmoth ("{T}, Sacrifice an artifact:
            // Add an amount of {B} equal to the sacrificed artifact's mana value") reads the
            // sacrificed permanent's mana value through EffectTarget.SacrificedAsCost after that
            // permanent is already in the graveyard (CR 113.7a); without the snapshots the
            // amount resolves to 0 and the ability produces nothing.
            sacrificedPermanents = sacrificedSnapshots,
            manaSpent = manaSpent,
            lastKnownSourceSnapshot = if (state.projectedState.getTypes(action.sourceId).isEmpty())
                com.wingedsheep.engine.state.components.stack.captureLastKnown(stateBeforeActivation, action.sourceId) else null,
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

        if (currentState.activeManaSpendingScope(action.playerId) != null) {
            val frame = com.wingedsheep.engine.core.ScopedManaProductionContinuation(
                action.playerId, action.sourceId, sourceName, cardComponent,
                currentState.getEntity(action.playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent(),
                ManaProvenanceTracker.sourceTag(
                    if (action.sourceId in currentState.getBattlefield()) currentState else stateBeforeActivation,
                    action.sourceId,
                ), costsTap,
            )
            val result = effectExecutorRegistry.execute(currentState.pushContinuation(frame), finalEffect, context).toExecutionResult()
            if (result.outcome is Outcome.Paused)
                return ExecutionResult.propagatePause(result.state, activationEvents + result.events.filterNot {
                    it is ManaAddedEvent && it.sourceId == action.sourceId && it.playerId == action.playerId
                })
            if (result.outcome !is Outcome.Done) return result
            val (_, completed) = result.state.popContinuation()
            return com.wingedsheep.engine.handlers.effects.mana.finishScopedManaProduction(
                completed, frame, activationEvents + result.events, manaPipeline,
            )
        }
        val stateBeforeEffect = currentState
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
        currentState = ManaProvenanceTracker.markSnowProduction(stateBeforeEffect, dampening.state, action.sourceId, action.playerId)

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
        // Every color the activation added, read off the pool before tagging moves it into
        // per-unit entries — a composite's ManaAddedEvent reports only its first part.
        val producedColors = if (dampening.dampened) emptySet()
            else manaColorsAdded(stateBeforeEffect, dampening.state, action.playerId)

        // Read the production event before tagging moves plain mana into per-unit entries.
        // Tag before triggered bonuses so those bonuses don't inherit the activation obligation.
        currentState = com.wingedsheep.engine.state.tagManaObligationProduction(
            stateBeforeEffect, currentState, action.playerId, action.sourceId,
        )

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
        //
        // All of these key on the source being *tapped* for mana, so a mana ability without {T}
        // in its cost (Ashnod's Altar, a Gold token) gets none of them — Kinnan's ruling — the
        // same gate the scoped-production path applies.
        if (!costsTap) return ExecutionResult.success(currentState, eventsWithMana + effectResult.events)
        return manaPipeline.finishTapBonuses(
            currentState, action.sourceId, cardComponent, action.playerId,
            manaEvent, eventsWithMana + effectResult.events, producedColors
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
     * The [ReplaceLandManaColor] statics the land [landId] is subject to — every permanent on the
     * battlefield with that static (printed or granted) whose filter matches the tapped land from
     * the static controller's projected perspective, in battlefield order.
     */
    private fun manaColorReplacementsFor(
        state: GameState,
        landId: EntityId
    ): List<ReplaceLandManaColor> {
        var matched: MutableList<ReplaceLandManaColor>? = null
        val grantsByEntity = state.grantedStaticAbilities.groupBy { it.entityId }
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val printed = cardRegistry.getCard(card.cardDefinitionId)?.script?.staticAbilities.orEmpty()
            // Granted statics too — a durational "{U}: … until end of turn" mana rule (Deep Water)
            // or an indefinite one on a targeted land (Quarum Trench Gnomes) lives only in
            // `grantedStaticAbilities`, since the layer projector doesn't carry them.
            val granted = grantsByEntity[entityId]?.map { it.ability }.orEmpty()
            for (staticAbility in if (granted.isEmpty()) printed else printed + granted) {
                val replacement = staticAbility as? ReplaceLandManaColor ?: continue
                val staticController = state.projectedState.getController(entityId) ?: continue
                val filterContext = PredicateContext(controllerId = staticController, sourceId = entityId)
                if (predicateEvaluator.matches(state, state.projectedState, landId, replacement.filter, filterContext)) {
                    (matched ?: mutableListOf<ReplaceLandManaColor>().also { matched = it }).add(replacement)
                }
            }
        }
        return matched ?: emptyList()
    }

    /**
     * [effect]'s produced mana after [replacement]: one mana of a color of the controller's choice
     * when it names no symbol (Pulse of Llanowar — routed through the any-color machinery, so a
     * manual tap prompts and the solver sees a five-colour source), the fixed colour it names (Deep
     * Water), or colorless (Quarum Trench Gnomes). A replacement that names a [ReplaceLandManaColor.replaces]
     * colour leaves every other kind of mana — colorless included — alone.
     */
    private fun replaceProducedMana(effect: Effect, replacement: ReplaceLandManaColor): Effect {
        val replaces = replacement.replaces
        return when (effect) {
            is AddManaEffect -> {
                if (replaces != null && effect.color != replaces) return effect
                when (val produces = replacement.produces) {
                    null -> AddManaOfChoiceEffect(ManaColorSet.AnyColor, effect.amount)
                    is ManaSymbol.Colored -> effect.copy(color = produces.color)
                    else -> AddColorlessManaEffect(
                        effect.amount, effect.restriction, riders = effect.riders, expiry = effect.expiry
                    )
                }
            }
            is AddColorlessManaEffect -> {
                if (replaces != null) return effect
                when (val produces = replacement.produces) {
                    null -> AddManaOfChoiceEffect(ManaColorSet.AnyColor, effect.amount)
                    is ManaSymbol.Colored -> AddManaEffect(
                        produces.color, effect.amount, effect.restriction,
                        expiry = effect.expiry, riders = effect.riders
                    )
                    else -> effect
                }
            }
            else -> effect
        }
    }

    /**
     * The combined [MultiplyManaOnSourceTap] factor applying to [sourceId] being tapped for mana
     * (Virtue of Strength: 3). Returns 1 when nothing on the battlefield multiplies this source.
     *
     * Instances stack **multiplicatively** — two Virtues of Strength make a basic land produce nine
     * times as much, per the printed ruling — so the factors are folded with `*`.
     *
     * Mirrors [manaColorReplacementsFor]: each static's filter is evaluated from the
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

    /**
     * Scales the mana [effect] produces by [multiplier], leaving everything else about it — color,
     * restriction, riders, expiry — untouched. Recurses into a [CompositeEffect] so a mana ability
     * bundled with a side effect (pain, a counter) scales its mana half only.
     *
     * [AddOneManaOfEachColorAmongEffect] has no amount to scale (it is "one of each colour among
     * …"), so it is deliberately left alone rather than silently mis-scaled.
     */
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
