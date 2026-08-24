package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect

/**
 * Molting Harpy
 * {B}
 * Creature — Harpy Mercenary
 * 2 / 1
 *
 * "Sacrifice this creature unless you pay {2}" is [PayOrSufferEffect] — the upkeep trigger offers
 * the mana payment and the sacrifice is the unpaid branch. Same shape as Prophecy's Pit Raptor.
 */
val MoltingHarpy = card("Molting Harpy") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Harpy Mercenary"
    oracleText = "Flying\n" +
        "At the beginning of your upkeep, sacrifice this creature unless you pay {2}."
    power = 2
    toughness = 1

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = PayOrSufferEffect(cost = Costs.pay.Mana("{2}"), suffer = SacrificeSelfEffect)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "148"
        artist = "Jeff Laubenstein"
        flavorText = "Shed harpy feathers leave painful wounds—which explains the harpies' mood."
        imageUri = "https://cards.scryfall.io/normal/front/d/d/ddfe33fb-71d5-4552-bcd3-f07e4e3847e1.jpg"
    }
}
