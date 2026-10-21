package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rite of Oblivion
 * {W}{B}
 * Sorcery
 * As an additional cost to cast this spell, sacrifice a nonland permanent.
 * Exile target nonland permanent.
 * Flashback {2}{W}{B}
 *
 * The sacrifice is an additional cost, so it is paid again when cast with flashback.
 */
val RiteOfOblivion = card("Rite of Oblivion") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice a nonland permanent.\n" +
        "Exile target nonland permanent.\n" +
        "Flashback {2}{W}{B} (You may cast this card from your graveyard for its flashback cost and any additional costs. Then exile it.)"

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.NonlandPermanent))

    spell {
        val t = target(TargetFilter.NonlandPermanent)
        effect = Effects.Move(t, Zone.EXILE)
    }

    keywordAbility(KeywordAbility.flashback("{2}{W}{B}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "237"
        artist = "Martina Pilcerova"
        flavorText = "Revenge is a circle without end."
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe1f8d52-565e-4900-8357-fd29f2022dc1.jpg?1783925555"
    }
}
