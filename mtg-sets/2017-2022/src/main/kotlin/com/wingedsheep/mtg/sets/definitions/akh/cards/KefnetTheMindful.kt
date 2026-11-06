package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Kefnet the Mindful
 * {2}{U}
 * Legendary Creature — God
 * 5/5
 * Flying, indestructible
 * Kefnet can't attack or block unless you have seven or more cards in hand.
 * {3}{U}: Draw a card, then you may return a land you control to its owner's hand.
 *
 * The combat restriction is the Hazoret/Rhonas/Bontu pair of statics over one condition — the
 * controller's hand size compared against 7. The activated ability draws first, then offers an
 * optional, untargeted choice of up to one land you control (per the ruling, chosen after seeing
 * the drawn card).
 */
val KefnetTheMindful = card("Kefnet the Mindful") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — God"
    power = 5
    toughness = 5
    oracleText = "Flying, indestructible\n" +
        "Kefnet can't attack or block unless you have seven or more cards in hand.\n" +
        "{3}{U}: Draw a card, then you may return a land you control to its owner's hand."

    keywords(Keyword.FLYING, Keyword.INDESTRUCTIBLE)

    val sevenOrMoreCardsInHand = Conditions.CompareAmounts(
        DynamicAmounts.cardsInYourHand(),
        ComparisonOperator.GTE,
        7
    )

    staticAbility {
        ability = CantAttackUnless(sevenOrMoreCardsInHand)
    }
    staticAbility {
        ability = CantBlockUnless(sevenOrMoreCardsInHand)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{U}")
        effect = Effects.Pipeline {
            run(Effects.DrawCards(1))
            val lands = gather(CardSource.BattlefieldMatching(filter = GameObjectFilter.Land, player = Player.You))
            val chosen = chooseUpTo(
                1,
                from = lands,
                prompt = "You may return a land you control to its owner's hand",
                useTargetingUI = true
            )
            toHand(chosen)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "59"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbda3b24-f2a6-4bcf-9a0a-bab69ffa8069.jpg?1783936521"
        ruling("2017-04-18", "Once Kefnet has attacked or blocked, it will remain in combat even if the number of cards in your hand becomes six or fewer.")
        ruling("2017-04-18", "While resolving Kefnet's activated ability, you don't choose which land to return to its owner's hand (or whether you'll return one at all) until you see the card you draw.")
    }
}
