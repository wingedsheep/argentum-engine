package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Rictus Robber
 * {3}{B}
 * Creature — Zombie Rogue
 * 4/3
 *
 * When this creature enters, if a creature died this turn, create a 2/2 blue and black
 * Zombie Rogue creature token.
 * Plot {2}{B}
 */
val RictusRobber = card("Rictus Robber") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Rogue"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, if a creature died this turn, create a 2/2 blue and " +
        "black Zombie Rogue creature token.\n" +
        "Plot {2}{B} (You may pay {2}{B} and exile this card from your hand. Cast it as a sorcery " +
        "on a later turn without paying its mana cost. Plot only as a sorcery.)"

    keywordAbility(KeywordAbility.plot("{2}{B}"))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        // Intervening "if": checked both on trigger and on resolution.
        triggerCondition = Conditions.CreatureDiedThisTurn
        effect = Effects.CreateToken(
            count = 1,
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLUE, Color.BLACK),
            creatureTypes = setOf("Zombie", "Rogue"),
            imageUri = "https://cards.scryfall.io/normal/front/7/4/74c7a0bd-6011-495a-b56c-8fa707dd7f12.jpg?1712316777"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "102"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/252def94-2d89-48f7-8ff7-9c8682ca3ec6.jpg?1712355654"
    }
}
