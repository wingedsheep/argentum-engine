package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Harried Spearguard
 * {R}
 * Creature — Human Soldier
 * 1/1
 *
 * Haste
 * When this creature dies, create a 1/1 black Rat creature token with "This token can't block."
 */
val HarriedSpearguard = card("Harried Spearguard") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier"
    oracleText = "Haste\n" +
        "When this creature dies, create a 1/1 black Rat creature token with \"This token can't block.\""
    power = 1
    toughness = 1

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.Dies
        effect = woeRatToken()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Borja Pindado"
        flavorText = "\"Why're you vermin so daft? This is a warehouse full of bricks. BRICKS! " +
            "You can't eat bricks.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1db79785-4f55-445f-93f2-14c6e4606fc5.jpg?1783915094"
    }
}
