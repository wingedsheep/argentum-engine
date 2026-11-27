package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Fires of Undeath
 * {2}{R}
 * Instant
 * Fires of Undeath deals 2 damage to any target.
 * Flashback {5}{B}
 */
val FiresOfUndeath = card("Fires of Undeath") {
    manaCost = "{2}{R}"
    colorIdentity = "BR"
    typeLine = "Instant"
    oracleText = "Fires of Undeath deals 2 damage to any target.\n" +
        "Flashback {5}{B} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(2, t)
    }
    keywordAbility(KeywordAbility.flashback("{5}{B}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "88"
        artist = "Jason Chan"
        flavorText = "\"I drink of those who are worthy of my palate. The rest I burn.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d94aaa4-c2fd-4714-9198-8415158b9c4d.jpg"
    }
}
