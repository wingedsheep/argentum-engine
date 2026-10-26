package com.wingedsheep.mtg.sets.definitions.mor.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.withSubtypeFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Distant Melody — Morningtide #32 (canonical / earliest real-expansion printing).
 * {3}{U} · Sorcery
 *
 * Choose a creature type. Draw a card for each permanent you control of that type.
 *
 * Harmonized Crescendo's spell without convoke: the type is chosen on resolution, then the draw
 * counts every *permanent* you control of that type (kindred permanents included, per the
 * 2008-04-01 ruling), not just creatures.
 */
val DistantMelody = card("Distant Melody") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Choose a creature type. Draw a card for each permanent you control of that type."

    spell {
        effect = Effects.Pipeline {
            val chosenType = chooseOption(OptionType.CREATURE_TYPE)
            run(
                Effects.DrawCards(
                    count = DynamicAmounts.battlefield(
                        Player.You,
                        GameObjectFilter.Permanent.withSubtypeFromVariable(chosenType)
                    ).count()
                )
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Omar Rayyan"
        flavorText = "Oona's song is like a twisted dinner chime. All the faeries return home, but it is " +
            "Oona who feasts—on the stolen dreams and rumors they serve her."
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a28e4f9-3b68-40bb-bf77-850b08c6b096.jpg?1783942800"
        ruling(
            "2008-04-01",
            "Note that this spell counts the number of permanents you control of the chosen type, not just " +
                "the number of creatures you control. It will also take your kindred permanents of the " +
                "chosen type into account."
        )
    }
}
