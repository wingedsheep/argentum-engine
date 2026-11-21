package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

private const val EXTORT_TEXT = "Extort (Whenever you cast a spell, you may pay {W/B}. If you do, " +
    "each opponent loses 1 life and you gain that much life.)"

/**
 * Tithe Drinker — Dragon's Maze #109 (canonical printing)
 * {W}{B} · Creature — Vampire · 2/1
 *
 * Lifelink
 * Extort
 *
 * Extort is composed as on Pontiff of Blight / Crypt Ghast: a cast trigger with a resolution-time
 * [Effects.MayPay] of `{W/B}` over [Effects.DrainLife].
 */
val TitheDrinker = card("Tithe Drinker") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Vampire"
    power = 2
    toughness = 1
    oracleText = "Lifelink (Damage dealt by this creature also causes you to gain that much life.)\n$EXTORT_TEXT"

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.MayPay(cost = ManaCost.parse("{W/B}"), then = Effects.DrainLife(1))
        description = EXTORT_TEXT
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e069aa06-35b0-4af8-89cb-af653708ed32.jpg?1783940020"
        ruling("2024-01-12", "You may pay {W/B} a maximum of one time for each extort triggered ability. You decide whether to pay when the ability resolves.")
        ruling("2024-01-12", "The amount of life you gain from extort is based on the total amount of life lost, not necessarily the number of opponents you have. For example, if your opponent's life total can't change (perhaps because that player controls Platinum Emperion), you won't gain any life.")
        ruling("2024-01-12", "The extort ability doesn't target any player.")
        ruling("2024-01-12", "The extort ability resolves before the spell that caused it to trigger. The ability resolves even if that spell is countered.")
    }
}
