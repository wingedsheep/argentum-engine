package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantOffspringToOwnSpells
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Zinnia, Valley's Voice
 * {U}{R}{W}
 * Legendary Creature — Bird Bard
 * 1/3
 *
 * Flying
 * Zinnia gets +X/+0, where X is the number of other creatures you control with base power 1.
 * Creature spells you cast gain offspring {2} as you cast them. (You may pay an additional {2} as
 * you cast a creature spell. If you do, when that creature enters, create a 1/1 token copy of it.)
 */
val ZinniaValleysVoice = card("Zinnia, Valley's Voice") {
    manaCost = "{U}{R}{W}"
    colorIdentity = "URW"
    typeLine = "Legendary Creature — Bird Bard"
    power = 1
    toughness = 3
    oracleText = "Flying\n" +
        "Zinnia gets +X/+0, where X is the number of other creatures you control with base power 1.\n" +
        "Creature spells you cast gain offspring {2} as you cast them. (You may pay an additional {2} as you " +
        "cast a creature spell. If you do, when that creature enters, create a 1/1 token copy of it.)"

    keywords(Keyword.FLYING)

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature.basePower(1),
                excludeSelf = true
            ).count(),
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    staticAbility {
        ability = GrantOffspringToOwnSpells(cost = ManaCost.parse("{2}"))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "4"
        artist = "Aldo Domínguez"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfe36901-5841-4942-8b61-c273111f715e.jpg?1783910737"
    }
}
