package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayhem
import com.wingedsheep.sdk.model.Rarity

/**
 * Electro's Bolt — Marvel's Spider-Man #77
 * {2}{R} · Sorcery
 *
 * Electro's Bolt deals 4 damage to target creature.
 * Mayhem {1}{R}
 */
val ElectrosBolt = card("Electro's Bolt") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Electro's Bolt deals 4 damage to target creature.\n" +
        "Mayhem {1}{R} (You may cast this card from your graveyard for {1}{R} if you discarded it " +
        "this turn. Timing rules still apply.)"

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.DealDamage(4, creature)
    }

    mayhem("{1}{R}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "JB Casacop"
        flavorText = "\"Hey Max, while you're at it, do you mind charging my phone?\"\n—Spider-Man"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25fe063f-35e4-4fca-9889-06834a8ef9b9.jpg?1783905337"
    }
}
