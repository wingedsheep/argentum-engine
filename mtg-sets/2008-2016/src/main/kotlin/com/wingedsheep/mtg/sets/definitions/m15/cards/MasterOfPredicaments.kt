package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Master of Predicaments — Magic 2015 #67
 * {3}{U}{U} · Creature — Sphinx · 4/4 · Rare
 *
 * Flying
 * Whenever this creature deals combat damage to a player, choose a card in your hand. That player
 * guesses whether the card's mana value is greater than 4. If the player guessed wrong, you may cast
 * the card without paying its mana cost.
 *
 * Modelling notes:
 * - "Choose a card in your hand" is a private `chooseExactly(1)` over the hand — the ruling says the
 *   card is set aside *without revealing it*, and nothing in the selection is shown to the opponent.
 *   An empty hand chooses nothing, so there is no card to guess about and the rest does nothing.
 * - "That player" is the damaged player: [Chooser.TriggeringPlayer] on a combat-damage-to-a-player
 *   trigger. [Effects.PlayerGuessesCondition] scores the guess against the chosen card's real mana
 *   value only after the answer is in, and reveals nothing — per the ruling, a card you don't cast
 *   (a right guess, or a declined cast) stays hidden.
 * - The cast is the `chooseSpell` → `CastFromCollectionWithoutPayingCost` tail: choosing nothing is
 *   the "may" decline, a land is never offered (it can't be cast, per the ruling), a split card offers
 *   either half (or the fused whole) after having been guessed at by its combined mana value, and the
 *   cast happens during the trigger's resolution with type-based timing ignored.
 */
val MasterOfPredicaments = card("Master of Predicaments") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx"
    power = 4
    toughness = 4
    oracleText = "Flying\nWhenever this creature deals combat damage to a player, choose a card in your " +
        "hand. That player guesses whether the card's mana value is greater than 4. If the player " +
        "guessed wrong, you may cast the card without paying its mana cost."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val predicament = chooseExactly(
                1,
                from = hand,
                prompt = "Choose a card in your hand",
                selectedLabel = "Chosen card",
            )
            ifNotEmpty(predicament) {
                run(
                    Effects.PlayerGuessesCondition(
                        condition = Conditions.CompareAmounts(
                            DynamicAmounts.manaValueOf(predicament),
                            ComparisonOperator.GT,
                            4,
                        ),
                        prompt = "Is the mana value of the card your opponent chose from their hand " +
                            "greater than 4?",
                        storeGuessedRightAs = "predicamentGuessedRight",
                        guesser = Chooser.TriggeringPlayer,
                    )
                )
                ifThen(
                    Conditions.CompareAmounts(
                        DynamicAmounts.storedNumber("predicamentGuessedRight"),
                        ComparisonOperator.EQ,
                        0,
                    )
                ) {
                    val toCast = chooseSpell(
                        from = predicament,
                        prompt = "Your opponent guessed wrong. You may cast the chosen card without " +
                            "paying its mana cost.",
                        selectedLabel = "Cast without paying its mana cost",
                    )
                    run(Effects.CastFromCollectionWithoutPayingCost(toCast))
                }
            }
        }
        description = "Whenever this creature deals combat damage to a player, choose a card in your " +
            "hand. That player guesses whether the card's mana value is greater than 4. If the player " +
            "guessed wrong, you may cast the card without paying its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "67"
        artist = "Matt Stewart"
        flavorText = "Designed by David Sirlin"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0b1720b-ab7a-4b52-a6d0-11c4fc573d17.jpg?1783939190"
        ruling(
            "2017-04-18",
            "If the card is a split card, its mana value is determined by the combined mana cost of its " +
                "two halves. If the player guesses wrong and you cast a split card this way, you may cast " +
                "either half (or both halves if the card has fuse), no matter what its mana value is."
        )
        ruling(
            "2014-07-18",
            "Although you may choose a land card in your hand, you won't be able to play it even if the " +
                "player guessed wrong."
        )
        ruling(
            "2014-07-18",
            "If you don't cast the card, either because the player guessed right or because you choose " +
                "not to cast it, you don't have to reveal it. Your opponent won't know the reason you " +
                "didn't cast the card."
        )
        ruling(
            "2014-07-18",
            "To choose a card in your hand, clearly separate it from the rest of your hand without revealing it."
        )
        ruling(
            "2014-07-18",
            "You cast the card in your hand as part of the resolution of the triggered ability. Timing " +
                "restrictions based on the card's type (such as creature or sorcery) are ignored. Other " +
                "restrictions, such as \"Cast [this card] only during combat,\" are not."
        )
        ruling(
            "2014-07-18",
            "If you cast a card \"without paying its mana cost,\" you can't pay any alternative costs. " +
                "Notably, you can't cast a card with bestow as an Aura this way. You can, however, pay " +
                "additional costs, such as kicker costs. If the card has any mandatory additional costs, " +
                "you must pay those."
        )
        ruling("2014-07-18", "If the card has {X} in its mana cost, X is 0.")
    }
}
