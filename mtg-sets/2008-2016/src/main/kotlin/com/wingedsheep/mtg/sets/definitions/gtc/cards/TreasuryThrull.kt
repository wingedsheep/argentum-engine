package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

private const val EXTORT_TEXT = "Extort (Whenever you cast a spell, you may pay {W/B}. If you do, " +
    "each opponent loses 1 life and you gain that much life.)"

/**
 * Treasury Thrull
 * {4}{W}{B}
 * Creature — Thrull
 * 4/4
 * Extort (Whenever you cast a spell, you may pay {W/B}. If you do, each opponent loses 1 life and
 * you gain that much life.)
 * Whenever this creature attacks, you may return target artifact, creature, or enchantment card from
 * your graveyard to your hand.
 *
 * Extort is composed as on Basilica Screecher. The attack trigger is a targeted graveyard return, so
 * plain [Effects.ReturnToHand] is right: the target's `zone = GRAVEYARD` is re-checked on resolution.
 *
 * Canonical printing: Gatecrash, the card's earliest real printing (reprinted in C15).
 */
val TreasuryThrull = card("Treasury Thrull") {
    manaCost = "{4}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Thrull"
    power = 4
    toughness = 4
    oracleText = "$EXTORT_TEXT\nWhenever this creature attacks, you may return target artifact, creature, " +
        "or enchantment card from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.MayPay(cost = ManaCost.parse("{W/B}"), then = Effects.DrainLife(1))
        description = EXTORT_TEXT
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val card = target(
            TargetFilter(
                (GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Enchantment).ownedByYou(),
                zone = Zone.GRAVEYARD,
            )
        )
        effect = Effects.May(Effects.ReturnToHand(card))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "201"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f013e6f0-85d0-4c8e-a10b-7beea572c32d.jpg?1783940099"
        ruling("2024-01-12", "You may pay {W/B} a maximum of one time for each extort triggered ability. You decide whether to pay when the ability resolves.")
        ruling("2024-01-12", "The amount of life you gain from extort is based on the total amount of life lost, not necessarily the number of opponents you have. For example, if your opponent's life total can't change (perhaps because that player controls Platinum Emperion), you won't gain any life.")
        ruling("2024-01-12", "The extort ability doesn't target any player.")
        ruling("2024-01-12", "The extort ability resolves before the spell that caused it to trigger. The ability resolves even if that spell is countered.")
    }
}
