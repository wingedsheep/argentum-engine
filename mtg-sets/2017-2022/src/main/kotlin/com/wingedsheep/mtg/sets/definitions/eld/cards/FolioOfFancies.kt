package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Folio of Fancies
 * {1}{U}
 * Artifact — Book
 * Players have no maximum hand size.
 * {X}{X}, {T}: Each player draws X cards.
 * {2}{U}, {T}: Each opponent mills cards equal to the number of cards in their hand.
 *
 * The mill iterates each opponent, which rebinds "you" to that opponent, so the hand counted is
 * the milling player's own.
 */
val FolioOfFancies = card("Folio of Fancies") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Book"
    oracleText = "Players have no maximum hand size.\n" +
        "{X}{X}, {T}: Each player draws X cards.\n" +
        "{2}{U}, {T}: Each opponent mills cards equal to the number of cards in their hand."

    staticAbility {
        ability = NoMaximumHandSize(Player.Each)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}{X}"), Costs.Tap)
        effect = Effects.ForEachPlayer(Player.ActivePlayerFirst, Effects.DrawCards(DynamicAmounts.xValue()))
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}"), Costs.Tap)
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            Patterns.Library.mill(DynamicAmounts.cardsInYourHand())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Colin Boyer"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6afc67d1-1018-4a15-ab5f-377fd11dcd3d.jpg?1783932658"
    }
}
