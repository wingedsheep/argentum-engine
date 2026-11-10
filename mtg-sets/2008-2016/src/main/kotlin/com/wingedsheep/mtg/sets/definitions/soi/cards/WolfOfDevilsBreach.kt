package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wolf of Devil's Breach (SOI #192) — {3}{R}{R} Creature — Elemental Wolf 5/5.
 *
 *   Whenever this creature attacks, you may pay {1}{R} and discard a card. If you do, this
 *   creature deals damage to target creature or planeswalker equal to the discarded card's
 *   mana value.
 *
 * The target is chosen as the trigger goes on the stack; the pay-and-discard is offered on
 * resolution (ruling). The two-part cost is `PayMana then Discard` behind a cards-in-hand check
 * — the gate's affordability test knows the mana half but not the discard half, so an empty hand
 * must not be offered the "yes" (you can't pay {1}{R} without also discarding — ruling). The
 * discard stores its card in [Patterns.Hand.discarded], which the damage reads as its mana value
 * (an {X} card counts X as 0).
 */
val WolfOfDevilsBreach = card("Wolf of Devil's Breach") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Wolf"
    oracleText = "Whenever this creature attacks, you may pay {1}{R} and discard a card. If you do, " +
        "this creature deals damage to target creature or planeswalker equal to the discarded " +
        "card's mana value."
    power = 5
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.If(
            condition = Conditions.CardsInHandAtLeast(1),
            then = Effects.MayPay(
                cost = Effects.PayMana("{1}{R}") then Effects.Discard(1),
                then = Effects.DealDamage(DynamicAmounts.manaValueOf(Patterns.Hand.discarded), victim),
                descriptionOverride = "You may pay {1}{R} and discard a card. If you do, this creature " +
                    "deals damage to target creature or planeswalker equal to the discarded card's mana value."
            )
        )
        description = "Whenever this creature attacks, you may pay {1}{R} and discard a card. If you do, " +
            "this creature deals damage to target creature or planeswalker equal to the discarded " +
            "card's mana value."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "192"
        artist = "Jack Wang"
        flavorText = "\"How can werewolves be considered abominations next to such creatures?\"\n—Arlinn Kord"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fedce04c-e7bc-4db0-94a1-66131761512c.jpg?1783937738"
        ruling("2016-04-08", "You can't choose to discard a card without also paying {1}{R}.")
        ruling(
            "2016-04-08",
            "If a card with {X} in its mana cost is discarded to Wolf of Devil's Breach's ability, X is " +
                "considered to be 0."
        )
        ruling(
            "2016-04-08",
            "You choose the target creature or planeswalker as the triggered ability of Wolf of Devil's " +
                "Breach is put onto the stack. You choose which card to discard, if any, as the ability " +
                "resolves. While players may respond to the triggered ability once you've chosen a target, " +
                "no player may take actions between the time you discard the card and the time damage is dealt."
        )
    }
}
