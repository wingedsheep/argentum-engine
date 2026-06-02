package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.Costs

/**
 * Champion of the Clachan
 * {3}{W}
 * Creature — Kithkin Knight
 * 4/5
 *
 * Flash
 * As an additional cost to cast this spell, behold a Kithkin and exile it.
 * (Exile a Kithkin you control or a Kithkin card from your hand.)
 * Other Kithkin you control get +1/+1.
 * When this creature leaves the battlefield, return the exiled card to its owner's hand.
 */
val ChampionOfTheClachan = card("Champion of the Clachan") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kithkin Knight"
    power = 4
    toughness = 5
    oracleText = "Flash\n" +
        "As an additional cost to cast this spell, behold a Kithkin and exile it. " +
        "(Exile a Kithkin you control or a Kithkin card from your hand.)\n" +
        "Other Kithkin you control get +1/+1.\n" +
        "When this creature leaves the battlefield, return the exiled card to its owner's hand."

    keywords(Keyword.FLASH)

    additionalCost(Costs.additional.BeholdAndExile(filter = Filters.WithSubtype("Kithkin")))

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype("Kithkin").youControl(),
                excludeSelf = true
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileToHand()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Edgar Sánchez Hidalgo"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46ce3474-381c-433b-acd8-4e628d0048d2.jpg?1767692155"
    }
}
