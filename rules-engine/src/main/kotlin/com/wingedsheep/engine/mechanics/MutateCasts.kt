package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.MutatedEvent
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.effects.permanent.types.withFaceIntrinsicComponents
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.MergedCardDefinitions
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import kotlinx.serialization.Serializable

/**
 * Marks a **merged permanent** (CR 730.2) — one object represented by several cards or tokens,
 * made by a mutating creature spell (CR 702.140c). The permanent is the entity of the creature that
 * was mutated onto ([hostCard]'s); every other component is held in no zone while merged, like a
 * meld partner ([com.wingedsheep.engine.state.components.identity.MeldedComponent]).
 *
 * The permanent's [CardComponent] is the topmost component's, naming a composite definition id
 * ([MergedCardDefinitions]) so it has every component's abilities (CR 702.140e). Built once per
 * merge, it is the permanent's copiable value (CR 730.2a). When the permanent leaves the
 * battlefield, [com.wingedsheep.engine.handlers.effects.ZoneTransitionService] turns the host back
 * into [hostCard] and puts every other component into the same zone (CR 730.3).
 *
 * @property componentIds Every component, topmost first; the host's own entity is one of them.
 * @property tokenIds The components that are tokens. The permanent is a token only while its
 *   topmost component is one (CR 730.2d).
 * @property hostFace The host's characteristics when it was first merged with — its place in the
 *   pile when a later mutate re-derives the permanent.
 * @property hostCard The host's own printed card, what it is again once it leaves.
 */
@Serializable
data class MergedComponent(
    val componentIds: List<EntityId>,
    val tokenIds: Set<EntityId>,
    val hostFace: CardComponent,
    val hostCard: CardComponent,
) : Component {
    fun partnerIds(hostId: EntityId): List<EntityId> = componentIds.filter { it != hostId }
}

/** Mutate's announcement, merge and separation (CR 702.140, 730). */
object MutateCasts {
    /** "a non-Human creature with the same owner as this spell" (CR 702.140a). */
    val target = TargetObject(
        filter = TargetFilter(
            GameObjectFilter.Creature.notSubtype(Subtype.HUMAN)
                .ownedBy(EffectTarget.PlayerRef(Player.OwnerOfSource))
        )
    )

    fun mutateOf(definition: CardDefinition?): KeywordAbility.Mutate? =
        definition?.keywordAbilities?.firstNotNullOfOrNull { it as? KeywordAbility.Mutate }

    fun selected(action: CastSpell) =
        action.useAlternativeCost && action.alternativeCostType == AlternativeCostType.MUTATE

    /** A mutating creature spell targets the creature it will merge with (CR 702.140a). */
    fun definitionForCast(definition: CardDefinition?, action: CastSpell): CardDefinition? =
        if (selected(action) && mutateOf(definition) != null) {
            definition!!.copy(script = definition.script.copy(targetRequirements = listOf(target)))
        } else definition

    /**
     * The creature a resolving spell mutates onto: a spell cast for its mutate cost that still has
     * its target. Null once it has ceased to be a mutating spell (CR 702.140b drops the target).
     */
    fun mutatingTarget(container: ComponentContainer): EntityId? {
        if (container.get<SpellOnStackComponent>()?.alternativeCost != AlternativeCostType.MUTATE) return null
        return (container.get<TargetsComponent>()?.targets?.singleOrNull() as? ChosenTarget.Permanent)?.entityId
    }

