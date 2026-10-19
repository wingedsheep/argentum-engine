package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Suspension
import com.wingedsheep.engine.core.TriggerModalTargetSelectionContinuation
import com.wingedsheep.engine.core.TriggeredAbilityContinuation
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.ModalLegalEnumeration
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.toEntityId
import com.wingedsheep.engine.legalactions.TargetInfo
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Picking targets for a spell or ability without simulating anything.
 *
 * Lifted out of `Strategist` in Phase 7, where it gained a second caller: a rollout
 * ([com.wingedsheep.ai.engine.rollout.PlayoutPolicy]) has to fill targets on every spell it plays
 * and is forbidden from simulating to do it — anything that simulates *inside* a playout makes the
 * playout quadratic. The Strategist still refines the targets it actually commits to by simulation
 * (`chooseCommittedTargets`); this is the heuristic floor both paths start from.
 *
 * The one thing everything here protects is that an action the AI submits is *legal*. Phase 1
 * measured the AI proposing ~0.9 illegal actions per game, 889 of 945 being "No valid targets
 * available", and [fillableRequirements] is where that was fixed.
 */
object TargetSelection {

    /**
     * Heuristic desirability of a target: higher = better. Opponent removal targets rank highest.
     *
     * @param intents Phase 6's card knowledge. On [IntentCatalog.NONE] an opponent's non-creature
     *   permanent falls back to the pre-Phase-6 flat `0.0`.
     */
    fun rank(
        state: GameState,
        entityId: EntityId,
        playerId: EntityId,
        intents: IntentCatalog = IntentCatalog.NONE,
    ): Double {
        val projected = state.projectedState
        val controller = projected.getController(entityId)
        // CR 810 — a teammate's permanent is not an opponent's, so removal must not rank it as
        // one. In a game without teams this is exactly the old `controller != playerId`.
        val isOpponent = controller != null && state.isOpponentTo(controller, playerId)
        val isPlayer = state.getEntity(entityId)?.get<PlayerComponent>() != null

        val card = state.getEntity(entityId)?.get<CardComponent>()

        return if (isPlayer) {
            // Player target — prefer opponent
            if (isOpponent) 5.0 else -5.0
        } else if (projected.isCreature(entityId)) {
            val value = if (card != null) {
                BoardPresence.permanentValue(state, projected, entityId, card, intents)
            } else 0.0
            // Opponent creatures: higher value = better target for removal
            // Friendly creatures rank below opponents in this removal-oriented fallback.
            if (isOpponent) value + 10.0 else -value
        } else if (card != null && intents.isEnabled) {
            // Phase 6. This branch used to be a flat `0.0`, which meant an opponent's Oblivion
            // Ring ranked exactly as high as an untapped Forest and *equally* as high as nothing —
            // the AI could not aim a Disenchant. It is the same shape as the creature branch above:
            // the permanent's board value, with the +10 that keeps any opponent permanent ahead of
            // any of ours.
            val value = BoardPresence.permanentValue(state, projected, entityId, card, intents)
            if (isOpponent) value + 10.0 else -value
        } else {
            0.0
        }
    }

