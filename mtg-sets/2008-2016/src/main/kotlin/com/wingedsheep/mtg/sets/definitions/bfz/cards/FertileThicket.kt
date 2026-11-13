package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Fertile Thicket
 * Land
 * This land enters tapped.
 * When this land enters, you may look at the top five cards of your library. If you do, reveal up
 * to one basic land card from among them, then put that card on top of your library and the rest on
 * the bottom in any order.
 * {T}: Add {G}.
 *
 * Munda, Ambush Leader's shape narrowed to one basic land: the "you may look … If you do" is the
 * trigger's optional gate (declining leaves the library untouched). All five looked-at cards are
 * shown, only basic lands are selectable; the chosen one is revealed to opponents and put on top,
 * and the rest go to the bottom in an order the controller chooses. Choosing none sends all five
 * to the bottom, as the ruling says.
 */
val FertileThicket = card("Fertile Thicket") {
    manaCost = ""
    colorIdentity = "G"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "When this land enters, you may look at the top five cards of your library. If you do, reveal up to " +
        "one basic land card from among them, then put that card on top of your library and the rest on " +
        "the bottom in any order.\n" +
        "{T}: Add {G}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(5))
            val (kept, rest) = chooseUpToSplit(
                count = 1,
                from = looked,
                filter = GameObjectFilter.BasicLand,
                prompt = "You may reveal a basic land card to put on top of your library",
                selectedLabel = "Reveal and put on top",
                remainderLabel = "Put on the bottom in any order",
                showAllCards = true
            )
            reveal(kept, revealToSelf = false)
            toLibraryTop(kept)
            toLibraryBottom(rest)
        }
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "237"
        artist = "Andreas Rocha"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15231a9b-1956-4ff6-b637-942444d3349f.jpg?1783938175"
        ruling(
            "2015-08-25",
            "You don't have to reveal a basic land card from among the five cards you look at, even if one is " +
                "there. If you don't, you won't put a card on top of your library."
        )
    }
}
