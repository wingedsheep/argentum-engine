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
 * Stromkirk Captain
 * {1}{B}{R}
 * Creature — Vampire Soldier
 * 2/2
 * First strike
 * Other Vampire creatures you control get +1/+1 and have first strike.
 */
val StromkirkCaptain = card("Stromkirk Captain") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Vampire Soldier"
    oracleText = "First strike\nOther Vampire creatures you control get +1/+1 and have first strike."
    power = 2
    toughness = 2

    keywords(Keyword.FIRST_STRIKE)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.VAMPIRE).youControl(),
                excludeSelf = true
            )
        )
    }

    staticAbility {
        ability = GrantKeyword(
            Keyword.FIRST_STRIKE,
            GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.VAMPIRE).youControl(),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Jana Schirmer & Johannes Voss"
        flavorText = "\"No longer can we allow our human populations to be mindlessly slaughtered by ghouls. " +
            "Slay all who trespass.\"\n—Runo Stromkirk"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5bfcca87-04f8-480a-bae6-ae87f7afb7e1.jpg?1783940795"
        ruling("2011-01-22", "If a creature with first strike deals damage in the first combat damage step, but then loses first strike before regular combat damage, it won't deal combat damage a second time (unless it also has double strike).")
    }
}
