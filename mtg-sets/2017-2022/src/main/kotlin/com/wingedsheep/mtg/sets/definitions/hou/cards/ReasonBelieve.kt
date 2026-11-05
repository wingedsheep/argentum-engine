package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Reason // Believe — Hour of Devastation #154
 *
 * Reason {U} — Sorcery: Scry 3.
 * Believe {4}{G} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Look at the top card of your library. You may put it onto the battlefield if it's a creature
 *   card. If you don't, put it into your hand.
 *
 * Believe looks at the top card privately and offers it for the battlefield only when it's a
 * creature card; whatever wasn't put onto the battlefield (a noncreature, or a declined creature)
 * goes to hand unrevealed — both per the 2017-07-14 rulings.
 */
val ReasonBelieve = card("Reason // Believe") {
    layout = CardLayout.SPLIT
    colorIdentity = "UG"

    face("Reason") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        oracleText = "Scry 3."

        spell {
            effect = Effects.Scry(3)
        }
    }

    face("Believe") {
        manaCost = "{4}{G}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Look at the top card of your library. You may put it onto the battlefield if it's a creature card. " +
            "If you don't, put it into your hand."
        keywords(Keyword.AFTERMATH)

        spell {
            effect = Effects.Pipeline {
                val looked = gather(CardSource.TopOfLibrary(1))
                val (toBattlefield, rest) = chooseUpToSplit(
                    1,
                    from = looked,
                    filter = GameObjectFilter.Creature,
                    prompt = "You may put the top card of your library onto the battlefield",
                    selectedLabel = "Put onto the battlefield",
                    remainderLabel = "Put into your hand"
                )
                move(toBattlefield, CardDestination.ToZone(Zone.BATTLEFIELD))
                toHand(rest)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "154"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1ca644e3-4fb3-4d38-b714-e3d7459bd8b9.jpg?1783936006"
        ruling("2017-07-14", "For Believe, you put the top card of your library into your hand if you don't put it onto the battlefield for any reason, whether it's not a creature card or whether you just didn't want to put it onto the battlefield.")
        ruling("2017-07-14", "If you don't put the top card of your library onto the battlefield, you don't reveal it before putting it into your hand.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
    }
}
