package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

private const val EXTORT_TEXT = "Extort (Whenever you cast a spell, you may pay {W/B}. If you do, " +
    "each opponent loses 1 life and you gain that much life.)"

/**
 * Thrull Parasite
 * {B}
 * Creature — Thrull
 * 1/1
 * Extort (Whenever you cast a spell, you may pay {W/B}. If you do, each opponent loses 1 life and
 * you gain that much life.)
 * {T}, Pay 2 life: Remove a counter from target nonland permanent.
 *
 * Extort is composed as on Basilica Screecher. The activated ability removes exactly one counter
 * of the player's choice of kind ([Effects.RemoveCounterOfAnyKind]); the target needn't carry any
 * counters (2013-01-24 ruling), in which case nothing is removed.
 */
val ThrullParasite = card("Thrull Parasite") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Thrull"
    power = 1
    toughness = 1
    oracleText = "$EXTORT_TEXT\n{T}, Pay 2 life: Remove a counter from target nonland permanent."

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.MayPay(cost = ManaCost.parse("{W/B}"), then = Effects.DrainLife(1))
        description = EXTORT_TEXT
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(2))
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.RemoveCounterOfAnyKind(target = permanent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Clint Cearley"
        flavorText = "The hunger of a vampire and the subtlety of a tax collector."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c38dfb08-ab41-4759-9967-b5a25f18518a.jpg"
        ruling("2024-01-12", "You may pay {W/B} a maximum of one time for each extort triggered ability. You decide whether to pay when the ability resolves.")
        ruling("2024-01-12", "The amount of life you gain from extort is based on the total amount of life lost, not necessarily the number of opponents you have. For example, if your opponent's life total can't change (perhaps because that player controls Platinum Emperion), you won't gain any life.")
        ruling("2024-01-12", "The extort ability doesn't target any player.")
        ruling("2024-01-12", "The extort ability resolves before the spell that caused it to trigger. The ability resolves even if that spell is countered.")
        ruling("2013-01-24", "The nonland permanent you choose as the target doesn't have to have any counters on it. If it has more than one kind of counter, you'll choose one of those counters when the ability resolves.")
        ruling("2013-01-24", "You can't activate Thrull Parasite's last ability to stop a player from paying the cost of a spell or an ability that requires removing a counter from a permanent (such as a planeswalker's loyalty ability).")
    }
}
