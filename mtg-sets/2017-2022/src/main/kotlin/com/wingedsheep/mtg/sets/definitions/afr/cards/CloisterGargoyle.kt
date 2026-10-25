package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Cloister Gargoyle
 * {2}{W}
 * Artifact Creature — Gargoyle
 * 0/4
 * When this creature enters, venture into the dungeon. (Enter the first room or advance to the next room.)
 * As long as you've completed a dungeon, this creature gets +3/+0 and has flying.
 */
val CloisterGargoyle = card("Cloister Gargoyle") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Gargoyle"
    power = 0
    toughness = 4
    oracleText = "When this creature enters, venture into the dungeon. (Enter the first room or advance to the next room.)\n" +
        "As long as you've completed a dungeon, this creature gets +3/+0 and has flying."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.VentureIntoTheDungeon()
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(3, 0, Filters.Self),
            condition = Conditions.YouCompletedADungeon
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FLYING, Filters.Self),
            condition = Conditions.YouCompletedADungeon
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/297bf274-d13d-4b75-b251-2d004b16b431.jpg?1783926537"
    }
}
