package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sphinx of Clear Skies
 * {3}{U}{U}
 * Creature — Sphinx
 * 5/5
 * Flying, ward {2}
 * Domain — Whenever this creature deals combat damage to a player, reveal the top X cards of your
 * library, where X is the number of basic land types among lands you control. An opponent separates
 * those cards into two piles. Put one pile into your hand and the other into your graveyard.
 *
 * Fact or Fiction's divvy (CR 700.3) with domain as X, counted on resolution. The opponent's
 * split always prompts so either pile may be empty; the controller then picks the hand pile.
 */
val SphinxOfClearSkies = card("Sphinx of Clear Skies") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx"
    power = 5
    toughness = 5
    oracleText = "Flying, ward {2}\nDomain — Whenever this creature deals combat damage to a player, " +
        "reveal the top X cards of your library, where X is the number of basic land types among lands " +
        "you control. An opponent separates those cards into two piles. Put one pile into your hand and " +
        "the other into your graveyard. (Piles can be empty.)"

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val revealed = gather(
                CardSource.TopOfLibrary(count = DynamicAmounts.domain(), player = Player.You),
                revealed = true
            )
            val piles = chooseAnyNumberSplit(
                from = revealed,
                chooser = Chooser.Opponent,
                prompt = "Separate the revealed cards into two piles. The cards you select form Pile 1; the rest form Pile 2.",
                selectedLabel = "Pile 1",
                remainderLabel = "Pile 2",
                alwaysPrompt = true
            )
            val choice = choosePile(
                pileA = piles.selected,
                pileB = piles.remainder,
                chooser = Chooser.Controller,
                prompt = "Choose which pile goes to your hand; the other goes to your graveyard."
            )
            toHand(choice.chosen)
            toGraveyard(choice.other)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "67"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/974c9e0c-07b2-4535-a3d0-bb827b651075.jpg?1783921342"
    }
}