    /**
     * Rank candidates for this action, reading the activated effect rather than its source card.
     * A pure, fixed stat boost wants a friendly creature; removal keeps the usual ranking.
     * Unknown, mixed, granted and multi-requirement abilities keep the existing fallback.
     * Resolve the ability once per action, outside the candidate sorting hot path.
     */
    fun ranker(
        state: GameState,
        action: LegalAction,
        playerId: EntityId,
        intents: IntentCatalog = IntentCatalog.NONE,
        /**
         * [com.wingedsheep.ai.engine.AiProfile.targetPolarityFromEffect]: rank by the polarity
         * [polarities] derives from the effect, for spells and abilities alike, instead of only
         * recognising a fixed stat boost on an activated ability.
         */
        polarityFromEffect: Boolean = false,
    ): (EntityId) -> Double {
        if (polarityFromEffect) {
            // One polarity per action, not per slot: the ranker is shared by every requirement of
            // the action. A multi-slot action whose slots disagree ("target creature you control
            // fights target creature you don't") reads UNKNOWN and keeps the removal ranking —
            // which is what it got before, and its filters already split the sides.
            val polarity = polarities(state, action, intents).distinct().singleOrNull()
            if (polarity == TargetPolarity.BENEFICIAL) {
                // Our best permanent first, theirs last; ourselves ahead of an opponent.
                return { entityId -> -rank(state, entityId, playerId, intents) }
            }
            if (polarity == TargetPolarity.HARMFUL) return { entityId -> rank(state, entityId, playerId, intents) }
        }
        val activation = action.action as? ActivateAbility
        val name = activation?.let { state.getEntity(it.sourceId)?.get<CardComponent>()?.name }
        val ability = if (name != null) intents.activatedAbility(name, activation.abilityId) else null
        val effect = ability?.effect as? ModifyStatsEffect
        val requirement = ability?.targetRequirements?.singleOrNull()
        val targetsRequirement = when (val target = effect?.target) {
            is EffectTarget.BoundVariable -> target.name == requirement?.id
            is EffectTarget.ContextTarget -> target.index == 0 && requirement != null
            else -> false
        }
        val power = (effect?.powerModifier as? DynamicAmount.Fixed)?.amount
        val toughness = (effect?.toughnessModifier as? DynamicAmount.Fixed)?.amount
        val beneficial = targetsRequirement && power != null && toughness != null &&
            power >= 0 && toughness >= 0 && (power > 0 || toughness > 0)
        return { entityId ->
            val value = rank(state, entityId, playerId, intents)
            if (beneficial && state.projectedState.isCreature(entityId)) -value else value
        }
    }

    /**
     * [TargetPolarity] of each of [action]'s target slots, in [targetInfosFor] order — or empty
     * when it cannot be read: no card knowledge, a choose-N modal spell whose modes are not yet
     * chosen, a face-down cast, or a definition whose requirements do not line up one-to-one with the slots
     * the enumerator offered. Empty means UNKNOWN everywhere, never "no targets".
     */
    fun polarities(state: GameState, action: LegalAction, intents: IntentCatalog): List<TargetPolarity> {
        if (!intents.isEnabled || action.modalEnumeration != null) return emptyList()
        val read = when (val base = action.action) {
            is CastSpell -> if (base.castFaceDown) emptyList()
                else state.getEntity(base.cardId)?.get<CardComponent>()?.name?.let { name ->
                    val transformed = castsTransformed(state, base)
                    // A choose-one modal (or tiered) spell is offered once per mode, with that
                    // mode pre-chosen: its slots are the mode's, and the card has none of its own.
                    if (base.chosenModes.isNotEmpty() && base.faceIndex == null && !base.castFused) {
                        intents.spellModeTargetPolarities(name, base.chosenModes, transformed)
                    } else {
                        intents.spellTargetPolarities(name, transformed, base.faceIndex, base.castFused)
                    }
                }.orEmpty()
            is ActivateAbility -> state.getEntity(base.sourceId)?.get<CardComponent>()?.name
                ?.let { intents.abilityTargetPolarities(it, base.abilityId) }.orEmpty()
            else -> emptyList()
        }
        val slots = targetInfosFor(action)?.size ?: return emptyList()
        return if (read.size == slots) read else emptyList()
    }

    /**
     * Whether [cast] puts its card on the stack **back face up**, so the spell's targets and
     * abilities are the back face's: a disturb cast (Mischievous Catgeist as Catlike Curiosity), a
     * modal DFC's back face, or a "cast it transformed" permission. Mirrors the engine's own
     * transformed-face routing in `CastSpellHandler`.
     */
    private fun castsTransformed(state: GameState, cast: CastSpell): Boolean {
        if (cast.useAlternativeCost && cast.alternativeCostType in TRANSFORMED_ALT_COSTS) return true
        return state.mayPlayPermissions.any {
            it.castTransformed && it.controllerId == cast.playerId && cast.cardId in it.cardIds
        }
    }

