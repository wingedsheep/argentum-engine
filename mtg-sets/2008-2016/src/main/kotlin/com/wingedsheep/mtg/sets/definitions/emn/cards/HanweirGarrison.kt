package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hanweir Garrison
 * {2}{R}
 * Creature — Human Soldier
 * 2/3
 * Whenever this creature attacks, create two 1/1 red Human creature tokens that are tapped and
 * attacking.
 *
 * Melds with Hanweir Battlements — the meld ability lives on the Battlements (CR 712.4a). The
 * tapped-and-attacking tokens use [CreateTokenEffect] with `tapped`/`attacking`
 * (Warren Warleader family).
 */
val HanweirGarrison = card("Hanweir Garrison") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "Whenever this creature attacks, create two 1/1 red Human creature tokens that " +
        "are tapped and attacking.\n(Melds with Hanweir Battlements.)"

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Human"),
            tapped = true,
            attacking = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "130"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/0/9/0900e494-962d-48c6-8e78-66a489be4bb2.jpg?1782711861"
    }
}
