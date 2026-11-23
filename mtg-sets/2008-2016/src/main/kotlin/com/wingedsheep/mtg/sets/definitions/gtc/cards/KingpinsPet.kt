package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Kingpin's Pet
 * {1}{W}{B}
 * Creature — Thrull
 * 2/2
 * Flying
 * Extort (Whenever you cast a spell, you may pay {W/B}. If you do, each opponent loses 1 life and
 * you gain that much life.)
 *
 * Extort is composed as on Crypt Ghast: a cast trigger with a resolution-time [Effects.MayPay] of
 * `{W/B}` over [Effects.DrainLife].
 */
val KingpinsPet = card("Kingpin's Pet") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Thrull"
    power = 2
    toughness = 2
    oracleText = "Flying\nExtort (Whenever you cast a spell, you may pay {W/B}. If you do, " +
        "each opponent loses 1 life and you gain that much life.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.MayPay(cost = ManaCost.parse("{W/B}"), then = Effects.DrainLife(1))
        description = "Extort (Whenever you cast a spell, you may pay {W/B}. If you do, " +
            "each opponent loses 1 life and you gain that much life.)"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Mark Zug"
        flavorText = "\"Debt and death sound so similar, and thrulls are so hard of hearing. Mistakes are inevitable.\"\n—Milana, Orzhov prelate"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/3465cf63-4f10-4b53-9703-69746364dbc7.jpg"
        ruling("2024-01-12", "You may pay {W/B} a maximum of one time for each extort triggered ability. You decide whether to pay when the ability resolves.")
        ruling("2024-01-12", "The amount of life you gain from extort is based on the total amount of life lost, not necessarily the number of opponents you have. For example, if your opponent's life total can't change (perhaps because that player controls Platinum Emperion), you won't gain any life.")
        ruling("2024-01-12", "The extort ability doesn't target any player.")
        ruling("2024-01-12", "The extort ability resolves before the spell that caused it to trigger. The ability resolves even if that spell is countered.")
    }
}