    private val TRANSFORMED_ALT_COSTS = setOf(AlternativeCostType.DISTURB, AlternativeCostType.MODAL_BACK_FACE)

    /**
     * Whether [action] has a mandatory target slot whose **every** legal target is on the wrong side
     * of the table for what the effect does to it — a Blossombind with only our own creatures to
     * enchant, a Prohibit with only our own spell on the stack, a Giant Growth with only theirs.
     *
     * Such an action can only be cast *at* us, so it is not a candidate: the right play is to hold
     * the card. The leaf score cannot be trusted to say so on its own — Prohibit at a spell above
     * its mana-value cap resolves to nothing, and nothing is exactly what a one-ply evaluator fails
     * to price against keeping the card.
     *
     * An optional slot never vetoes: it can be left empty. A target whose side cannot be read (an
     * ownerless object) is never "wrong", so one of those anywhere keeps the action.
     *
     * An activated ability that acts on the permanent its source is attached to is aimed the same
     * way, with no slot to fill: Strands of Undeath's "{B}: Regenerate enchanted creature" while it
     * enchants an opponent's creature only ever spends our mana on their board.
     */
    fun aimsOnlyAtTheWrongSide(
        state: GameState,
        action: LegalAction,
        playerId: EntityId,
        intents: IntentCatalog,
    ): Boolean {
        if (actsOnTheWrongSideOfItsHost(state, action, playerId, intents)) return true
        val polarities = polarities(state, action, intents)
        if (polarities.isEmpty()) return false
        val infos = targetInfosFor(action) ?: return false
        return infos.zip(polarities).any { (info, polarity) ->
            info.minTargets > 0 && info.validTargets.isNotEmpty() && when (polarity) {
                TargetPolarity.HARMFUL -> info.validTargets.all { sideOf(state, it, playerId) == Side.OURS }
                TargetPolarity.BENEFICIAL -> info.validTargets.all { sideOf(state, it, playerId) == Side.THEIRS }
                TargetPolarity.UNKNOWN -> false
            }
        }
    }

    private fun actsOnTheWrongSideOfItsHost(
        state: GameState,
        action: LegalAction,
        playerId: EntityId,
        intents: IntentCatalog,
    ): Boolean {
        if (!intents.isEnabled) return false
        val activation = action.action as? ActivateAbility ?: return false
        val source = state.getEntity(activation.sourceId) ?: return false
        val host = source.get<AttachedToComponent>()?.targetId ?: return false
        val name = source.get<CardComponent>()?.name ?: return false
        return when (intents.abilityHostPolarity(name, activation.abilityId)) {
            TargetPolarity.HARMFUL -> sideOf(state, host, playerId) == Side.OURS
            TargetPolarity.BENEFICIAL -> sideOf(state, host, playerId) == Side.THEIRS
            TargetPolarity.UNKNOWN -> false
        }
    }

    /**
     * [TargetPolarity] of each requirement of [decision], indexed by
     * [com.wingedsheep.engine.core.TargetRequirementInfo.index] — read off the continuation that
     * asked it, when that continuation carries the effect the targets are for: a triggered ability
     * being put on the stack ([TriggeredAbilityContinuation]), or one mode of a modal triggered
     * ability ([TriggerModalTargetSelectionContinuation] — Proud Pack-Rhino's "put a shield counter
     * on target permanent"). Empty — UNKNOWN everywhere — for any other question, and for a
     * trigger whose dependent targets are asked one slot at a time.
     */
    fun decisionPolarities(
        state: GameState,
        decision: ChooseTargetsDecision,
        intents: IntentCatalog,
    ): Map<Int, TargetPolarity> {
        if (!intents.isEnabled) return emptyMap()
        val suspension = state.peekContinuation() as? Suspension ?: return emptyMap()
        if (suspension.question.id != decision.id) return emptyMap()
        val read = when (val answer = suspension.answer) {
            is TriggeredAbilityContinuation ->
                if (answer.sequentialTargets != null) emptyList()
                else intents.effectTargetPolarities(answer.effect, answer.targetRequirements)
            is TriggerModalTargetSelectionContinuation -> {
                val mode = answer.chosenModeIndices.getOrNull(answer.currentOrdinal)
                    ?.let(answer.modes::getOrNull)
                if (mode == null) emptyList()
                else intents.effectTargetPolarities(mode.effect, mode.targetRequirements)
            }
            else -> emptyList()
        }
        return decision.targetRequirements.mapNotNull { req ->
            read.getOrNull(req.index)?.let { req.index to it }
        }.toMap()
    }

