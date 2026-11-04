package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Jace's Sentinel
 * {1}{U}
 * Creature — Merfolk Warrior
 * 1/3
 * As long as you control a Jace planeswalker, this creature gets +1/+0 and can't be blocked.
 *
 * Two [ConditionalStaticAbility]s sharing one condition — "a Jace planeswalker" is a planeswalker
 * with the Jace subtype, re-evaluated at projection so both bonuses track the planeswalker.
 */
val JacesSentinel = card("Jace's Sentinel") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Warrior"
    power = 1
    toughness = 3
    oracleText = "As long as you control a Jace planeswalker, this creature gets +1/+0 and can't be blocked."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 0, GroupFilter.source()),
            condition = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Jace")),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CantBeBlocked(),
            condition = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Jace")),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "283"
        artist = "Alex Konstad"
        flavorText = "\"We drove the Sun Empire out of the forest long ago, and we will not let them return.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/1/9131361b-ec1e-4390-82c9-b9b9f77792a8.jpg?1783935684"
    }
}
