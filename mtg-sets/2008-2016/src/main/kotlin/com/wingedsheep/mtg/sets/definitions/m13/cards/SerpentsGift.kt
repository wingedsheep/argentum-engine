package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Serpent's Gift
 * {2}{G}
 * Instant
 * Target creature gains deathtouch until end of turn.
 */
val SerpentsGift = card("Serpent's Gift") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gains deathtouch until end of turn. (Any amount of damage it deals to a creature is enough to destroy it.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "190"
        artist = "Steve Argyle"
        flavorText = "Venom is nature's way of redressing the imbalance between weak and mighty."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e27503e-059e-4c44-a817-678e67254111.jpg"
    }
}