    /**
     * Whether [entityId] sits on our side of the table for [playerId] — the side a
     * [TargetPolarity.BENEFICIAL] slot wants. A teammate's permanent is ours (CR 810).
     */
    fun isOurs(state: GameState, entityId: EntityId, playerId: EntityId): Boolean =
        sideOf(state, entityId, playerId) == Side.OURS

    /** Whether [entityId] sits on an opponent's side for [playerId]. */
    fun isTheirs(state: GameState, entityId: EntityId, playerId: EntityId): Boolean =
        sideOf(state, entityId, playerId) == Side.THEIRS

    private enum class Side { OURS, THEIRS, NEITHER }

    /**
     * Which side of the table [entityId] sits on for [playerId]: a player is its own side, a
     * permanent its projected controller's, and a spell on the stack its caster's (the projection
     * covers the battlefield only, so the stack falls back to [ControllerComponent]). A teammate
     * is ours (CR 810).
     */
    private fun sideOf(state: GameState, entityId: EntityId, playerId: EntityId): Side {
        val controller = when {
            state.getEntity(entityId)?.get<PlayerComponent>() != null -> entityId
            else -> state.projectedState.getController(entityId)
                ?: state.getEntity(entityId)?.get<ControllerComponent>()?.playerId
        } ?: return Side.NEITHER
        return when {
            controller == playerId || controller in state.teamOf(playerId) -> Side.OURS
            state.isOpponentTo(controller, playerId) -> Side.THEIRS
            else -> Side.NEITHER
        }
    }

