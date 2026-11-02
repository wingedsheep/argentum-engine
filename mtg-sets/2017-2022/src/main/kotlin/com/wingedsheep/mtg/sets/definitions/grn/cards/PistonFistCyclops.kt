package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Piston-Fist Cyclops — Guilds of Ravnica #217 (canonical printing)
 * {1}{U/R}{U/R} · Creature — Cyclops · 4/3
 *
 * Defender
 * As long as you've cast an instant or sorcery spell this turn, this creature can attack
 * as though it didn't have defender.
 */
val PistonFistCyclops = card("Piston-Fist Cyclops") {
    manaCost = "{1}{U/R}{U/R}"
    colorIdentity = "UR"
    typeLine = "Creature — Cyclops"
    power = 4
    toughness = 3
    oracleText = "Defender\n" +
        "As long as you've cast an instant or sorcery spell this turn, this creature can attack " +
        "as though it didn't have defender."

    keywords(Keyword.DEFENDER)

    staticAbility {
        ability = CanAttackDespiteDefender(
            condition = Conditions.YouCastSpellsThisTurn(1, GameObjectFilter.InstantOrSorcery)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "217"
        artist = "Wayne Reynolds"
        flavorText = "Its hyperpneumatics can punch through a wall and the spy on the other side."
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2ec7523a-a9c4-4b76-8f9e-b591a88e2e37.jpg?1783934116"
    }
}
