package com.wingedsheep.engine.core

import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.PrintingRef
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Builds the per-entity [ComponentContainer] for a card from its [CardDefinition].
 *
 * Extracted from [GameInitializer] so the same characteristic-copying logic (identity, owner,
 * controller, plus the keyword-derived [ProtectionComponent] / [HasMorphAbilityComponent] /
 * [HexproofFromColorComponent] / [ToxicComponent] decorations) is shared between game setup
 * (libraries / command zone) and minting a token from a bare definition (Momir's random-creature
 * copy — see [com.wingedsheep.engine.handlers.effects.token.TokenFromDefinition]). Keeping one
 * builder means a definition-derived component added here is never silently missing on a minted
 * token.
 */
object CardEntityFactory {

    /**
     * @param printingRef When non-null and resolvable in [printingRegistry], the per-entity
     *   [CardComponent] image URIs and definition id are taken from the chosen printing rather
     *   than the canonical [CardDefinition.metadata]. Null (the common case, and always for tokens)
     *   uses the canonical metadata.
     */
    fun create(
        cardDef: CardDefinition,
        ownerId: EntityId,
        printingRef: PrintingRef? = null,
        printingRegistry: PrintingRegistry? = null,
    ): ComponentContainer {
        val protections = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Protection>()
        val protectionColors = protections.flatMap { p ->
            when (val s = p.scope) {
                is ProtectionScope.Color -> listOf(s.color)
                is ProtectionScope.Colors -> s.colors
                else -> emptyList()
            }
        }.toSet()
        val protectionSubtypes = protections.mapNotNull {
            (it.scope as? ProtectionScope.Subtype)?.subtype
        }.toSet()
        val protectionSupertypes = protections.mapNotNull {
            (it.scope as? ProtectionScope.Supertype)?.supertype
        }.toSet()

        // Resolve the chosen printing (if pinned by the deck entry) so we can stamp the
        // printing's art onto the per-entity CardComponent. When no override resolves, we
        // fall back to the canonical CardDefinition metadata — the legacy behaviour.
        val printing = printingRef?.let { printingRegistry?.getPrinting(it) }

        // Use Name#SetCode-CollectorNumber as the definition ID when available, so that
        // cards with the same name but different art variants (basic lands across sets)
        // resolve back to the correct CardDefinition via CardRegistry.
        // SetCode is included to avoid collisions between sets that share collector numbers
        // (e.g., Khans and Dominaria both use 250-269 for basic lands). Honour the chosen
        // printing's set/CN when one was pinned — this keeps cardDefinitionId stable for
        // copy/clone effects that round-trip through CardRegistry.
        val effectiveSetCode = printing?.setCode ?: cardDef.setCode
        val effectiveCollectorNumber = printing?.collectorNumber ?: cardDef.metadata.collectorNumber
        val definitionId = effectiveCollectorNumber?.let { cn ->
            if (effectiveSetCode != null) "${cardDef.name}#$effectiveSetCode-$cn"
            else "${cardDef.name}#$cn"
        } ?: cardDef.name

        var container = ComponentContainer.of(
            CardComponent(
                cardDefinitionId = definitionId,
                name = cardDef.name,
                manaCost = cardDef.manaCost,
                typeLine = cardDef.typeLine,
                oracleText = cardDef.oracleText,
                baseStats = cardDef.creatureStats,
                baseKeywords = cardDef.keywords,
                baseFlags = cardDef.flags,
                colors = cardDef.colors,
                ownerId = ownerId,
                spellEffect = cardDef.spellEffect,
                imageUri = printing?.imageUri ?: cardDef.metadata.imageUri,
                backFaceImageUri = printing?.backFaceImageUri
                    ?: cardDef.backFace?.metadata?.imageUri
                    // Modal DFC backs aren't a separate CardDefinition; their art rides on the face.
                    ?: cardDef.cardFaces.firstOrNull { it.imageUri != null }?.imageUri,
                hasNonManaActivatedAbility = cardDef.hasNonManaActivatedAbility,
            ),
            OwnerComponent(ownerId),
            ControllerComponent(ownerId)
        )

        if (cardDef.script.cantBeCountered) {
            container = container.with(CantBeCounteredComponent)
        }

        if (cardDef.script.cantBeCopied) {
            container = container.with(com.wingedsheep.engine.state.components.identity.CantBeCopiedComponent)
        }

        if (cardDef.keywordAbilities.any { it is KeywordAbility.Morph }) {
            container = container.with(HasMorphAbilityComponent)
        }

        if (protectionColors.isNotEmpty() || protectionSubtypes.isNotEmpty() || protectionSupertypes.isNotEmpty()) {
            container = container.with(ProtectionComponent(protectionColors, protectionSubtypes, protectionSupertypes))
        }

        val hexproofFromColors = cardDef.keywordAbilities
            .filterIsInstance<KeywordAbility.Hexproof>()
            .mapNotNull { (it.scope as? ProtectionScope.Color)?.color }
            .toSet()
        if (hexproofFromColors.isNotEmpty()) {
            container = container.with(HexproofFromColorComponent(hexproofFromColors))
        }

        // Toxic N (702.164). Multiple instances stack per Rule 702.164b — sum across
        // any printed Toxic abilities so the projector can emit a single TOXIC_<n>.
        val toxicAmount = cardDef.keywordAbilities
            .filterIsInstance<KeywordAbility.Numeric>()
            .filter { it.keyword == Keyword.TOXIC }
            .sumOf { it.n }
        if (toxicAmount > 0) {
            container = container.with(ToxicComponent(toxicAmount))
        }

        return container
    }
}
