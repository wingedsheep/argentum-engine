package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dragonlord Dromoka {4}{G}{W}
 * Legendary Creature — Elder Dragon
 * 5/7
 *
 * This spell can't be countered.
 * Flying, lifelink
 * Your opponents can't cast spells during your turn.
 *
 * The lock is Voice of Victory's [PlayersCantCastSpells] scoped to `EachOpponent` while
 * `IsYourTurn`.
 */
val DragonlordDromoka = card("Dragonlord Dromoka") {
    manaCost = "{4}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Elder Dragon"
    power = 5
    toughness = 7
    oracleText = "This spell can't be countered.\n" +
        "Flying, lifelink\n" +
        "Your opponents can't cast spells during your turn."

    cantBeCountered = true

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    staticAbility {
        ability = PlayersCantCastSpells(affected = Player.EachOpponent, condition = IsYourTurn)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "217"
        artist = "Eric Deschamps"
        flavorText = "Dromoka's followers forsake blood ties so that they may join a greater family."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/908042f3-0d03-4edf-816a-ec846a1e315f.jpg?1783938573"
    }
}
