package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

private const val EXTORT_TEXT = "Extort (Whenever you cast a spell, you may pay {W/B}. If you do, " +
    "each opponent loses 1 life and you gain that much life.)"

/**
 * Basilica Screecher
 * {1}{B}
 * Creature — Bat
 * 1/2
 * Flying
 * Extort (Whenever you cast a spell, you may pay {W/B}. If you do, each opponent loses 1 life and
 * you gain that much life.)
 *
 * Extort is composed as on Crypt Ghast: a cast trigger with a resolution-time [Effects.MayPay] of
 * `{W/B}` over [Effects.DrainLife].
 */
val BasilicaScreecher = card("Basilica Screecher") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Bat"
    power = 1
    toughness = 2
    oracleText = "Flying\n$EXTORT_TEXT"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.MayPay(cost = ManaCost.parse("{W/B}"), then = Effects.DrainLife(1))
        description = EXTORT_TEXT
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "58"
        artist = "Christine Choi"
        flavorText = "A screech in the night is all it takes for Orzhov debtors to clutch at their coin purses."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d233c6bc-c4dd-482d-b0f4-87359acab7cb.jpg"
        ruling("2024-01-12", "You may pay {W/B} a maximum of one time for each extort triggered ability. You decide whether to pay when the ability resolves.")
        ruling("2024-01-12", "The amount of life you gain from extort is based on the total amount of life lost, not necessarily the number of opponents you have.")
        ruling("2024-01-12", "The extort ability doesn't target any player.")
        ruling("2024-01-12", "The extort ability resolves before the spell that caused it to trigger. The ability resolves even if that spell is countered.")
    }
}
