package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.ModalLegalEnumeration
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
            // Own creatures: higher value = better target for pump/bite source
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
     * For spells/abilities that require target selection, fill in heuristic
     * targets so the action can actually resolve.
     *
     * Multi-target spells: for each requirement, pick the highest-value
     * opponent creature (or lowest-value own creature, depending on context).
     * Single-target spells: pick the best target by creature value.
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

        val chosenTargets = mutableListOf<ChosenTarget>()
        val chosenIds = mutableSetOf<EntityId>()
        for ((index, info) in targetInfos.withIndex()) {
            val available = if (info.mustDifferFromEarlier) {
                info.validTargets.filterNot(chosenIds::contains)
            } else {
                info.validTargets
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
            val selectedId = available.maxByOrNull { rank(state, it, playerId, intents) }
                ?: return if (targetInfos.drop(index).all { it.minTargets == 0 }) {
                    applyTargets(baseAction, chosenTargets)
                } else {
                    action.action
                }
            chosenTargets += toChosenTarget(state, info, selectedId, playerId)
            chosenIds += selectedId
        }
        return applyTargets(baseAction, chosenTargets)
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
                val available = if (info.mustDifferFromEarlier) {
                    info.validTargets.filterNot(chosenIds::contains)
                } else {
                    info.validTargets
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

    /** Return [baseAction] with its target list replaced. */
    fun applyTargets(baseAction: GameAction, targets: List<ChosenTarget>): GameAction =
        when (baseAction) {
            is CastSpell -> baseAction.copy(targets = targets)
            is ActivateAbility -> baseAction.copy(targets = targets)
            else -> baseAction
        }
}
