package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Knight of Obligation
 * {3}{W}
 * Creature — Human Knight
 * 2/4
 * Vigilance
 * Extort (Whenever you cast a spell, you may pay {W/B}. If you do, each opponent loses 1 life and
 * you gain that much life.)
 *
 * Extort is composed as on Crypt Ghast: a cast trigger with a resolution-time [Effects.MayPay] of
 * `{W/B}` over [Effects.DrainLife].
 */
val KnightOfObligation = card("Knight of Obligation") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 4
    oracleText = "Vigilance\nExtort (Whenever you cast a spell, you may pay {W/B}. If you do, " +
        "each opponent loses 1 life and you gain that much life.)"

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.MayPay(cost = ManaCost.parse("{W/B}"), then = Effects.DrainLife(1))
        description = "Extort (Whenever you cast a spell, you may pay {W/B}. If you do, " +
            "each opponent loses 1 life and you gain that much life.)"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "18"
        artist = "Ryan Barger"
        flavorText = "He can calculate interest to six decimal places even in the midst of a deadly duel."
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c2a1100-a2e6-4ef5-a8e3-2aca552d6b66.jpg"
        ruling("2024-01-12", "You may pay {W/B} a maximum of one time for each extort triggered ability. You decide whether to pay when the ability resolves.")
        ruling("2024-01-12", "The amount of life you gain from extort is based on the total amount of life lost, not necessarily the number of opponents you have. For example, if your opponent's life total can't change (perhaps because that player controls Platinum Emperion), you won't gain any life.")
        ruling("2024-01-12", "The extort ability doesn't target any player.")
        ruling("2024-01-12", "The extort ability resolves before the spell that caused it to trigger. The ability resolves even if that spell is countered.")
    }
}
