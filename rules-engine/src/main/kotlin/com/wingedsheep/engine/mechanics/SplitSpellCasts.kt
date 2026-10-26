package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * The off-stack characteristics restored when a split half, Adventure, Omen or modal DFC spell back
 * leaves the stack.
 */
@Serializable
data class SplitSpellCharacteristicsComponent(val original: CardComponent) : Component

/** Single-half and fused split spells share the ordinary cast, payment and target-validation rails. */
object SplitSpellCasts {
    /** Selection-pipeline face key; normal faces are nonnegative, primary/back use -1/-2. */
    const val FUSED_FACE = -3
    /** Fuse (CR 702.102) is declared once, on the split card; a fused spell combines both halves. */
    fun hasFuse(definition: CardDefinition?): Boolean = definition?.isSplit == true &&
        definition.cardFaces.size == 2 && definition.cardFaces.none { it.typeLine.isPermanent } &&
        Keyword.FUSE in definition.keywords

    fun choiceFailure(state: GameState, definition: CardDefinition?, action: CastSpell): String? {
        if (action.castFused) {
            if (!hasFuse(definition)) return "This card has no fuse ability"
            if (action.cardId !in state.getHand(action.playerId)) return "Fuse can only be used from your hand"
            if (action.faceIndex != null || action.castFaceDown || action.castPrototyped) return "Choose either one half or fuse"
        } else if (definition?.isSplit == true && action.faceIndex !in definition.cardFaces.indices) {
            return "Choose a split half to cast"
        }
        return null
    }

    fun definitionForCast(definition: CardDefinition?, action: CastSpell): CardDefinition? {
        if (definition?.isSplit != true) return definition
        if (action.castFused && hasFuse(definition)) return fusedDefinition(definition)
        val face = action.faceIndex?.let { definition.cardFaces.getOrNull(it) } ?: return definition
        if (face.typeLine.isPermanent) return definition
        return definition.copy(manaCost = face.manaCost, typeLine = face.typeLine, keywords = face.keywords,
            oracleText = face.oracleText, script = face.script, hasNoManaCost = false)
    }

    fun fusedDefinition(definition: CardDefinition): CardDefinition = definition.copy(
        manaCost = definition.cardFaces.fold(ManaCost.ZERO) { cost, face -> cost + face.manaCost },
        typeLine = definition.typeLine.copy(
            cardTypes = definition.cardFaces.flatMap { it.typeLine.cardTypes }.toSet(),
            subtypes = definition.cardFaces.flatMap { it.typeLine.subtypes }.toSet(),
            supertypes = definition.cardFaces.flatMap { it.typeLine.supertypes }.toSet(),
        ),
        keywords = definition.keywords + definition.cardFaces.flatMap { it.keywords },
        hasNoManaCost = false,
        script = definition.script.copy(
            targetRequirements = definition.cardFaces.flatMap { it.script.targetRequirements },
            additionalCosts = definition.cardFaces.flatMap { it.script.additionalCosts },
        ),
    )

    fun announce(state: GameState, action: CastSpell, registry: CardRegistry): GameState {
        val container = state.getEntity(action.cardId) ?: return state
        if (container.has<SplitSpellCharacteristicsComponent>()) return state
        val original = container.get<CardComponent>() ?: return state
        val printed = registry.getCard(original.cardDefinitionId) ?: return state
        val spell = definitionForCast(printed, action)?.takeIf { it !== printed }
            ?: alternativeFaceDefinition(printed, action) ?: return state
        return state.updateEntity(action.cardId) {
            it.with(SplitSpellCharacteristicsComponent(original)).with(original.copy(
                manaCost = spell.manaCost, typeLine = spell.typeLine, colors = spell.colors,
                baseKeywords = spell.keywords, oracleText = spell.oracleText, spellEffect = spell.script.spellEffect,
                manaValueOverride = null,
            ))
        }
    }

    /**
     * An Adventure (CR 715.3b), an Omen (CR 720.3b) or a modal double-faced card's spell back
     * (CR 712.8f) is on the stack with only that face's characteristics — the face's mana cost, so
     * its mana value, plus its type line, colors and keywords. The cast rails already evaluate the
     * face on their own (CR 715.3a / 720.3a), so this only reshapes the stack object, the way a
     * split half is reshaped; [end] restores the card when it leaves the stack. The card keeps its
     * own name — a simplification of the rule, which name-keyed trackers rely on. Null for any
     * other cast.
     */
    private fun alternativeFaceDefinition(printed: CardDefinition, action: CastSpell): CardDefinition? {
        if (printed.layout !in ALTERNATIVE_FACE_LAYOUTS) return null
        val face = action.faceIndex?.let { printed.cardFaces.getOrNull(it) } ?: return null
        if (face.typeLine.isPermanent) return null
        return printed.copy(manaCost = face.manaCost, typeLine = face.typeLine, keywords = face.keywords,
            oracleText = face.oracleText, script = face.script, colorIndicator = null, hasNoManaCost = false)
    }

    private val ALTERNATIVE_FACE_LAYOUTS = setOf(CardLayout.ADVENTURE, CardLayout.OMEN, CardLayout.MODAL_DFC)

    fun end(state: GameState, id: EntityId): GameState {
        val original = state.getEntity(id)?.get<SplitSpellCharacteristicsComponent>()?.original ?: return state
        return state.updateEntity(id) { it.with(original).without<SplitSpellCharacteristicsComponent>() }
    }
}
