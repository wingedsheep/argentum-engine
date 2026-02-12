package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Akroma's Blessing
 * {2}{W}
 * Instant
 * Choose a color. Creatures you control gain protection from the chosen color until end of turn.
 * Cycling {W}
 */
val AkromasBlessing = card("Akroma's Blessing") {
    manaCost = "{2}{W}"
    typeLine = "Instant"
    oracleText = "Choose a color. Creatures you control gain protection from the chosen color until end of turn.\nCycling {W}"

    spell {
        effect = Effects.ChooseColorAndGrantProtection()
    }

    keywordAbility(KeywordAbility.cycling("{W}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Adam Rex"
        flavorText = "The clerics saw her as a divine gift. She saw them only as allies in her war against Phage."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3710c68-3f71-4d76-8bd2-001f0e8036f5.jpg?1562941167"
    }
}
