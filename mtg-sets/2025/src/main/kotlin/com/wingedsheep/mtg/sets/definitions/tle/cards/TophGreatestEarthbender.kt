package com.wingedsheep.mtg.sets.definitions.tle.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Toph, Greatest Earthbender
 * {2}{R}{G}
 * Legendary Creature — Human Warrior Ally
 * 3/3
 * When Toph enters, earthbend X, where X is the amount of mana spent to cast her.
 * Land creatures you control have double strike.
 *
 * X is [DynamicAmounts.totalManaSpent], read off the payment recorded on Toph when she resolved
 * (generic and colorless included) — 0 when she entered without being cast. The static is
 * Earthbending Student's [GrantKeyword] over land creatures you control, with double strike.
 */
val TophGreatestEarthbender = card("Toph, Greatest Earthbender") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Human Warrior Ally"
    power = 3
    toughness = 3
    oracleText = "When Toph enters, earthbend X, where X is the amount of mana spent to cast her.\n" +
        "Land creatures you control have double strike."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val land = target(TargetFilter.Land.youControl())
        effect = Effects.Earthbend(DynamicAmounts.totalManaSpent(), land)
        description = "When Toph enters, earthbend X, where X is the amount of mana spent to cast her."
    }

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.DOUBLE_STRIKE,
            filter = GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Land).youControl())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "70"
        artist = "Brian Yuen"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b15acc09-dd32-4e93-a788-7c0ae965a0c8.jpg?1783904838"
        ruling("2025-10-02", "You may target a land that is already a creature, perhaps because of a previous earthbend ability. The land will get the +1/+1 counters, gain haste, and have its base power and toughness set to 0/0.")
        ruling("2025-10-02", "If a land was animated by earthbend and would go to any zone other than the graveyard or exile, it will not be returned to the battlefield by the delayed triggered ability created by earthbend.")
    }
}
