package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sylvan Advocate — Oath of the Gatewatch #144 (canonical printing)
 * {1}{G} · Creature — Elf Druid Ally · 2/3
 *
 * Vigilance
 * As long as you control six or more lands, this creature and land creatures you control get +2/+2.
 *
 * One "as long as" condition ([Conditions.ControlLandsAtLeast]`(6)`) shared by two Layer 7c
 * [ModifyStats] statics: one on the source, one over land creatures you control (types read from
 * projected state, so animated manlands count). The land-creature group excludes the source, so an
 * Advocate that has somehow become a land creature still gets +2/+2 only once — the ability affects
 * a set of objects, and each object in it gets the bonus a single time.
 */
val SylvanAdvocate = card("Sylvan Advocate") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid Ally"
    power = 2
    toughness = 3
    oracleText = "Vigilance\nAs long as you control six or more lands, this creature and land creatures you control get +2/+2."

    keywords(Keyword.VIGILANCE)

    val sixLands = Conditions.ControlLandsAtLeast(6)

    staticAbility {
        ability = ModifyStats(2, 2, Filters.Self)
        condition = sixLands
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 2,
            toughnessBonus = 2,
            filter = GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Land).youControl(), excludeSelf = true),
        )
        condition = sixLands
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "144"
        artist = "Volkan Baǵa"
        flavorText = "\"The land gives of itself. Its abundance provides, even in times of chaos.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4ad1ff9b-306d-47c3-9334-371e4482370b.jpg?1783937899"
        ruling(
            "2016-01-22",
            "Damage remains marked on creatures until the turn ends. If Sylvan Advocate's ability stops " +
                "applying (because Sylvan Advocate leaves the battlefield or you no longer control six or more " +
                "lands), then any land creatures that needed the toughness bonus to stay alive will be destroyed.",
        )
    }
}
