package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Scorching Shot
 * {R}{R}
 * Sorcery
 * Scorching Shot deals 5 damage to target creature.
 */
val ScorchingShot = card("Scorching Shot") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Scorching Shot deals 5 damage to target creature."

    spell {
        val creature = target("creature", Targets.Creature)
        effect = Effects.DealDamage(5, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "145"
        artist = "Caio Monteiro"
        flavorText = "Most outlaws prefer to strike when there are no witnesses. Slickshots prefer as many as possible."
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f93d8357-83bd-4157-a389-68e4aa4985c8.jpg?1712355845"
    }
}
