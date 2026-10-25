package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Triumphant Adventurer
 * {W}{B}
 * Creature — Human Knight
 * 1/1
 * Deathtouch
 * During your turn, this creature has first strike.
 * Whenever this creature attacks, venture into the dungeon.
 * (Enter the first room or advance to the next room.)
 */
val TriumphantAdventurer = card("Triumphant Adventurer") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Knight"
    oracleText = "Deathtouch\n" +
        "During your turn, this creature has first strike.\n" +
        "Whenever this creature attacks, venture into the dungeon. (Enter the first room or advance to the next room.)"
    power = 1
    toughness = 1

    keywords(Keyword.DEATHTOUCH)

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE, GroupFilter.source())
        condition = Conditions.IsYourTurn
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.VentureIntoTheDungeon()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "237"
        artist = "Alexander Mokhov"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/214d3167-4eea-42a1-a464-ddff59843142.jpg?1783926443"
    }
}
