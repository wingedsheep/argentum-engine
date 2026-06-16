package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Su-Chi
 * {4}
 * Artifact Creature — Construct
 * 4/4
 * When this creature dies, add {C}{C}{C}{C}.
 */
val SuChi = card("Su-Chi") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 4
    toughness = 4
    oracleText = "When this creature dies, add {C}{C}{C}{C}."

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.AddColorlessMana(4)
        description = "When this creature dies, add {C}{C}{C}{C}."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "66"
        artist = "Christopher Rush"
        flavorText = "Flawed copies of relics from the Thran Empire, the Su-Chi were inherently unstable but provided useful knowledge for Tocasia's students."
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a64d4f93-0c04-4078-aec0-7e9de92f260f.jpg?1562930042"
    }
}
