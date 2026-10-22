package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Moonveil Regent — Innistrad: Midnight Hunt #149
 * {3}{R} · Creature — Dragon · 4/4
 *
 * Flying
 * Whenever you cast a spell, you may discard your hand. If you do, draw a card for each of that
 * spell's colors.
 * When this creature dies, it deals X damage to any target, where X is the number of colors among
 * permanents you control.
 *
 * Discarding a hand always happens once chosen — even an empty one (ruling: you still draw) — so the
 * "if you do" is the choice itself: `May(discard hand, then draw)`. The spell's colors are read as
 * the trigger resolves (live off the stack, or from the card if it has already left). The death
 * trigger counts colors among the permanents you control as it resolves; the Regent itself is gone
 * by then and no longer contributes red.
 */
val MoonveilRegent = card("Moonveil Regent") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever you cast a spell, you may discard your hand. If you do, draw a card for each of that spell's colors.\n" +
        "When this creature dies, it deals X damage to any target, where X is the number of colors among permanents you control."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.May(
            Patterns.Hand.discardHand() then
                Effects.DrawCards(DynamicAmounts.colorCountOf(EffectTarget.TriggeringEntity))
        )
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val anyTarget = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.colorsAmongPermanents(), anyTarget)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "149"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9fc55fd-161a-4d77-82f0-27075210e7e7.jpg?1783925597"
        ruling("2021-09-24", "If you have no cards in hand as Moonveil Regent's first ability resolves, you may still choose to discard your hand. You'll just draw a card for each of that spell's colors.")
        ruling("2021-09-24", "If you cast a colorless spell, you may choose to discard your hand and draw no cards.")
        ruling("2021-09-24", "Moonveil Regent's last ability triggers even if you control no permanents that are one or more colors when it dies. The amount of damage is calculated as the ability resolves. Consider only the permanents you control at that time to determine how much damage Moonveil Regent deals.")
    }
}
