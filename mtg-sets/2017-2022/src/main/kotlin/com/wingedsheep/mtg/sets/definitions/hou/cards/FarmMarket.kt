package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Farm // Market — Hour of Devastation #148
 *
 * Farm {2}{W} — Instant: Destroy target attacking or blocking creature.
 * Market {2}{U} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Draw two cards, then discard two cards.
 */
val FarmMarket = card("Farm // Market") {
    layout = CardLayout.SPLIT
    colorIdentity = "WU"

    face("Farm") {
        manaCost = "{2}{W}"
        typeLine = "Instant"
        oracleText = "Destroy target attacking or blocking creature."

        spell {
            val creature = target(TargetFilter.AttackingOrBlockingCreature)
            effect = Effects.Destroy(creature)
        }
    }

    face("Market") {
        manaCost = "{2}{U}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Draw two cards, then discard two cards."
        keywords(Keyword.AFTERMATH)

        spell {
            effect = Effects.DrawCards(2) then Patterns.Hand.discardCards(2)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "148"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d998db65-8785-4ee9-940e-fa9ab62e180f.jpg?1783936008"
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
    }
}
