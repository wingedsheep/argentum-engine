package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MakePlottedEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Make Your Own Luck {3}{G}{U}
 * Sorcery
 *
 * Look at the top three cards of your library. You may exile a nonland card from among them.
 * If you do, it becomes plotted. Put the rest into your hand.
 *
 * A gather → choose-up-to → exile-and-plot → rest-to-hand pipeline of atomic effects:
 *  - gather the top three (looked at by you only, not revealed to opponents);
 *  - `ChooseUpTo(1)` over a *nonland* filter is the "you may exile a nonland" optional fork —
 *    picking none leaves the selected slot empty;
 *  - the selected card is moved to exile, then [MakePlottedEffect] (CR 718) stamps the plotted
 *    designation + permanent free-cast-on-a-later-turn permission (the Plot keyword's state
 *    without a plot cost). It no-ops on an empty collection, so declining is safe;
 *  - the remaining cards go to your hand.
 */
val MakeYourOwnLuck = card("Make Your Own Luck") {
    manaCost = "{3}{G}{U}"
    colorIdentity = "UG"
    typeLine = "Sorcery"
    oracleText = "Look at the top three cards of your library. You may exile a nonland card from " +
        "among them. If you do, it becomes plotted. Put the rest into your hand. (You may cast it " +
        "as a sorcery on a later turn without paying its mana cost.)"

    spell {
        effect = Effects.Composite(
            GatherCardsEffect(CardSource.TopOfLibrary(DynamicAmount.Fixed(3)), storeAs = "looked"),
            SelectFromCollectionEffect(
                from = "looked",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                filter = GameObjectFilter.Nonland,
                storeSelected = "toPlot",
                storeRemainder = "toHand",
                selectedLabel = "Exile and plot",
                remainderLabel = "Put into hand"
            ),
            MoveCollectionEffect(from = "toPlot", destination = CardDestination.ToZone(Zone.EXILE)),
            MakePlottedEffect(from = "toPlot"),
            MoveCollectionEffect(from = "toHand", destination = CardDestination.ToZone(Zone.HAND))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Chris Seaman"
        flavorText = "\"Aim alone won't win you a duel if you can't keep your nerve.\"\n—Annie Flash"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/0557b0a3-2b48-408f-a508-9f4da2ab1cd1.jpg?1712356151"

        ruling("2024-04-12", "A plotted card is exiled face up. You may cast it as a sorcery on a later turn without paying its mana cost, but not on the turn it became plotted.")
        ruling("2024-04-12", "If you don't exile a card, all three cards are put into your hand.")
    }
}
