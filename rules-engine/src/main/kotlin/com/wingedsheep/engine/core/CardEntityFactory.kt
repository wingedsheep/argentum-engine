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
import com.wingedsheep.sdk.scripting.RedirectZoneChange

/**
 * Builds the per-entity [ComponentContainer] for a card from its [CardDefinition].
 *
 * Extracted from [GameInitializer] so the same characteristic-copying logic (identity, owner,
 * controller, plus the keyword-derived [ProtectionComponent] / [HasMorphAbilityComponent] /
 * [HexproofFromComponent] / [ToxicComponent] decorations) is shared between game setup
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
        // Resolve the chosen printing (if pinned by the deck entry) so we can stamp the
        // printing's art onto the per-entity CardComponent. When no override resolves, we
        // fall back to the canonical CardDefinition metadata — the legacy behaviour.
        val printing = printingRef?.let { printingRegistry?.getPrinting(it) }

        // Use Name#SetCode-CollectorNumber as the definition ID when available, so that
        // cards with the same name but different art variants (basic lands across sets)
        // resolve back to the correct CardDefinition via CardRegistry.
        // SetCode is included to avoid collisions between sets that share collector numbers
        // (e.g., Khans and Dominaria both use 250-269 for basic lands). Keep the oracle
        // definition's registered identity: a PrintingRef changes presentation only. Using the
        // chosen reprint's coordinates here would make ability lookups miss when its canonical
        // CardDefinition lives in another set.
        val effectiveSetCode = cardDef.setCode
        val effectiveCollectorNumber = cardDef.metadata.collectorNumber
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
                // The set whose art this entity presents. `definitionId` above keeps the *oracle*
                // definition's set on purpose, so this is the only place the pinned reprint's set
                // survives onto the entity — and it's what makes a reprint mint its own set's tokens.
                printingSetCode = printing?.setCode ?: cardDef.setCode,
                hasNonManaActivatedAbility = cardDef.hasNonManaActivatedAbility,
                hasActivatedAbility = cardDef.hasActivatedAbility,
                // Original-printing set (canonical, not the pinned printing) — "originally printed in X".
                originalSetCode = cardDef.setCode,
                hasAdventure = cardDef.isAdventure,
            ),
            OwnerComponent(ownerId),
            ControllerComponent(ownerId)
        )

        return applyDefinitionDecorations(container, cardDef)
    }

    /**
     * Attach every component derived purely from a [CardDefinition]'s printed characteristics —
     * "can't be countered/copied", morph, protection, card-intrinsic self-redirects, hexproof-from,
     * Toxic N.
     *
     * Split out of [create] because several places mint card entities without going through it:
     * the scenario builders (`ScenarioTestBase`, `ScenarioBuilderService`, `GameTestDriver`) build
     * their own `CardComponent` for a hand/battlefield card. They all call this so a decoration
     * added here can never be silently missing on one of those paths — which would show up as a
     * card quietly losing an ability only in scenario tests or the scenario editor.
     */
    fun applyDefinitionDecorations(
        container: ComponentContainer,
        cardDef: CardDefinition
    ): ComponentContainer {
        var result = container

        if (cardDef.script.cantBeCountered) {
            result = result.with(CantBeCounteredComponent)
        }

        if (cardDef.script.cantBeCopied) {
            result = result.with(com.wingedsheep.engine.state.components.identity.CantBeCopiedComponent)
        }

        if (cardDef.keywordAbilities.any { it is KeywordAbility.Morph }) {
            result = result.with(HasMorphAbilityComponent)
        }

        // Madness (CR 702.35a). The static half functions while the card is in a player's *hand*,
        // so the cost has to ride the card entity in every zone rather than be looked up from the
        // battlefield — see [MadnessComponent].
        (cardDef.keywordAbilities.firstOrNull { it is KeywordAbility.Madness } as? KeywordAbility.Madness)
            ?.let { result = result.with(MadnessComponent(it.cost)) }

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
        // "Protection from instants" (Emrakul, the Promised End). Normalized to the uppercase card
        // type name here so the projector can emit `PROTECTION_FROM_CARDTYPE_<TYPE>` — the same
        // keyword the *granted* card-type protections (Sword of Wealth and Power, Pippin) project,
        // so every consumer downstream already honors it.
        val protectionCardTypes = protections.mapNotNull {
            (it.scope as? ProtectionScope.CardType)?.cardType?.uppercase()
        }.toSet()
        if (protectionColors.isNotEmpty() || protectionSubtypes.isNotEmpty() ||
            protectionSupertypes.isNotEmpty() || protectionCardTypes.isNotEmpty()
        ) {
            result = result.with(
                ProtectionComponent(
                    protectionColors,
                    protectionSubtypes,
                    protectionSupertypes,
                    protectionCardTypes
                )
            )
        }

        // Card-intrinsic "would be put into [zone] from anywhere → redirect instead" self-replacements
        // (Darksteel Colossus, Progenitus, Wilt-Leaf Liege). Carried on the card entity so they
        // function in every zone, not just on the battlefield — see [SelfZoneRedirectComponent].
        val selfRedirects = cardDef.script.replacementEffects
            .filterIsInstance<RedirectZoneChange>()
            .filter { it.selfOnly }
        if (selfRedirects.isNotEmpty()) {
            result = result.with(SelfZoneRedirectComponent(selfRedirects))
        }

        // "Hexproof from [quality]" (CR 702.11b). Only the scopes the rules engine enforces are
        // carried: colors (`HEXPROOF_FROM_<COLOR>`) and card types (`HEXPROOF_FROM_CARDTYPE_<TYPE>`).
        // Other [ProtectionScope]s format oracle text but have no targeting wiring yet, so they are
        // dropped rather than projected as a keyword nothing consults.
        val hexproofScopes = cardDef.keywordAbilities
            .filterIsInstance<KeywordAbility.Hexproof>()
            .map { it.scope }
        val hexproofColors = hexproofScopes.flatMap { s ->
            when (s) {
                is ProtectionScope.Color -> listOf(s.color)
                is ProtectionScope.Colors -> s.colors
                else -> emptyList()
            }
        }.toSet()
        val hexproofCardTypes = hexproofScopes.filterIsInstance<ProtectionScope.CardType>()
            .map { it.cardType.uppercase() }
            .toSet()
        if (hexproofColors.isNotEmpty() || hexproofCardTypes.isNotEmpty()) {
            result = result.with(HexproofFromComponent(hexproofColors, hexproofCardTypes))
        }

        // Toxic N (702.164). Multiple instances stack per Rule 702.164b — sum across
        // any printed Toxic abilities so the projector can emit a single TOXIC_<n>.
        val toxicAmount = cardDef.keywordAbilities
            .filterIsInstance<KeywordAbility.Numeric>()
            .filter { it.keyword == Keyword.TOXIC }
            .sumOf { it.n }
        if (toxicAmount > 0) {
            result = result.with(ToxicComponent(toxicAmount))
        }

        return result
    }
}