    /**
     * For spells/abilities that require target selection, fill in heuristic
     * targets so the action can actually resolve.
     *
     * Pure stat-boosting activated abilities prefer high-value friendly creatures.
     * Other actions keep the removal-oriented ranking: high-value opposing creatures first.
     *
     * This is the cheap path — one heuristic target per requirement, no simulation. The action the
     * Strategist actually commits routes through `chooseCommittedTargets`, which refines it.
     */
    fun fillHeuristically(
        state: GameState,
        action: LegalAction,
        playerId: EntityId,
        fillPartialRequirements: Boolean,
        intents: IntentCatalog = IntentCatalog.NONE,
        /** See [ranker]'s parameter of the same name. */
        polarityFromEffect: Boolean = false,
    ): GameAction {
        action.modalEnumeration?.let {
            return fillModalHeuristically(state, action, playerId, intents)
        }
        if (!action.requiresTargets) return action.action
        // Only CastSpell and ActivateAbility carry a `targets` list the AI fills in. A targeted
        // activated ability (e.g. "{4}{R}, Sacrifice: deal 3 damage to target") that isn't handled
        // here is submitted with no target, rejected by the engine ("requires a target"), and the
        // AI re-picks it forever — an infinite loop.
        val baseAction = action.action
        if (targetsAlreadyFilled(baseAction) != false) return action.action
        val targetInfos = fillableRequirements(action, fillPartialRequirements) ?: return action.action

        val rankTarget = ranker(state, action, playerId, intents, polarityFromEffect)
        val slotPolarities = if (polarityFromEffect) polarities(state, action, intents) else emptyList()
        val chosenTargets = mutableListOf<ChosenTarget>()
        val chosenIds = mutableSetOf<EntityId>()
        // How many targets each slot took, kept so a slot left empty *by choice* ahead of a filled
        // one can be announced explicitly — see [applyTargets].
        val counts = mutableListOf<Int>()
        fun finish() = applyTargets(baseAction, chosenTargets, groupCountsFor(action, counts))
        for ((index, info) in targetInfos.withIndex()) {
            val candidates = info.validTargetsByPrefix?.get(chosenTargets.joinToString(",") { it.toEntityId().toString() })
                ?: if (info.validTargetsByPrefix == null) info.validTargets else emptyList()
            val available = if (info.mustDifferFromEarlier) {
                candidates.filterNot(chosenIds::contains)
            } else {
                candidates
            }
            // Nothing left for this slot. [fillableRequirements] guarantees `validTargets` is not
            // empty, so the only way here is an "other target" requirement (CR 601.2c) whose every
            // legal target was already spent on an earlier slot: Mabel's Mettle's "up to one *other*
            // target creature" with a single creature on the board. The enumerator cannot know which
            // target the earlier slot will take, so it offers that creature for both.
            //
            // Targets are submitted as one flat list sliced back by max counts, so only *trailing*
            // slots may be left empty — filling around a hole would silently re-attribute every
            // later target. When the rest is optional the prefix is a legal list; otherwise there is
            // no legal list at all and the action goes back unfilled, for the caller's simulation
            // (or the engine) to reject. Either beats `first()` on an empty list, which is what this
            // used to do — an `?: available.first()` that could only ever run when `available` was
            // empty, and so could only ever throw.
            val worth = worthTargeting(state, info, available, playerId, slotPolarities.getOrNull(index))
            if (worth.isEmpty() && available.isNotEmpty()) {
                // An optional slot whose every candidate is on the wrong side: leave it empty.
                counts += 0
                continue
            }
            val picks = pick(state, info, worth, rankTarget)
            if (picks.isEmpty()) {
                return if (targetInfos.drop(index).all { it.minTargets == 0 }) {
                    finish()
                } else {
                    action.action
                }
            }
            // A mandatory multi-target slot the board can't fill (two creatures controlled by
            // different players, with every creature under one player) has no legal list.
            if (picks.size < info.minTargets) return action.action
            picks.forEach { chosenTargets += toChosenTarget(state, info, it, playerId) }
            chosenIds += picks
            counts += picks.size
        }
        return finish()
    }

    /**
     * The candidates worth putting in [info]'s slot. A mandatory slot keeps them all — it has to
     * take *something*, and the ranker already orders the least bad first. An **optional** slot
     * ("up to one target creature", CR 601.2c lets it hold none) of known [polarity] drops every
     * candidate on the wrong side of the table: a +1/+1 counter is not put on an opponent's
     * creature, nor a "destroy" aimed at our own permanent, just because the slot is there. Empty
     * means the slot is best left empty — Tam's Resistance cast for its Empower Jace alone.
     */
    fun worthTargeting(
        state: GameState,
        info: TargetInfo,
        candidates: List<EntityId>,
        playerId: EntityId,
        polarity: TargetPolarity?,
    ): List<EntityId> {
        if (info.minTargets > 0) return candidates
        return when (polarity) {
            TargetPolarity.BENEFICIAL -> candidates.filterNot { isTheirs(state, it, playerId) }
            TargetPolarity.HARMFUL -> candidates.filterNot { isOurs(state, it, playerId) }
            TargetPolarity.UNKNOWN, null -> candidates
        }
    }

    /**
     * Explicit per-requirement target counts for [action] when [counts] leaves a slot empty ahead
     * of a filled one, else null (let the engine infer the split, as it always has). The engine
     * reads the flat target list greedily, earliest group fullest — so with "up to one target
     * creature gets +2/+2" skipped and "up to one target creature gets -2/-2" aimed at their
     * creature, an unannounced split would hand their creature the pump.
     */
    fun groupCountsFor(action: LegalAction, counts: List<Int>): List<Int>? {
        val hole = counts.withIndex().any { (i, n) -> n == 0 && counts.drop(i + 1).any { it > 0 } }
        if (!hole) return null
        val slots = targetInfosFor(action)?.size ?: return null
        return counts + List(maxOf(0, slots - counts.size)) { 0 }
    }

