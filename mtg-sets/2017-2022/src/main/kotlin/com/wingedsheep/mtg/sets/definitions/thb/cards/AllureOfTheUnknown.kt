package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Allure of the Unknown
 * {3}{B}{R}
 * Sorcery
 * Reveal the top six cards of your library. An opponent exiles a nonland card from among them,
 * then you put the rest into your hand. That opponent may cast the exiled card without paying
 * its mana cost.
 *
 * [Effects.ChooseOpponent] pins "an opponent" first (Devastating Mastery's shape) so the player
 * who exiles the card is the same one offered the free cast — [Chooser.ChosenOpponent] for both
 * the exile choice and the `caster`. The opponent's cast happens during resolution or not at all
 * (ruling); declining leaves the card in exile. Six revealed lands mean nothing is exiled and all
 * six go to hand.
 */
val AllureOfTheUnknown = card("Allure of the Unknown") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "Reveal the top six cards of your library. An opponent exiles a nonland card from " +
        "among them, then you put the rest into your hand. That opponent may cast the exiled card " +
        "without paying its mana cost."

    spell {
        effect = Effects.Pipeline {
            run(Effects.ChooseOpponent("Choose an opponent to exile a nonland card"))
            val revealed = gather(CardSource.TopOfLibrary(6, Player.You))
            reveal(revealed)
            val exiled = chooseExactly(
                1,
                from = revealed,
                chooser = Chooser.ChosenOpponent,
                filter = GameObjectFilter.Nonland,
                prompt = "Exile a nonland card from among the revealed cards",
                selectedLabel = "Exile",
                remainderLabel = "Put into their hand",
                showAllCards = true
            )
            exile(exiled)
            val rest = filter(revealed, GameObjectFilter.Any.currentlyIn(Zone.LIBRARY))
            toHand(rest)
            val toCast = chooseSpell(
                from = exiled,
                chooser = Chooser.ChosenOpponent,
                prompt = "You may cast the exiled card without paying its mana cost",
                selectedLabel = "Cast without paying its mana cost",
                remainderLabel = "Leave in exile",
                showAllCards = true
            )
            run(Effects.CastFromCollectionWithoutPayingCost(toCast, caster = Chooser.ChosenOpponent))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "207"
        artist = "Seb McKinnon"
        flavorText = "The light of hope blinded Pantor to the ills of the world."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f77e09c3-917c-4bbb-bb94-60f4e9417832.jpg?1783931526"
        ruling(
            "2020-01-24",
            "If all six of the revealed cards are land cards, your opponent doesn't exile anything. You " +
                "put all six cards into your hand."
        )
        ruling(
            "2020-01-24",
            "If your opponent wishes to cast the card, they do so as part of the resolution of Allure of " +
                "the Unknown. They can't wait to cast it later. Timing permissions based on a card's type " +
                "are ignored."
        )
    }
}
