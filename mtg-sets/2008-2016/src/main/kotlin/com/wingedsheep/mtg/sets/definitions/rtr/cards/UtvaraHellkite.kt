package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Utvara Hellkite
 * {6}{R}{R}
 * Creature — Dragon
 * 6/6
 * Flying
 * Whenever a Dragon you control attacks, create a 6/6 red Dragon creature token with flying.
 *
 * Per-attacker ANY-bound attack trigger: one token per attacking Dragon. The tokens aren't
 * attacking, so they never re-trigger it.
 */
val UtvaraHellkite = card("Utvara Hellkite") {
    manaCost = "{6}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 6
    toughness = 6
    oracleText = "Flying\n" +
        "Whenever a Dragon you control attacks, create a 6/6 red Dragon creature token with flying."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype("Dragon").youControl()).attacks()
        effect = Effects.CreateToken(
            power = 6,
            toughness = 6,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dragon"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/8/4/84310f84-3e5f-4db8-bff1-16bef64de1a0.jpg?1783940383",
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "110"
        artist = "Mark Zug"
        flavorText = "The fear of dragons is as old and as powerful as the fear of death itself."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f17c6478-dd80-4854-9560-bfc5ef597872.jpg?1783940353"
    }
}
