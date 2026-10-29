package com.wingedsheep.engine.view.projection

import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.view.ClientCardFace
import com.wingedsheep.engine.view.ClientPlaneswalkerAbility
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost

/**
 * The faces a card can show beyond its current one, and its planeswalker loyalty menu: the other
 * face of a transforming or modal DFC (for the flip preview), the halves of a split / Room card,
 * and the loyalty abilities a planeswalker on the battlefield can activate.
 */
internal data class CardFacesView(
    val isDoubleFaced: Boolean,
    val currentFace: String?,
    val backFaceName: String?,
    val backFaceTypeLine: String?,
    val backFaceOracleText: String?,
    val backFaceImageUri: String?,
    val backFacePower: Int?,
    val backFaceToughness: Int?,
    val backFaceKeywords: Set<Keyword>,
    val backFaceIsLandscape: Boolean,
    val isRoom: Boolean,
    val isLandscapeFace: Boolean,
    val cardFaces: List<ClientCardFace>,
    val planeswalkerAbilities: List<ClientPlaneswalkerAbility>?,
)

internal class CardFacesProjector(
    private val cardRegistry: CardRegistry,
    private val grantedAbilityUtils: CastPermissionUtils,
    private val stackText: StackTextRenderer,
) {

    fun project(
        state: GameState,
        entityId: EntityId,
        zoneKey: ZoneKey,
        container: ComponentContainer,
        cardComponent: CardComponent,
        cardDef: CardDefinition?
    ): CardFacesView {
        // Modal DFC (CR 712) back face for display/flip preview. A *spell* back lives in
        // `cardFaces` (Flamescroll Celebrant // Revel in Silence) and is picked up here; a
        // *permanent* back lives in `backFace` (the MSH hero cycle) and is picked up by
        // `dfcBackFace` below, which is also what the transform machinery reads.
        val modalBackFace = if (cardDef?.layout == CardLayout.MODAL_DFC) {
            cardDef.cardFaces.firstOrNull()
        } else null
        val dfcBackFace = dfcBackFace(container, cardDef)
        val doubleFaced = container.get<DoubleFacedComponent>()

        return CardFacesView(
            // A modal DFC with a *spell* back (CR 712) keeps it in `cardFaces`, so the SDK
            // `isDoubleFaced` (transform machinery) stays false — surface it to the client as
            // double-faced for display/flip-preview only. One with a *permanent* back already
            // reports `isDoubleFaced`, since that back is reachable by transform too (CR 712.3).
            isDoubleFaced = doubleFaced != null || cardDef?.isDoubleFaced == true || modalBackFace != null,
            currentFace = doubleFaced?.currentFace?.name
                ?: if (cardDef?.isDoubleFaced == true || modalBackFace != null) "FRONT" else null,
            backFaceName = dfcBackFace?.name ?: modalBackFace?.name,
            backFaceTypeLine = dfcBackFace?.typeLine?.toString() ?: modalBackFace?.typeLine?.toString(),
            backFaceOracleText = dfcBackFace?.oracleText ?: modalBackFace?.oracleText,
            backFaceImageUri = cardComponent.backFaceImageUri ?: dfcBackFace?.metadata?.imageUri ?: modalBackFace?.imageUri,
            // A modal DFC's spell back lives in `cardFaces` and carries no P/T of its own, so only
            // the transform back face (a permanent) contributes stats; keywords come from either.
            backFacePower = dfcBackFace?.creatureStats?.basePower,
            backFaceToughness = dfcBackFace?.creatureStats?.baseToughness,
            backFaceKeywords = dfcBackFace?.keywords ?: modalBackFace?.keywords ?: emptySet(),
            // Only transforming DFCs can pair a landscape face with a portrait one; a modal DFC's
            // other half lives in `cardFaces` under the MODAL_DFC layout, which is portrait on both
            // sides, so it never contributes here.
            backFaceIsLandscape = dfcBackFace?.isLandscapePrint == true,
            isRoom = cardDef?.isRoom == true,
            // `cardDef` already tracks the *displayed* face of a DFC (dfcBackFace resolves the
            // other one relative to DoubleFacedComponent.currentFace), so a defeated Siege recast
            // as its portrait back face correctly stops reporting landscape.
            isLandscapeFace = cardDef?.isLandscapePrint == true,
            cardFaces = splitFaces(container, cardDef),
            planeswalkerAbilities = planeswalkerAbilities(state, entityId, cardDef, zoneKey),
        )
    }

    /**
     * Build per-face DTOs for split-layout cards (currently Rooms). Returns an empty list for
     * normal single-face cards. The [ClientCardFace.isUnlocked] flag reflects the live
     * [RoomComponent] state for battlefield permanents and is `false` everywhere else (since
     * lock state only exists once the Room is on the battlefield, per CR 709.5d).
     */
    private fun splitFaces(
        container: ComponentContainer,
        cardDef: CardDefinition?
    ): List<ClientCardFace> {
        if (cardDef == null || cardDef.layout != CardLayout.SPLIT) return emptyList()
        if (cardDef.cardFaces.isEmpty()) return emptyList()
        val roomComp = container.get<RoomComponent>()
        return cardDef.cardFaces.map { face ->
            val faceIdValue = face.name
            val isUnlocked = roomComp?.unlocked?.any { it.value == faceIdValue } ?: false
            ClientCardFace(
                faceId = faceIdValue,
                name = face.name,
                manaCost = face.manaCost.toString(),
                typeLine = face.typeLine.toString(),
                oracleText = face.oracleText,
                isUnlocked = isUnlocked
            )
        }
    }

    private fun planeswalkerAbilities(
        state: GameState,
        entityId: EntityId,
        cardDef: CardDefinition?,
        zoneKey: ZoneKey
    ): List<ClientPlaneswalkerAbility>? {
        if (cardDef == null) return null
        if (zoneKey.zoneType != Zone.BATTLEFIELD) return null
        if (!cardDef.typeLine.cardTypes.contains(CardType.PLANESWALKER)) return null
        val printed = cardDef.script.activatedAbilities.filter { it.isPlaneswalkerAbility }
        // Loyalty abilities granted by another permanent ("Planeswalkers you control have
        // '[−4]: …'" — Way of the Wildspeaker) or by a resolved effect are activated through the
        // same legal actions as printed ones, so the menu lists them too. Their text isn't on
        // this card's oracle: one copied off another planeswalker (Nicol Bolas, Dragon-God) shows the
        // donor's oracle line, an authored grant the ability's own description.
        val granted = (
            state.grantedActivatedAbilities.filter { it.entityId == entityId }.map { it.ability } +
                grantedAbilityUtils.getStaticGrantedAbilitiesWithGranter(entityId, state).map { it.ability }
            ).filter { it.isPlaneswalkerAbility }
        val abilities = printed + granted
        if (abilities.isEmpty()) return null
        // Consumed in order, so two abilities sharing a loyalty cost (Garruk Relentless's two
        // 0-cost abilities) each take their own oracle line instead of both echoing the first.
        val oracleDescriptions = parseOracleLoyaltyLines(cardDef.oracleText)
            .mapValues { (_, lines) -> ArrayDeque(lines) }
        val grantedIds = granted.map { it.id }.toSet()
        // What the controller would actually pay — Carth the Lion's [+1] surcharge turns a [+2] into
        // [+3] — the same cost the legal actions and the payment use. A [−X] keeps its "−X" badge;
        // its X picker carries the raised bound.
        val surcharge = state.projectedState.getController(entityId)
            ?.let { grantedAbilityUtils.loyaltyCostSurcharge(state, it) } ?: 0
        return abilities.mapNotNull { ability ->
            val loyaltyX = ability.cost == AbilityCost.LoyaltyX
            val loyalty = (ability.cost as? AbilityCost.Loyalty)?.change
                ?: if (loyaltyX) 0 else return@mapNotNull null
            val description = (if (ability.id in grantedIds) donorOracleLine(state, ability, cardDef) else if (loyaltyX) cardDef.oracleText.lines()
                .firstOrNull { it.startsWith("−X:") || it.startsWith("-X:") }
                ?.substringAfter(":")?.trim() else oracleDescriptions[loyalty]?.removeFirstOrNull())
                ?: ability.descriptionOverride
                ?: StackTextRenderer.effectDisplayText(ability.effect, "this planeswalker")
            ClientPlaneswalkerAbility(
                abilityId = ability.id.value,
                loyaltyChange = if (loyaltyX) loyalty else loyalty + surcharge,
                loyaltyX = loyaltyX,
                description = description
            )
        }
    }

    /**
     * The printed text of a loyalty ability copied off another planeswalker ("has all loyalty
     * abilities of all other planeswalkers" — Nicol Bolas, Dragon-God): the donor's own oracle line,
     * found by the ability's id among the printed loyalty abilities of the planeswalkers on the
     * battlefield. Null for an authored grant (Way of the Wildspeaker), which no card prints.
     * The donor's name in that line reads as the gainer's — "if Ral Zarek is on the battlefield,
     * Nicol Bolas deals the damage" (Nicol Bolas, Dragon-God ruling, 2024-01-12).
     */
    private fun donorOracleLine(
        state: GameState,
        ability: com.wingedsheep.sdk.scripting.ActivatedAbility,
        gainer: CardDefinition,
    ): String? {
        val loyalty = (ability.cost as? AbilityCost.Loyalty)?.change ?: return null
        for (id in state.getBattlefield()) {
            val donor = state.getEntity(id)?.get<CardComponent>()?.cardDefinitionId?.let(cardRegistry::getCard) ?: continue
            val sameCost = donor.script.activatedAbilities.filter { it.cost == ability.cost }
            val index = sameCost.indexOfFirst { it.id == ability.id }
            if (index < 0) continue
            val line = parseOracleLoyaltyLines(donor.oracleText)[loyalty]?.getOrNull(index) ?: return null
            return line
                .replace(donor.name, gainer.name)
                .replace(donor.name.substringBefore(","), gainer.name.substringBefore(","))
        }
        return null
    }

    /**
     * Parse a planeswalker's oracle text into a map of loyalty change → ability texts, in the order
     * the lines appear. Example line: "−2: Ajani deals 4 damage to target tapped creature."
     * Handles the Unicode minus (U+2212), the ASCII hyphen, and a leading "+".
     *
     * A loyalty cost maps to a *list* because it isn't unique: Garruk Relentless has two 0-cost
     * abilities, Vivien, Monsters' Advocate two −2s. Keying one text per cost gave every ability
     * sharing that cost the first line's text, so the card showed the same ability twice.
     */
    private fun parseOracleLoyaltyLines(oracleText: String): Map<Int, List<String>> {
        if (oracleText.isBlank()) return emptyMap()
        val pattern = Regex("""^\s*([+−\-]?)(\d+):\s*(.+?)\s*$""")
        val result = mutableMapOf<Int, MutableList<String>>()
        for (raw in oracleText.lines()) {
            val match = pattern.matchEntire(raw) ?: continue
            val sign = match.groupValues[1]
            val magnitude = match.groupValues[2].toIntOrNull() ?: continue
            val loyalty = if (sign == "−" || sign == "-") -magnitude else magnitude
            val text = match.groupValues[3].trimEnd('.', ' ')
            result.getOrPut(loyalty) { mutableListOf() }.add(text)
        }
        return result
    }

    /**
     * Resolve the back face [CardDefinition] for a DFC client DTO. Looks at the permanent's
     * [DoubleFacedComponent] first (so a transformed permanent still exposes its other face), and
     * falls back to the card definition's `backFace` pointer for cards not currently on the
     * battlefield.
     */
    private fun dfcBackFace(
        container: ComponentContainer,
        cardDef: CardDefinition?
    ): CardDefinition? {
        val dfc = container.get<DoubleFacedComponent>()
        if (dfc != null) {
            val otherId = when (dfc.currentFace) {
                DoubleFacedComponent.Face.FRONT -> dfc.backCardDefinitionId
                DoubleFacedComponent.Face.BACK -> dfc.frontCardDefinitionId
            }
            cardRegistry.getCard(otherId)?.let { return it }
        }
        return cardDef?.backFace
    }
}
