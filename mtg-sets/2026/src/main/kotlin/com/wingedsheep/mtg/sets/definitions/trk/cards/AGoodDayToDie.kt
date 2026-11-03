package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * A Good Day to Die
 * {B}
 * Instant
 *
 * Target creature gets +2/+0 until end of turn. When that creature dies this turn, you draw a card.
 *
 * Make Your Mark's shape: the death clause is a delayed trigger watching the chosen creature,
 * `fireOnce` for the printed "When", expiring at end of turn. "You" is the spell's controller.
 */
val AGoodDayToDie = card("A Good Day to Die") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+0 until end of turn. When that creature dies this turn, you draw a card."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 0, t) then
            Effects.CreateDelayedTrigger(
                effect = Effects.DrawCards(1),
                trigger = Triggers.self.dies(),
                watchedTarget = t,
                fireOnce = true,
                expiry = DelayedTriggerExpiry.EndOfTurn,
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Monztre"
        flavorText = "There are no old warriors.\n—Klingon proverb"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2b350b2-0eda-4c2b-b9d6-b58ab4848545.jpg?1785981483"
    }
}
