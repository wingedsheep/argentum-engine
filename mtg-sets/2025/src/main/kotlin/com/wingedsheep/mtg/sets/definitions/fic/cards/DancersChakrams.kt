package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.jobSelect
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantStaticAbility
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Dancer's Chakrams (FIC #17)
 * {3}{W}
 * Artifact — Equipment
 * Job select (When this Equipment enters, create a 1/1 colorless Hero creature token, then attach
 * this to it.)
 * Equipped creature gets +2/+2, has lifelink and "Other commanders you control get +2/+2 and have
 * lifelink," and is a Performer in addition to its other types.
 * Krishna — Equip {3}
 *
 * The job-select shell (Paladin's Arms) plus a granted lord static: [GrantStaticAbility] puts the
 * quoted ability on the equipped creature, so "other" and "you control" are read relative to that
 * creature (Rune of Might's holder rule). "Commanders" is the commander designation
 * (`commander()`, CR 903.3) on any permanent, not just creatures. Per the ruling the granted
 * ability works whether or not the equipped creature is itself a commander.
 */
val DancersChakrams = card("Dancer's Chakrams") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "Job select (When this Equipment enters, create a 1/1 colorless Hero creature token, then attach this to it.)\n" +
        "Equipped creature gets +2/+2, has lifelink and \"Other commanders you control get +2/+2 and have lifelink,\" " +
        "and is a Performer in addition to its other types.\n" +
        "Krishna — Equip {3}"

    jobSelect()

    staticAbility {
        ability = ModifyStats(2, 2, Filters.EquippedCreature)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.LIFELINK, Filters.EquippedCreature)
    }
    staticAbility {
        val otherCommandersYouControl =
            GroupFilter(GameObjectFilter.Permanent.commander().youControl(), excludeSelf = true)
        ability = GrantStaticAbility(
            CompositeStaticAbility(
                listOf(
                    ModifyStats(2, 2, otherCommandersYouControl),
                    GrantKeyword(Keyword.LIFELINK, otherCommandersYouControl),
                )
            ),
            Filters.EquippedCreature,
        )
    }
    staticAbility {
        ability = GrantSubtype("Performer", Filters.EquippedCreature)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "17"
        artist = "Domco."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27ec595f-406b-4539-8d2e-eb98df1a0fc9.jpg?1783906369"

        ruling("2025-06-06", "The last ability granted by Dancer's Chakrams to the equipped creature applies whether or not Dancer's Chakrams is attached to a commander.")
        ruling("2025-06-06", "If the job select ability causes two Hero tokens to be created (due to an effect such as that of Doubling Season), the Equipment becomes attached to only one of them.")
    }
}