    private fun fillModalHeuristically(
        state: GameState,
        action: LegalAction,
        playerId: EntityId,
        intents: IntentCatalog,
    ): GameAction {
        val cast = action.action as? CastSpell ?: return action.action
        val modal = action.modalEnumeration ?: return cast
        val modes = modal.modes.filter { it.available }.take(modal.chooseCount)
        if (modes.size < modal.minChooseCount) return cast

        val orderedTargets = mutableListOf<List<ChosenTarget>>()
        for (mode in modes) {
            val chosen = mutableListOf<ChosenTarget>()
            val chosenIds = mutableSetOf<EntityId>()
            for (info in mode.targetRequirements) {
                val candidates = info.validTargetsByPrefix?.get(chosen.joinToString(",") { it.toEntityId().toString() })
                    ?: if (info.validTargetsByPrefix == null) info.validTargets else emptyList()
                val available = if (info.mustDifferFromEarlier) {
                    candidates.filterNot(chosenIds::contains)
                } else {
                    candidates
                }
                if (available.isEmpty() && info.minTargets == 0) continue
                val selectedId = available.maxByOrNull { rank(state, it, playerId, intents) }
                    ?: return cast
                chosen += toChosenTarget(state, info, selectedId, playerId)
                chosenIds += selectedId
            }
            orderedTargets += chosen
        }
        val withModes = cast.copy(
            chosenModes = modes.map { it.index },
            modeTargetsOrdered = orderedTargets,
            targets = orderedTargets.flatten(),
        )
        return payEscalateCost(state, withModes, modal, modes.size)
    }

    /**
     * Pay a non-mana escalate cost (CR 702.120a) for the modes just chosen — one cost per mode
     * beyond the first, so three modes on Collective Brutality discard two cards.
     *
     * The enumeration's cost data is for **one** extra mode; the cast handler validates the scaled
     * total, so the count has to be multiplied here. `modalEnumeration.chooseCount` is already
     * capped by what the caster can pay, so the candidate pool always covers the picks.
     */
    private fun payEscalateCost(
        state: GameState,
        cast: CastSpell,
        modal: ModalLegalEnumeration,
        chosenModeCount: Int,
    ): CastSpell {
        val info = modal.additionalCostPerExtraMode ?: return cast
        val extraModes = chosenModeCount - 1
        if (extraModes <= 0) return cast
        val existing = cast.additionalCostPayment ?: AdditionalCostPayment()
        val payment = when (info.costType) {
            // Prefer lands as discard fodder, mirroring the activated-ability discard heuristic.
            "DiscardCard" -> existing.copy(
                discardedCards = info.validDiscardTargets
                    .sortedByDescending { state.getEntity(it)?.get<CardComponent>()?.isLand == true }
                    .take(info.discardCount * extraModes)
            )
            "TapPermanents" -> existing.copy(
                tappedPermanents = info.validTapTargets.take(info.tapCount * extraModes)
            )
            "SacrificePermanent" -> existing.copy(
                sacrificedPermanents = info.validSacrificeTargets.take(info.sacrificeCount * extraModes)
            )
            "BouncePermanent" -> existing.copy(
                bouncedPermanents = info.validBounceTargets.take(info.bounceCount * extraModes)
            )
            "ExileFromGraveyard" -> existing.copy(
                exiledCards = info.validExileTargets.take(info.exileMinCount * extraModes)
            )
            // A cost shape with no picker never reaches here: the enumerator caps chooseCount at 1
            // for one, so extraModes is 0.
            else -> return cast
        }
        return cast.copy(additionalCostPayment = payment)
    }

    /**
     * Up to [count] of [ranked], best first, skipping any pick that would take the summed mana value
     * over [cap] ("... with total mana value 6 or less", Kairi, the Swirling Sky).
     */
    fun underManaValueCap(state: GameState, ranked: List<EntityId>, cap: Int, count: Int): List<EntityId> {
        val picks = mutableListOf<EntityId>()
        var total = 0
        for (candidate in ranked) {
            if (picks.size == count) break
            val manaValue = com.wingedsheep.engine.mechanics.ManaValues.of(state, candidate)
            if (total + manaValue > cap) continue
            picks += candidate
            total += manaValue
        }
        return picks
    }

