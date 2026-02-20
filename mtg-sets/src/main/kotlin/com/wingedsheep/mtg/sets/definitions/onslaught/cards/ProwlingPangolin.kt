package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AnyPlayerMayPayEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect

/**
 * Prowling Pangolin
 * {3}{B}{B}
 * Creature — Pangolin Beast
 * 6/5
 * When Prowling Pangolin enters the battlefield, any player may sacrifice
 * two creatures. If a player does, sacrifice Prowling Pangolin.
 */
val ProwlingPangolin = card("Prowling Pangolin") {
    manaCost = "{3}{B}{B}"
    typeLine = "Creature — Pangolin Beast"
    power = 6
    toughness = 5
    oracleText = "When Prowling Pangolin enters the battlefield, any player may sacrifice two creatures. If a player does, sacrifice Prowling Pangolin."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = AnyPlayerMayPayEffect(
            cost = PayCost.Sacrifice(GameObjectFilter.Creature, count = 2),
            consequence = SacrificeSelfEffect
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Heather Hudson"
        flavorText = "\"It's always hungry, yet easily sated.\""
        imageUri = "https://cards.scryfall.io/large/front/0/f/0f037e99-75fb-4a2a-b4c6-448ef21b16a3.jpg?1562898495"
    }
}
