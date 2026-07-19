package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Dark Deed
 * {1}{B}
 * Instant
 * Target creature gets -4/-4 until end of turn.
 */
val DarkDeed = card("Dark Deed") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -4/-4 until end of turn."

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.ModifyStats(-4, -4, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "93"
        artist = "Lixin Yin"
        flavorText = "\"You're pretty good, toots. But me? I'm magic.\"\n—Bullseye"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49e36cac-3999-40b3-91b3-85af4fded679.jpg?1783902948"
    }
}
