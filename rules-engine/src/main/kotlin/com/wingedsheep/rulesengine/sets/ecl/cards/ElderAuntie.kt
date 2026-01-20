package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.CreateTokenEffect
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Elder Auntie
 *
 * {2}{R} Creature — Goblin Warlock 2/2
 * When this creature enters, create a 1/1 black and red Goblin creature token.
 */
object ElderAuntie {
    val definition = CardDefinition.creature(
        name = "Elder Auntie",
        manaCost = ManaCost.parse("{2}{R}"),
        subtypes = setOf(Subtype.GOBLIN, Subtype.WARLOCK),
        power = 2,
        toughness = 2,
        oracleText = "When this creature enters, create a 1/1 black and red Goblin creature token.",
        metadata = ScryfallMetadata(
            collectorNumber = "133",
            rarity = Rarity.COMMON,
            artist = "Caio Monteiro",
            imageUri = "https://cards.scryfall.io/normal/front/f/f/ff3f3f3f-3f3f-3f3f-3f3f-3f3f3f3f3f3f.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Elder Auntie") {
        // ETB: Create a 1/1 black and red Goblin token
        triggered(
            trigger = OnEnterBattlefield(),
            effect = CreateTokenEffect(
                count = 1,
                power = 1,
                toughness = 1,
                colors = setOf(Color.BLACK, Color.RED),
                creatureTypes = setOf("Goblin")
            )
        )
    }
}