    /**
     * The heuristic picks for one requirement: the best-ranked candidate, or — when the requirement
     * demands several ("two target creatures") — the best [TargetInfo.minTargets] of them, each
     * from a controller not already used when [TargetInfo.differentControllers] is set. Returns
     * fewer than required when [available] can't supply them; empty when it is empty.
     */
    fun pick(
        state: GameState,
        info: TargetInfo,
        available: List<EntityId>,
        rankTarget: (EntityId) -> Double,
    ): List<EntityId> {
        if (info.exclusiveGroups != null || info.distinctCategories != null) return com.wingedsheep.engine.core.constrainedTargetSelection(
            available, info.minTargets, minOf(info.maxTargets, maxOf(1, info.minTargets)), info.selectionGroups,
            info.exclusiveGroups, rankTarget, info.distinctCategories)
        val needed = maxOf(1, info.minTargets)
        val picks = mutableListOf<EntityId>()
        val controllers = mutableSetOf<EntityId>()
        val grouped = info.selectionGroups?.map { group -> available.filter { it in group } }
            ?.filter { it.size >= info.minTargets }
            ?.maxByOrNull { group -> group.sortedByDescending(rankTarget).take(needed).sumOf(rankTarget) }
        val pool = if (info.selectionGroups != null) grouped.orEmpty() else available
        for (candidate in pool.sortedByDescending(rankTarget)) {
            if (picks.size == needed) break
            if (info.differentControllers) {
                val controller = state.projectedState.getController(candidate)
                if (controller != null && !controllers.add(controller)) continue
            }
            picks += candidate
        }
        return picks
    }

    /**
     * Whether [baseAction]'s targets are already filled. `null` = the action type carries no
     * AI-filled target list (only CastSpell / ActivateAbility do), `true`/`false` otherwise.
     */
    fun targetsAlreadyFilled(baseAction: GameAction): Boolean? =
        when (baseAction) {
            is CastSpell -> baseAction.targets.isNotEmpty()
            is ActivateAbility -> baseAction.targets.isNotEmpty()
            else -> null
        }

    /**
     * The target requirements the AI will actually fill, or null when it cannot build a legal
     * target list at all and should leave the action's targets alone.
     *
     * Targets are submitted as one **flat** list, which the engine slices back into requirements
     * by their max counts (`TargetValidator.validateTargets`). An unfilled slot can therefore only
     * ever be a trailing one — there is no way to say "requirement 0 got nothing, requirement 1
     * got this".
     *
     * That mattered more than it sounds. V0 bails on the *whole spell* the moment any requirement
     * has no legal target, and then submits no targets at all, which the engine rejects with "No
     * valid targets available" — Phase 1 measured that as ~0.9 rejected actions per game, 889 of
     * 945 rejections. The shape behind almost all of them is an **optional** trailing slot:
     * Conduct Electricity's "up to one target creature token" with no token on the board makes the
     * AI decline to target the mandatory creature either.
     *
     * @param fillPartial the Phase 4a fix, gated by `AiProfile.useMeaningfulFilter` — not because
     *   the old behaviour is defensible, but because `AiProfile.LEGACY_V0` is the permanent
     *   reference opponent that every published number is quoted against, and quietly making it
     *   stronger would silently rebase months of arena results.
     */
    fun fillableRequirements(action: LegalAction, fillPartial: Boolean): List<TargetInfo>? {
        val all = targetInfosFor(action) ?: return null
        val fillable = all.takeWhile { it.validTargets.isNotEmpty() }
        if (fillable.size == all.size) return all
        if (!fillPartial) return null
        val unfilled = all.drop(fillable.size)
        // A mandatory slot with no legal target means the spell cannot be cast at all, and a
        // later slot that *does* have targets cannot be reached past a skipped one.
        if (unfilled.any { it.minTargets > 0 || it.validTargets.isNotEmpty() }) return null
        return fillable
    }

