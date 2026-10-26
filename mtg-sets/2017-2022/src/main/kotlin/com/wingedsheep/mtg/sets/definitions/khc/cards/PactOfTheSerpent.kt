package com.wingedsheep.mtg.sets.definitions.khc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.withSubtypeFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.OptionType

/**
 * Pact of the Serpent — Kaldheim Commander #9 (canonical printing)
 * {1}{B}{B} · Sorcery
 *
 * Choose a creature type. Target player draws X cards and loses X life, where X is the number of
 * creatures they control of the chosen type.
 *
 * The player is targeted on cast; the type is chosen on resolution (ruling). X counts the
 * *target player's* creatures of that type (projected subtypes, so changelings count) as the
 * spell resolves, and is captured once so the draw and the life loss use the same number.
 */
val PactOfTheSerpent = card("Pact of the Serpent") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose a creature type. Target player draws X cards and loses X life, where X is the " +
        "number of creatures they control of the chosen type."

    spell {
        val targetPlayer = target(Targets.Player)
        effect = Effects.Pipeline {
            val chosenType = chooseOption(OptionType.CREATURE_TYPE)
            val x = storeNumber(
                DynamicAmounts.battlefield(
                    targetPlayer.asPlayer,
                    GameObjectFilter.Creature.withSubtypeFromVariable(chosenType),
                ).count()
            )
            run(Effects.DrawCards(x.amount, targetPlayer))
            run(Effects.LoseLife(x.amount, targetPlayer))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Donato Giancola"
        flavorText = "\"Our quarrels behind us. Our foes before us. Death or triumph for us all!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5b3d118-ba4b-4a93-92a2-cd763de49d29.jpg?1783928337"
        ruling("2021-02-05", "You choose the target player as you cast Pact of the Serpent, but you don't choose the creature type until the spell resolves.")
        ruling("2021-02-05", "The value of X is based on the number of creatures the player controls of the chosen type as Pact of the Serpent resolves.")
    }
}