    /** "Put it over or under?" — yes is over (CR 702.140c: the spell's controller chooses). */
    fun question(decisionId: String, spellId: EntityId, spellName: String, targetName: String, controllerId: EntityId) =
        YesNoDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Mutate $spellName onto $targetName",
            context = DecisionContext(sourceId = spellId, sourceName = spellName, phase = DecisionPhase.RESOLUTION),
            yesText = "Over",
            noText = "Under",
            hint = "On top, the creature becomes $spellName; underneath, it keeps its own characteristics. " +
                "Either way it gains every ability of both.",
            peerOptions = true,
            yesDetail = "Becomes $spellName",
            noDetail = "Stays $targetName",
        )

    /**
     * Merge the resolving spell [spellId] with [hostId] (CR 702.140c, 730.2): the spell leaves the
     * stack without entering the battlefield, and the host — the same object it was (CR 730.2c) —
     * takes the topmost component's characteristics and every component's abilities.
     */
    fun merge(
        state: GameState,
        registry: CardRegistry,
        spellId: EntityId,
        hostId: EntityId,
        onTop: Boolean,
    ): Pair<GameState, MutatedEvent> {
        val host = state.getEntity(hostId)!!
        val spell = state.getEntity(spellId)!!
        val controllerId = spell.get<SpellOnStackComponent>()!!.casterId
        val existing = host.get<MergedComponent>()
        val hostNow = host.get<CardComponent>()!!
        val previous = existing ?: MergedComponent(
            componentIds = listOf(hostId),
            tokenIds = if (host.has<TokenComponent>()) setOf(hostId) else emptySet(),
            hostFace = hostNow,
            hostCard = host.get<CopyOfComponent>()?.originalCardComponent ?: hostNow,
        )
        // A copy of a mutating spell is a token once it's part of a permanent (CR 707.10f, 730.2d).
        val spellIsToken = spell.has<CopyOfComponent>()
        val merged = previous.copy(
            componentIds = if (onTop) listOf(spellId) + previous.componentIds else previous.componentIds + spellId,
            tokenIds = if (spellIsToken) previous.tokenIds + spellId else previous.tokenIds,
        )

        var newState = state.removeFromStack(spellId).updateEntity(spellId) {
            it.without<SpellOnStackComponent>().without<TargetsComponent>().without<TextReplacementComponent>()
        }
        val card = mergedCard(newState, hostId, merged)
        val definition = registry.getCard(card.cardDefinitionId)
        newState = newState.updateEntity(hostId) { c ->
            var updated = c.with(card).with(merged)
            updated = if (merged.componentIds.first() in merged.tokenIds) updated.with(TokenComponent) else updated.without<TokenComponent>()
            // A face-up card on top makes the permanent face up; nothing sees it "turned face up" (CR 730.2e).
            if (onTop) updated = updated.without<FaceDownComponent>()
            if (definition != null) withFaceIntrinsicComponents(updated, definition) else updated
        }
        val spellName = spell.get<CardComponent>()?.name ?: "Unknown"
        return newState to MutatedEvent(hostId, spellId, controllerId, spellName,
            com.wingedsheep.engine.state.nameVisibleToAll(state, hostId, hostNow.name), onTop)
    }

    /**
     * The permanent's [CardComponent]: the topmost component's characteristics (CR 730.2a) naming
     * the composite definition that carries every component's abilities (CR 702.140e).
     */
    private fun mergedCard(state: GameState, hostId: EntityId, merged: MergedComponent): CardComponent {
        val cards = merged.componentIds.map { id ->
            if (id == hostId) merged.hostFace else state.getEntity(id)!!.get<CardComponent>()!!
        }
        val top = cards.first()
        return top.copy(
            cardDefinitionId = MergedCardDefinitions.compose(cards.map { it.cardDefinitionId }),
            oracleText = mergedOracleText(cards),
            baseKeywords = cards.flatMapTo(linkedSetOf()) { it.baseKeywords },
            baseFlags = cards.flatMapTo(linkedSetOf()) { it.baseFlags },
            hasNonManaActivatedAbility = cards.any { it.hasNonManaActivatedAbility },
            hasActivatedAbility = cards.any { it.hasActivatedAbility },
            ownerId = merged.hostCard.ownerId ?: top.ownerId,
        )
    }

    /** The topmost card's text, then each other component's, so every ability the permanent has reads. */
    private fun mergedOracleText(cards: List<CardComponent>): String = buildString {
        append(cards.first().oracleText)
        for (card in cards.drop(1)) {
            if (isNotEmpty()) append("\n\n")
            append("Mutated with ${card.name}")
            if (card.oracleText.isNotBlank()) append(":\n${card.oracleText}")
        }
    }

    /**
     * Undo a merge on the host as the permanent leaves the battlefield: it is its own printed card
     * again (a token only if it was one), and the other components are the caller's to place —
     * [com.wingedsheep.engine.handlers.effects.ZoneTransitionService] puts each into the zone the
     * permanent went to (CR 730.3).
     */
    fun separate(state: GameState, registry: CardRegistry, hostId: EntityId, merged: MergedComponent): GameState {
        val definition = registry.getCard(merged.hostCard.cardDefinitionId)
        var newState = state.updateEntity(hostId) { c ->
            var reverted = c.with(merged.hostCard).without<MergedComponent>()
            reverted = if (hostId in merged.tokenIds) reverted.with(TokenComponent) else reverted.without<TokenComponent>()
            if (definition != null) withFaceIntrinsicComponents(reverted, definition) else reverted
        }
        for (id in merged.tokenIds) {
            if (id != hostId) newState = newState.updateEntity(id) { it.with(TokenComponent) }
        }
        return newState
    }
}
