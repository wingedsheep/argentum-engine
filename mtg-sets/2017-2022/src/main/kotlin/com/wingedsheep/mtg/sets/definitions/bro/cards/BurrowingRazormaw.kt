package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Burrowing Razormaw
 * {2}{G}
 * Creature — Beast
 * 4/2
 * When this creature dies, mill four cards. (Put the top four cards of your library into your graveyard.)
 */
val BurrowingRazormaw = card("Burrowing Razormaw") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 2
    oracleText = "When this creature dies, mill four cards. (Put the top four cards of your library into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Patterns.Library.mill(4)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Uriah Voth"
        flavorText = "\"That's odd. I don't remember seeing plans to dig a trench near here.\"\n—Lognell, Argivian scout"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7c8c73c-ee2e-4aa9-aa6c-42cf9d58d9cc.jpg"
    }
}
