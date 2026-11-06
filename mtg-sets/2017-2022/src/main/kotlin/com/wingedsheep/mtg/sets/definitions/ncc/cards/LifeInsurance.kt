package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private const val EXTORT_TEXT = "Extort (Whenever you cast a spell, you may pay {W/B}. If you do, " +
    "each opponent loses 1 life and you gain that much life.)"

/**
 * Life Insurance
 * {3}{W}{B}
 * Enchantment
 *
 * Extort
 * Whenever a nontoken creature dies, you lose 1 life and create a Treasure token.
 *
 * Extort is composed as on Crypt Ghast: a cast trigger with a resolution-time [Effects.MayPay] of
 * `{W/B}` over [Effects.DrainLife]. The second ability watches *any* nontoken creature (yours or an
 * opponent's), so the subject carries no controller restriction; the life loss is the controller's
 * (`EffectTarget.Controller` — `LoseLife` defaults to a target opponent) and is not optional.
 */
val LifeInsurance = card("Life Insurance") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Enchantment"
    oracleText = "$EXTORT_TEXT\nWhenever a nontoken creature dies, you lose 1 life and create a Treasure token."

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.MayPay(cost = ManaCost.parse("{W/B}"), then = Effects.DrainLife(1))
        description = EXTORT_TEXT
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.nontoken()).dies()
        effect = Effects.LoseLife(1, EffectTarget.Controller) then Effects.CreateTreasure()
        description = "Whenever a nontoken creature dies, you lose 1 life and create a Treasure token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Brian Valeza"
        flavorText = "All the money in the world won't buy back a pulse."
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dda58066-d96f-452f-ab83-8635ec478d63.jpg?1783923349"
        ruling("2024-01-12", "You may pay {W/B} a maximum of one time for each extort triggered ability. You decide whether to pay when the ability resolves.")
        ruling("2024-01-12", "The amount of life you gain from extort is based on the total amount of life lost, not necessarily the number of opponents you have. For example, if your opponent's life total can't change (perhaps because that player controls Platinum Emperion), you won't gain any life.")
        ruling("2024-01-12", "The extort ability doesn't target any player.")
        ruling("2024-01-12", "The extort ability resolves before the spell that caused it to trigger. The ability resolves even if that spell is countered.")
    }
}
