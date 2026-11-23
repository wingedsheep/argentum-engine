package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

private const val EXTORT_TEXT = "Extort (Whenever you cast a spell, you may pay {W/B}. If you do, " +
    "each opponent loses 1 life and you gain that much life.)"

/**
 * Syndic of Tithes
 * {2}{W}
 * Creature — Human Cleric
 * 2/2
 * Extort (Whenever you cast a spell, you may pay {W/B}. If you do, each opponent loses 1 life and
 * you gain that much life.)
 *
 * Extort is composed as on Basilica Screecher: a cast trigger with a resolution-time
 * [Effects.MayPay] of `{W/B}` over [Effects.DrainLife].
 */
val SyndicOfTithes = card("Syndic of Tithes") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = EXTORT_TEXT

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.MayPay(cost = ManaCost.parse("{W/B}"), then = Effects.DrainLife(1))
        description = EXTORT_TEXT
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "26"
        artist = "Steve Prescott"
        flavorText = "\"Entrance is free. Donations are required.\"\n—Notice outside Vizkopa Bank"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bafaa3b-eeaa-427f-9a73-6a1c98d257ca.jpg"
        ruling("2024-01-12", "You may pay {W/B} a maximum of one time for each extort triggered ability. You decide whether to pay when the ability resolves.")
        ruling("2024-01-12", "The amount of life you gain from extort is based on the total amount of life lost, not necessarily the number of opponents you have. For example, if your opponent's life total can't change (perhaps because that player controls Platinum Emperion), you won't gain any life.")
        ruling("2024-01-12", "The extort ability doesn't target any player.")
        ruling("2024-01-12", "The extort ability resolves before the spell that caused it to trigger. The ability resolves even if that spell is countered.")
    }
}
