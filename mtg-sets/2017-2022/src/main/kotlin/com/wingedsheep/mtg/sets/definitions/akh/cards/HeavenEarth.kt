package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Heaven // Earth — Amonkhet #224
 *
 * Heaven {X}{G} — Instant: Heaven deals X damage to each creature with flying.
 * Earth {X}{R}{R} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Earth deals X damage to each creature without flying.
 *
 * Each half chooses its own X when cast.
 */
val HeavenEarth = card("Heaven // Earth") {
    layout = CardLayout.SPLIT
    colorIdentity = "GR"

    face("Heaven") {
        manaCost = "{X}{G}"
        typeLine = "Instant"
        oracleText = "Heaven deals X damage to each creature with flying."

        spell {
            effect = Patterns.Group.dealDamageToAll(
                DynamicAmounts.xValue(),
                GroupFilter.AllCreatures.withKeyword(Keyword.FLYING)
            )
        }
    }

    face("Earth") {
        manaCost = "{X}{R}{R}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Earth deals X damage to each creature without flying."
        keywords(Keyword.AFTERMATH)

        spell {
            effect = Patterns.Group.dealDamageToAll(
                DynamicAmounts.xValue(),
                GroupFilter.AllCreatures.withoutKeyword(Keyword.FLYING)
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "224"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe1a4032-efbb-4f72-9181-994b2b35f598.jpg?1783936452"
        ruling("2017-04-18", "You don't have to choose the same value for X while casting Earth as you did while casting Heaven.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
        ruling("2017-04-18", "If another effect allows you to cast a split card with aftermath from any zone other than a graveyard, you can't cast the half with aftermath.")
    }
}
