package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Rodeo Pyromancers
 * {3}{R}
 * Creature — Human Mercenary
 * 3/4
 * Whenever you cast your first spell each turn, add {R}{R}.
 */
val RodeoPyromancers = card("Rodeo Pyromancers") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Mercenary"
    oracleText = "Whenever you cast your first spell each turn, add {R}{R}."
    power = 3
    toughness = 4

    triggeredAbility {
        trigger = Triggers.NthSpellCast(1, Player.You)
        effect = Effects.AddMana(Color.RED, 2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "143"
        artist = "Kim Sokol"
        flavorText = "\"My citizens throw good money at them, and for what? Cheap tricks and " +
            "collateral damage. Damnable Slickshot charlatans.\"\n—Baron Bertram Graywater"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df877a29-06e1-474d-8600-410bbec674ae.jpg?1712355836"
    }
}
