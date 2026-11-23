package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

private const val EXTORT_TEXT = "Extort (Whenever you cast a spell, you may pay {W/B}. If you do, " +
    "each opponent loses 1 life and you gain that much life.)"

/**
 * Syndicate Enforcer
 * {3}{B}
 * Creature — Human Rogue
 * 3/2
 * Extort (Whenever you cast a spell, you may pay {W/B}. If you do, each opponent loses 1 life and
 * you gain that much life.)
 */
val SyndicateEnforcer = card("Syndicate Enforcer") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 3
    toughness = 2
    oracleText = EXTORT_TEXT

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.MayPay(cost = ManaCost.parse("{W/B}"), then = Effects.DrainLife(1))
        description = EXTORT_TEXT
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "80"
        artist = "Steven Belledin"
        flavorText = "Even the darkest corners of Ravnica still lie in the shadow of the Church."
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cde6ee2e-a114-4935-8345-d3e264f9fc26.jpg"
        ruling("2024-01-12", "You may pay {W/B} a maximum of one time for each extort triggered ability. You decide whether to pay when the ability resolves.")
        ruling("2024-01-12", "The amount of life you gain from extort is based on the total amount of life lost, not necessarily the number of opponents you have. For example, if your opponent's life total can't change (perhaps because that player controls Platinum Emperion), you won't gain any life.")
        ruling("2024-01-12", "The extort ability doesn't target any player.")
        ruling("2024-01-12", "The extort ability resolves before the spell that caused it to trigger. The ability resolves even if that spell is countered.")
    }
}
