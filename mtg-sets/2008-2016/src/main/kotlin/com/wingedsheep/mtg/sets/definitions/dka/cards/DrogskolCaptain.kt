package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Drogskol Captain
 * {1}{W}{U}
 * Creature — Spirit Soldier
 * 2/2
 * Flying
 * Other Spirit creatures you control get +1/+1 and have hexproof.
 */
val DrogskolCaptain = card("Drogskol Captain") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Spirit Soldier"
    power = 2
    toughness = 2
    oracleText = "Flying\nOther Spirit creatures you control get +1/+1 and have hexproof. (They can't be the targets of spells or abilities your opponents control.)"

    keywords(Keyword.FLYING)

    val otherSpirits = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.SPIRIT).youControl(), excludeSelf = true)
    staticAbility {
        ability = ModifyStats(powerBonus = 1, toughnessBonus = 1, filter = otherSpirits)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.HEXPROOF, otherSpirits)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Peter Mohrbacher"
        flavorText = "Dead or alive, true leaders can inspire an entire army."
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8238e36-625f-460d-9e39-fd501e65490c.jpg"
    }
}
