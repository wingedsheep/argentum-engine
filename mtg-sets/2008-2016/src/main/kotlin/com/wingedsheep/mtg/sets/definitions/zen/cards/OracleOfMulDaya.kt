package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary
import com.wingedsheep.sdk.scripting.RevealTopOfLibrary

/**
 * Oracle of Mul Daya — Zendikar #172
 * {3}{G} · Creature — Elf Shaman · 2/2
 *
 * You may play an additional land on each of your turns.
 * Play with the top card of your library revealed.
 * You may play lands from the top of your library.
 *
 * Exploration's [GrantAdditionalLandDrop] plus Courser of Kruphix's pair: the public
 * [RevealTopOfLibrary] and the lands-only [PlayLandsAndCastFilteredFromTopOfLibrary]
 * (`spellFilter = null`). A land played from the top uses one of the turn's land plays.
 */
val OracleOfMulDaya = card("Oracle of Mul Daya") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Shaman"
    power = 2
    toughness = 2
    oracleText = "You may play an additional land on each of your turns.\n" +
        "Play with the top card of your library revealed.\n" +
        "You may play lands from the top of your library."

    staticAbility {
        ability = GrantAdditionalLandDrop(count = 1)
    }

    staticAbility { ability = RevealTopOfLibrary }

    staticAbility {
        ability = PlayLandsAndCastFilteredFromTopOfLibrary(spellFilter = null)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "172"
        artist = "Vance Kovacs"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f89a173-0b2f-4a6a-b706-9aed8dbcabec.jpg?1783942133"
        ruling(
            "2009-10-01",
            "If you control more than one Oracle of Mul Daya, the effects of their first abilities are " +
                "cumulative. If you control two, for example, you can play three lands on your turn.",
        )
        ruling(
            "2009-10-01",
            "Oracle of Mul Daya doesn't change the times when you can play a land card from the top of your " +
                "library. You can play a land only during your main phase when you have priority and the stack " +
                "is empty. Doing so counts as one of your land plays for the turn.",
        )
        ruling(
            "2009-10-01",
            "If you play your first land of the turn from the top of your library, and the new top card is " +
                "another land card, you can play that one too.",
        )
    }
}
