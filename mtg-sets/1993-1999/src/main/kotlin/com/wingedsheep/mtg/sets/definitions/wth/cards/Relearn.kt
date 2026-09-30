package com.wingedsheep.mtg.sets.definitions.wth.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val Relearn = card("Relearn") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return target instant or sorcery card from your graveyard to your hand."

    spell {
        val spellCard = target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.ReturnToHand(spellCard)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Zina Saunders"
        flavorText = "\"Barrin taught me that the hardest lessons to grasp are the ones you've already learned.\"\n—Ertai, wizard adept"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/902f8480-8ae7-4b5f-abdf-1bd46066049e.jpg?1783946738"
    }
}
