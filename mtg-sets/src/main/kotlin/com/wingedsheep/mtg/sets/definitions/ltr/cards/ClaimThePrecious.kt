package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Claim the Precious
 * {1}{B}{B}
 * Sorcery
 *
 * Destroy target creature. The Ring tempts you.
 */
val ClaimThePrecious = card("Claim the Precious") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature. The Ring tempts you."

    spell {
        val t = target("target creature", Targets.Creature)
        effect = Effects.Destroy(t).then(Effects.TheRingTemptsYou())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "Artur Treffner"
        flavorText = "\"I think it is a sad story, and it might have happened to others, even to some Hobbits that I have known.\"\n—Gandalf"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/121c12c4-83ea-463e-8fdf-30718968a2bd.jpg?1686968419"
    }
}