    /** Normalize an action's target metadata into requirements (multi-target or single-target). */
    fun targetInfosFor(action: LegalAction): List<TargetInfo>? =
        action.targetRequirements
            ?: action.validTargets?.let { targets ->
                listOf(
                    TargetInfo(
                        index = 0,
                        description = action.targetDescription ?: "",
                        minTargets = action.minTargets,
                        maxTargets = action.targetCount,
                        validTargets = targets,
                        targetZone = null
                    )
                )
            }

    /** Build the right [ChosenTarget] variant for [entityId] given the requirement's zone. */
    fun toChosenTarget(
        state: GameState,
        info: TargetInfo,
        entityId: EntityId,
        playerId: EntityId
    ): ChosenTarget = when (info.targetZone) {
        "GRAVEYARD" -> {
            val ownerId = state.getEntity(entityId)?.get<OwnerComponent>()?.playerId ?: playerId
            ChosenTarget.Card(entityId, ownerId, Zone.GRAVEYARD)
        }
        "STACK" -> ChosenTarget.Spell(entityId)
        else -> {
            // `targetZone` is only populated for multi-requirement spells; a single-target
            // spell/ability (Reprieve, or Sandman's "target land card from your graveyard")
            // surfaces `validTargets` with `targetZone = null`. So fall back to authoritative
            // game state and build the variant the target's actual zone demands:
            //  - a spell on the stack must become a `ChosenTarget.Spell`, not a `Permanent`
            //    (else the engine rejects the cast, "Target must be a spell on the stack");
            //  - a card in a non-battlefield zone (graveyard/exile/hand/library/command) must
            //    become a `ChosenTarget.Card` carrying that zone, not a `Permanent` (else the
            //    engine rejects it — e.g. Sandman's graveyard land — and the AI re-picks the
            //    same failing activation forever).
            // Mirrors the web client's target-payload builder (pipelinePhases.ts).
            val isSpell = state.isSpellOnStack(entityId)
            val isPlayer = state.getEntity(entityId)?.get<PlayerComponent>() != null
            val cardZone = zoneOfCardTarget(state, entityId)
            when {
                isSpell -> ChosenTarget.Spell(entityId)
                isPlayer -> ChosenTarget.Player(entityId)
                cardZone != null -> {
                    val ownerId = state.getEntity(entityId)?.get<OwnerComponent>()?.playerId
                        ?: cardZone.ownerId
                    ChosenTarget.Card(entityId, ownerId, cardZone.zoneType)
                }
                else -> ChosenTarget.Permanent(entityId)
            }
        }
    }

    /**
     * The [ZoneKey] of [entityId] when it is a card in a non-battlefield "card target" zone
     * (graveyard, exile, hand, library, command) — the zones a `ChosenTarget.Card` addresses.
     * Returns `null` for battlefield permanents and the stack, which are handled by the
     * `Permanent`/`Spell` variants. Mirrors the client's `CARD_TARGET_ZONES` set.
     */
    private fun zoneOfCardTarget(state: GameState, entityId: EntityId): ZoneKey? {
        val key = state.zones.entries.firstOrNull { entityId in it.value }?.key ?: return null
        return when (key.zoneType) {
            Zone.GRAVEYARD, Zone.EXILE, Zone.HAND, Zone.LIBRARY, Zone.COMMAND -> key
            else -> null
        }
    }

    /**
     * Return [baseAction] with its target list replaced, and — when [groupCounts] is given — the
     * per-requirement split announced with it ([CastSpell.targetGroupCounts]).
     */
    fun applyTargets(
        baseAction: GameAction,
        targets: List<ChosenTarget>,
        groupCounts: List<Int>? = null,
    ): GameAction =
        when (baseAction) {
            is CastSpell -> if (groupCounts == null) baseAction.copy(targets = targets)
                else baseAction.copy(targets = targets, targetGroupCounts = groupCounts)
            is ActivateAbility -> if (groupCounts == null) baseAction.copy(targets = targets)
                else baseAction.copy(targets = targets, targetGroupCounts = groupCounts)
            else -> baseAction
        }
}
