package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Pride Sovereign — Hour of Devastation #126
 * {2}{G} · Creature — Cat · 2/2
 *
 * This creature gets +1/+1 for each other Cat you control.
 * {W}, {T}, Exert this creature: Create two 1/1 white Cat creature tokens with lifelink.
 *
 * The bonus counts Cat permanents you control other than itself (the Squirrel Mob shape), read
 * continuously, so it grows as the tokens it makes enter.
 */
val PrideSovereign = card("Pride Sovereign") {
    manaCost = "{2}{G}"
    colorIdentity = "GW"
    typeLine = "Creature — Cat"
    power = 2
    toughness = 2
    oracleText = "This creature gets +1/+1 for each other Cat you control.\n" +
        "{W}, {T}, Exert this creature: Create two 1/1 white Cat creature tokens with lifelink. " +
        "(An exerted creature won't untap during your next untap step.)"

    staticAbility {
        val otherCats = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Permanent.withSubtype(Subtype.CAT),
            excludeSelf = true,
        ).count()
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = otherCats,
            toughnessBonus = otherCats,
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap, Costs.Exert)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Cat"),
            keywords = setOf(Keyword.LIFELINK),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/a/f/af0086cd-56b5-450e-818c-e54f45d1a104.jpg?1783936412",
        )
        description = "{W}, {T}, Exert this creature: Create two 1/1 white Cat creature tokens with lifelink."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "126"
        artist = "Ryan Yee"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd3d32b7-672f-4ceb-a1c9-17daefd2cb0c.jpg?1783936016"
        ruling(
            "2017-07-14",
            "Because damage remains marked on a creature until it's removed as the turn ends, the damage " +
                "Pride Sovereign takes during combat may become lethal if other Cats you control leave the " +
                "battlefield later in the turn."
        )
        ruling(
            "2017-07-14",
            "You can exert a creature to pay a cost even if you've already exerted it earlier in the turn. " +
                "Exerting it multiple times will keep it tapped only during your next untap step."
        )
    }
}
