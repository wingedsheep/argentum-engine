package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Flood of Recollection
 * {U}{U}
 * Sorcery
 *
 * Return target instant or sorcery card from your graveyard to your hand. Exile Flood of Recollection.
 */
val FloodOfRecollection = card("Flood of Recollection") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return target instant or sorcery card from your graveyard to your hand. Exile Flood of Recollection."

    spell {
        val t = target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.Move(t, Zone.HAND)
        selfExile()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Magali Villeneuve"
        flavorText = "Jace tumbled down the waterfall and plunged deep into his past."
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03a353c1-a640-4b01-9754-3fb6011c8211.jpg?1783935325"
    }
}
