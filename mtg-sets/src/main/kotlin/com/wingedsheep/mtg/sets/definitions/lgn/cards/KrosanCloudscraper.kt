package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.dsl.Costs

/**
 * Krosan Cloudscraper
 * {7}{G}{G}{G}
 * Creature — Beast Mutant
 * 13/13
 * At the beginning of your upkeep, sacrifice Krosan Cloudscraper unless you pay {G}{G}.
 * Morph {7}{G}{G}
 */
val KrosanCloudscraper = card("Krosan Cloudscraper") {
    manaCost = "{7}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast Mutant"
    power = 13
    toughness = 13
    oracleText = "At the beginning of your upkeep, sacrifice Krosan Cloudscraper unless you pay {G}{G}.\nMorph {7}{G}{G} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)"

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = PayOrSufferEffect(
            cost = Costs.pay.Mana(ManaCost.parse("{G}{G}")),
            suffer = SacrificeSelfEffect
        )
    }

    morph = "{7}{G}{G}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "130"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51ef4cda-e55b-45a8-9c02-4e77e5b15a9e.jpg?1562911611"
    }
}
