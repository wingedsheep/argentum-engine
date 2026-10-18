package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.PutOntoBattlefieldAttachedToChosenContinuation
import com.wingedsheep.engine.core.TargetRequirementInfo
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.ChooserResolution
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.handlers.predicates.EnchantRestriction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.PutOntoBattlefieldAttachedToChosenEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import kotlin.reflect.KClass

/**
 * Executor for [PutOntoBattlefieldAttachedToChosenEffect].
 *
 * Puts a targeted Aura or Equipment card onto the battlefield attached to a host the
 * controller chooses at resolution. Models "Return target Aura or Equipment card from your
 * graveyard to the battlefield attached to a creature you control" (One Last Job mode 3).
 *
 * Flow:
 *  1. Resolve the targeted card (the Aura/Equipment). If it isn't an Aura or Equipment, no-op.
 *  2. Enumerate legal hosts: permanents matching the effect's [hostFilter]. For Auras the host
 *     set is intersected with the Aura's own enchant legality ([CardScript.auraTarget], Rule
 *     303.4f — targeting restrictions like hexproof/shroud are ignored), so the Aura can only
 *     be put onto something it can legally enchant.
 *  3. If a legal host exists, pause for the effect's chooser (the controller by default) to choose one
 *     ([ChooseTargetsDecision]) and resume via
 *     [PutOntoBattlefieldAttachedToChosenContinuation] to move-and-attach.
 *  4. If no legal host exists: an Equipment still enters the battlefield (unattached); an Aura
 *     can't enter and stays in its current zone (Rule 303.4g). This matches the One Last Job
 *     ruling exactly.
 *
 * The actual move-and-attach reuses [com.wingedsheep.engine.handlers.effects.library.MoveCollectionExecutor.moveAuraToBattlefield],
 * which is permanent-agnostic (it wires controller, `AttachedToComponent`, the host's
 * `AttachmentsComponent`, and the card's static/replacement effects) and therefore serves both
 * Auras and Equipment.
 */
class PutOntoBattlefieldAttachedToChosenExecutor(
    private val zones: ZoneTransitionService,
    private val cardRegistry: CardRegistry,
    private val targetFinder: TargetFinder
) : EffectExecutor<PutOntoBattlefieldAttachedToChosenEffect> {

    override val effectType: KClass<PutOntoBattlefieldAttachedToChosenEffect> =
        PutOntoBattlefieldAttachedToChosenEffect::class

    override fun execute(
        state: GameState,
        effect: PutOntoBattlefieldAttachedToChosenEffect,
        context: EffectContext
    ): EffectResult {
        val cardId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state) // target gone — fizzles for this mode

        val container = state.getEntity(cardId) ?: return EffectResult.success(state)
        val cardComponent = container.get<CardComponent>()
            ?: return EffectResult.success(state)

        val isAura = cardComponent.typeLine.isAura
        val isEquipment = cardComponent.typeLine.isEquipment
        if (!isAura && !isEquipment) {
            // Not an Aura or Equipment — nothing this mode can do.
            return EffectResult.success(state)
        }

        val controllerId = context.controllerId

        // Hosts that match the effect's host filter (e.g. "a creature you control").
        val hostRequirement = TargetObject(filter = TargetFilter(baseFilter = effect.hostFilter))
        var legalHosts = targetFinder.findLegalTargets(
            state = state,
            requirement = hostRequirement,
            controllerId = controllerId,
            sourceId = cardId,
            ignoreTargetingRestrictions = true
        )

        // For an Aura, narrow to hosts it can legally enchant (Rule 303.4f).
        if (isAura) {
            val auraTarget = cardRegistry.getCard(cardComponent.cardDefinitionId)?.script?.auraTarget
            if (auraTarget != null) {
                val auraLegal = targetFinder.findLegalTargets(
                    state = state,
                    requirement = auraTarget,
                    controllerId = controllerId,
                    sourceId = cardId,
                    ignoreTargetingRestrictions = true
                ).toSet()
                // Untargeted, so hexproof/shroud don't matter — but what the Aura can't legally
                // enchant still rules a host out: "can't be enchanted" and protection from the
                // Aura's qualities (CR 303.4f; Takklemaggot's ruling on protection from black).
                val projected = state.projectedState
                legalHosts = legalHosts.filter { host ->
                    host in auraLegal &&
                        EnchantRestriction.hostAllowsAura(state, projected, zones.predicateEvaluator, cardId, host) &&
                        !EnchantRestriction.hostProtectedFromAttachment(state, projected, cardRegistry, cardId, cardComponent, host)
                }
            } else {
                // No enchant target defined — the Aura can't be attached to anything.
                legalHosts = emptyList()
            }
        }

        if (legalHosts.isEmpty()) {
            // No legal host. Equipment still enters the battlefield (unattached, Rule 301.5c);
            // an Aura can't enter and stays in its current zone (Rule 303.4g).
            return if (isEquipment) {
                val fromZone = findCurrentZone(state, cardId)
                    ?: return EffectResult.success(state)
                val transition = zones.moveToZone(
                    state, cardId, Zone.BATTLEFIELD,
                    ZoneEntryOptions(controllerId = controllerId),
                    fromZone
                )
                EffectResult.success(transition.state, transition.events)
            } else {
                EffectResult.success(state)
            }
        }

        // Pause for the chooser (the controller unless the effect names someone else) to pick a
        // host. Whoever picks, the card enters under the effect controller's control.
        val chooserId = when (val outcome = ChooserResolution.resolve(state, effect.chooser, context)) {
            is ChooserResolution.Outcome.Resolved -> outcome.playerId
            is ChooserResolution.Outcome.NeedsOpponentPick -> return ChooserResolution.pauseForOpponentPick(
                state, outcome.opponents, effect, context,
                prompt = "Choose which opponent chooses what ${cardComponent.name} attaches to"
            )
            is ChooserResolution.Outcome.Unresolvable ->
                return EffectResult.error(state, "PutOntoBattlefieldAttachedToChosen chooser: ${outcome.reason}")
        }
        val cardName = cardComponent.name
        val requirementInfo = TargetRequirementInfo(
            index = 0,
            // Not a target: say what the pick is for ("creature for Takklemaggot to attach to").
            description = "${effect.hostFilter.description} for $cardName to attach to",
            minTargets = 1,
            maxTargets = 1
        )
        val decision = { decisionId: String -> ChooseTargetsDecision(
            id = decisionId,
            playerId = chooserId,
            prompt = "Choose what $cardName attaches to",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
                phase = DecisionPhase.RESOLUTION
            ),
            targetRequirements = listOf(requirementInfo),
            legalTargets = mapOf(0 to legalHosts)
        ) }

        val continuation = PutOntoBattlefieldAttachedToChosenContinuation(
            cardId = cardId,
            controllerId = controllerId
        )

        return EffectResult.from(state.suspendForDecision(decision, continuation, emptyList()))
    }

    private fun findCurrentZone(state: GameState, entityId: com.wingedsheep.sdk.model.EntityId): ZoneKey? {
        for ((zoneKey, entities) in state.zones) {
            if (entityId in entities) return zoneKey
        }
        return null
    }
}
