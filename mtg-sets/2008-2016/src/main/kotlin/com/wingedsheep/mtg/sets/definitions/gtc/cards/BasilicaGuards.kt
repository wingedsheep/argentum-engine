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
 * Basilica Guards
 * {2}{W}
 * Creature — Human Soldier
 * 1/4
 * Defender
 * Extort (Whenever you cast a spell, you may pay {W/B}. If you do, each opponent loses 1 life and
 * you gain that much life.)
 *
 * Extort is composed as on Crypt Ghast: a cast trigger with a resolution-time [Effects.MayPay] of
 * `{W/B}` over [Effects.DrainLife].
 */
val BasilicaGuards = card("Basilica Guards") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 4
    oracleText = "Defender\n$EXTORT_TEXT"

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.MayPay(cost = ManaCost.parse("{W/B}"), then = Effects.DrainLife(1))
        description = EXTORT_TEXT
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Dan Murayama Scott"
        flavorText = "\"None may pass . . . for free.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2be39fed-4b39-4027-9c80-f2186f7dd941.jpg"
        ruling("2024-01-12", "You may pay {W/B} a maximum of one time for each extort triggered ability. You decide whether to pay when the ability resolves.")
        ruling("2024-01-12", "The amount of life you gain from extort is based on the total amount of life lost, not necessarily the number of opponents you have.")
        ruling("2024-01-12", "The extort ability doesn't target any player.")
        ruling("2024-01-12", "The extort ability resolves before the spell that caused it to trigger. The ability resolves even if that spell is countered.")
    }
}
