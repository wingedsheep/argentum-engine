package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Elesh Norn, Grand Cenobite — New Phyrexia #9
 * {5}{W}{W} · Legendary Creature — Phyrexian Praetor · 4/7
 *
 * Vigilance
 * Other creatures you control get +2/+2.
 * Creatures your opponents control get -2/-2.
 */
val EleshNornGrandCenobite = card("Elesh Norn, Grand Cenobite") {
    manaCost = "{5}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Phyrexian Praetor"
    power = 4
    toughness = 7
    oracleText = "Vigilance\nOther creatures you control get +2/+2.\nCreatures your opponents control get -2/-2."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = ModifyStats(powerBonus = 2, toughnessBonus = 2, filter = GroupFilter.OtherCreaturesYouControl)
    }

    staticAbility {
        ability = ModifyStats(powerBonus = -2, toughnessBonus = -2, filter = GroupFilter.AllCreaturesOpponentsControl)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "9"
        artist = "Igor Kieryluk"
        flavorText = "\"The Gitaxians whisper among themselves of other worlds. If they exist, we must bring Phyrexia's magnificence to them.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b66390d6-1649-4bfa-92d3-77664650d552.jpg?1783941327"
        ruling(
            "2017-11-17",
            "If a second Elesh Norn, Grand Cenobite comes under your control, you'll put one into its owner's graveyard due to the \"legend rule\" at the same time that any of your opponents' creatures getting -4/-4 are put into their owner's graveyard for having 0 or less toughness."
        )
    }
}
