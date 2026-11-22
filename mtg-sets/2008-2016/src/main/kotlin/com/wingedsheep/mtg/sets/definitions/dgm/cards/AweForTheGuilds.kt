package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Awe for the Guilds
 * {2}{R}
 * Sorcery
 * Monocolored creatures can't block this turn.
 *
 * A dynamic group restriction (it doesn't change characteristics), so monocolored creatures that
 * enter later this turn can't block either, and one that becomes multicolored before blocks can.
 */
val AweForTheGuilds = card("Awe for the Guilds") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Monocolored creatures can't block this turn."

    spell {
        effect = Effects.CantBlockGroup(GroupFilter(GameObjectFilter.Creature.monocolored()))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Mathias Kollros"
        flavorText = "When the guilds cooperate, the guildless celebrate their peaceful society. When the guilds clash, the guildless just try to keep out of the way."
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec644ac3-07a2-43de-8173-9cc18e2ea2d9.jpg?1783940038"
        ruling("2013-04-15", "A monocolored creature is exactly one color. (Colorless creatures aren't monocolored.)")
        ruling(
            "2013-04-15",
            "No monocolored creature will be able to block that turn, including monocolored creatures that enter the battlefield after Awe for the Guilds resolves. A creature that's monocolored when Awe for the Guilds resolves but somehow becomes multicolored before blockers are declared will be able to block."
        )
    }
}
