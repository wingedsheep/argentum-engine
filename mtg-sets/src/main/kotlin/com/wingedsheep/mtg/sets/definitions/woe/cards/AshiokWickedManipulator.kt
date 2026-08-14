package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ReplaceLifePaymentWithLibraryExile
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Ashiok, Wicked Manipulator
 * {3}{B}{B}
 * Legendary Planeswalker — Ashiok
 * Loyalty 5
 *
 * If you would pay life while your library has at least that many cards in it, exile that many
 * cards from the top of your library instead.
 * +1: Look at the top two cards of your library. Exile one of them and put the other into your hand.
 * −2: Create two 1/1 black Nightmare creature tokens with "At the beginning of combat on your turn,
 *     if a card was put into exile this turn, put a +1/+1 counter on this token."
 * −7: Target player exiles the top X cards of their library, where X is the total mana value of
 *     cards you own in exile.
 *
 * Modeling notes:
 *  - The static ability is a genuine replacement effect, not a triggered one, so it uses the
 *    [ReplaceLifePaymentWithLibraryExile] replacement on the new `LifePaymentEvent` pattern. Every
 *    life *payment* in the engine funnels through `LifePaymentService`, which consults this before
 *    deducting life — so it covers cost atoms, additional casting costs, ward and Phyrexian-style
 *    payments, pain-cost mana abilities and the `PayLife` resolution effects alike. Life *loss*
 *    (damage, "you lose N life") keeps its own path untouched, which is exactly the printed
 *    reminder text: damage and unpayable costs still cause you to lose life.
 *  - The +1 is the Gather → Select → Move pipeline with the two destinations swapped relative to
 *    a normal impulse-draw: the *selected* card goes to hand and the remainder is exiled. That is
 *    the same choice the oracle text asks for ("exile one of them and put the other into your
 *    hand") — with two cards, choosing which to keep and choosing which to exile are the same
 *    decision. A library with only one card left still works: you look at what's there and keep it.
 *  - The −2 tokens carry a triggered ability whose intervening "if" (CR 603.4) reuses
 *    [Conditions.CardsPutIntoExileThisTurn], the game-wide `CARDS_PUT_INTO_EXILE` turn tracker
 *    built for Ennis, Debate Moderator. Ashiok's own static ability and +1 both feed it, and so
 *    does an opponent's exiling. `Triggers.BeginCombat` is already "at the beginning of combat on
 *    your turn" (a `StepEvent(BEGIN_COMBAT, Player.You)`), so the tokens only check on your turn.
 *  - The −7's X is the total mana value of cards *you own* in exile — the engine keys the exile
 *    zone by owner, so `DynamicAmounts.zone(Player.You, Zone.EXILE).sumManaValue()` is exactly
 *    that, and it is evaluated against Ashiok's controller even though the exiling player is the
 *    target.
 */
val AshiokWickedManipulator = card("Ashiok, Wicked Manipulator") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Planeswalker — Ashiok"
    startingLoyalty = 5
    oracleText = "If you would pay life while your library has at least that many cards in it, " +
        "exile that many cards from the top of your library instead.\n" +
        "+1: Look at the top two cards of your library. Exile one of them and put the other into " +
        "your hand.\n" +
        "−2: Create two 1/1 black Nightmare creature tokens with \"At the beginning of combat on " +
        "your turn, if a card was put into exile this turn, put a +1/+1 counter on this token.\"\n" +
        "−7: Target player exiles the top X cards of their library, where X is the total mana " +
        "value of cards you own in exile."

    // If you would pay life while your library has at least that many cards in it, exile that
    // many cards from the top of your library instead.
    replacementEffect(ReplaceLifePaymentWithLibraryExile())

    // +1: Look at the top two cards of your library. Exile one of them and put the other into
    //     your hand.
    loyaltyAbility(+1) {
        effect = Patterns.Library.lookAtTopAndKeep(
            count = 2,
            keepCount = 1,
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.EXILE),
        )
    }

    // −2: Create two 1/1 black Nightmare creature tokens with "At the beginning of combat on your
    //     turn, if a card was put into exile this turn, put a +1/+1 counter on this token."
    loyaltyAbility(-2) {
        effect = CreateTokenEffect(
            count = DynamicAmount.Fixed(2),
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Nightmare"),
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = Triggers.BeginCombat.event,
                    binding = Triggers.BeginCombat.binding,
                    triggerCondition = Conditions.CardsPutIntoExileThisTurn(),
                    effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                    descriptionOverride = "At the beginning of combat on your turn, if a card was " +
                        "put into exile this turn, put a +1/+1 counter on this token.",
                ),
            ),
            imageUri = "https://cards.scryfall.io/normal/front/8/5/858428ee-3a9c-4dfc-84c2-751e59df2ed7.jpg?1783914991",
        )
    }

    // −7: Target player exiles the top X cards of their library, where X is the total mana value
    //     of cards you own in exile.
    loyaltyAbility(-7) {
        target("target player", Targets.Player)
        effect = Patterns.Library.exileTop(
            count = DynamicAmounts.zone(Player.You, Zone.EXILE).sumManaValue(),
            target = EffectTarget.ContextTarget(0),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "78"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c4d0db1-74a5-42c0-ac95-e696585d8022.jpg?1783915111"

        ruling("2023-09-01", "Ashiok, Wicked Nightmare's first ability isn't optional. You can't choose to pay life instead of exiling cards from the top of your library while you control Ashiok, and you can't split the payment between life and cards.")
        ruling("2023-09-01", "If you would pay life while you control Ashiok and your library does not have at least that many cards in it, you'll just pay life as normal.")
        ruling("2023-09-01", "Ashiok's first ability doesn't allow you to attempt to pay an amount of life greater than your current life total.")
    }
}
