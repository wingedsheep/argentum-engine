package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/** Creates three Soldiers and leads every other Soldier creature you control. */
val CaptainOfTheWatch = card("Captain of the Watch") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "Vigilance (Attacking doesn't cause this creature to tap.)\n" +
        "Other Soldier creatures you control get +1/+1 and have vigilance.\n" +
        "When this creature enters, create three 1/1 white Soldier creature tokens."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = 3,
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/6/d/6dccd622-264e-4867-b34b-324c94861241.jpg?1783942347"
        )
    }
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.SOLDIER).youControl(), excludeSelf = true)
        )
    }
    staticAbility {
        ability = GrantKeyword(
            Keyword.VIGILANCE,
            GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.SOLDIER).youControl(), excludeSelf = true)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "6"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6c26b98-790e-403b-b94b-261a4c92e721.jpg?1783942404"
        ruling("2009-10-01", "Captain of the Watch grants +1/+1 and vigilance to all other Soldier creatures you control, not just the ones its ability puts onto the battlefield.")
    }
}
