package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sunhome Guildmage
 * {R}{W}
 * Creature — Human Wizard
 * 2/2
 * {1}{R}{W}: Creatures you control get +1/+0 until end of turn.
 * {2}{R}{W}: Create a 1/1 red and white Soldier creature token with haste.
 */
val SunhomeGuildmage = card("Sunhome Guildmage") {
    manaCost = "{R}{W}"
    colorIdentity = "WR"
    typeLine = "Creature — Human Wizard"
    oracleText = "{1}{R}{W}: Creatures you control get +1/+0 until end of turn.\n" +
        "{2}{R}{W}: Create a 1/1 red and white Soldier creature token with haste."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{1}{R}{W}")
        effect = Patterns.Group.modifyStatsForAll(
            1, 0,
            GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    activatedAbility {
        cost = Costs.Mana("{2}{R}{W}")
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED, Color.WHITE),
            creatureTypes = setOf("Soldier"),
            keywords = setOf(Keyword.HASTE),
            imageUri = "https://cards.scryfall.io/normal/front/a/8/a8a5a5a5-a8ae-41ed-9af5-b99d80ce0b35.jpg?1783940147",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "200"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42d1122a-099b-49bf-9b53-52429658816a.jpg"
    }
}
