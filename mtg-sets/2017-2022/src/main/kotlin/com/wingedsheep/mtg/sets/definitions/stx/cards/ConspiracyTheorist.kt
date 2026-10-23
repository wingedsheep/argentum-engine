package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Conspiracy Theorist — Strixhaven: School of Mages #94 (canonical printing)
 * {1}{R} · Creature — Human Shaman · 2/2
 *
 * Whenever this creature attacks, you may pay {1} and discard a card. If you do, draw a card.
 * Whenever you discard one or more nonland cards, you may exile one of them from your graveyard.
 * If you do, you may cast it this turn.
 *
 * The attack trigger is the Sokenzan Smelter two-part optional cost — one [Effects.MayPay] over
 * `PayMana("{1}") then Discard(1)` — behind a cards-in-hand check: the gate's affordability test
 * knows the mana half but not the discard half, so with an empty hand the "yes" would be offered
 * and the draw would follow an unpaid cost. It is offered once per resolution (ruling).
 *
 * The discard payoff is the batch discard trigger (`discards(Nonland, batch = true)`, CR 603.2c):
 * one firing per discard event, its matching cards seeded as [triggerCaptured]. Those still in the
 * graveyard are the "them" to pick from; `chooseUpTo(1)` is the "you may", and the exiled card
 * gets a this-turn cast permission (`nonLandOnly` — "cast", never "play"). Casting it follows the
 * card's normal timing and costs (ruling).
 */
val ConspiracyTheorist = card("Conspiracy Theorist") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    oracleText = "Whenever this creature attacks, you may pay {1} and discard a card. If you do, " +
        "draw a card.\n" +
        "Whenever you discard one or more nonland cards, you may exile one of them from your " +
        "graveyard. If you do, you may cast it this turn."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.If(
            condition = Conditions.CardsInHandAtLeast(1),
            then = Effects.MayPay(
                cost = Effects.PayMana("{1}") then Effects.Discard(1),
                then = Effects.DrawCards(1),
                descriptionOverride = "You may pay {1} and discard a card. If you do, draw a card."
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.you.discards(GameObjectFilter.Nonland, batch = true)
        effect = Effects.Pipeline {
            val stillInGraveyard = filter(triggerCaptured, GameObjectFilter.Any.currentlyIn(Zone.GRAVEYARD))
            val chosen = chooseUpTo(
                1,
                from = stillInGraveyard,
                prompt = "You may exile one of the discarded cards. You may cast it this turn."
            )
            exile(chosen)
            run(Effects.GrantMayPlayFromExile(chosen, MayPlayExpiry.EndOfTurn, nonLandOnly = true))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/742a7487-70a2-448b-8e66-c33cba798a32.jpg?1783927357"
        ruling("2021-04-16", "While resolving Conspiracy Theorist's ability, you can't pay {1} and discard a card multiple times to draw more cards.")
        ruling("2021-04-16", "The card exiled by the second ability is exiled face up. Casting it follows the normal rules for casting that card. You must pay its costs, and you must follow all applicable timing rules. For example, if it's a creature card, you can cast it only during your main phase while the stack is empty. In most cases, if you exile a non-instant card during an opponent's turn, you won't be able to cast it that turn.")
    }
}
