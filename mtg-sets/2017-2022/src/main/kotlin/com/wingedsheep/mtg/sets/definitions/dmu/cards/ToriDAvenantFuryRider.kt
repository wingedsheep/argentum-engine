package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Tori D'Avenant, Fury Rider
 * {1}{R}{R}{W}
 * Legendary Creature — Human Knight
 * 3/3
 * Vigilance, trample
 * Whenever Tori D'Avenant attacks, all other attacking creatures you control get +1/+1 until end of
 * turn. Other red attacking creatures you control gain trample until end of turn. Untap each other
 * white attacking creature you control.
 *
 * Three group passes over "other attacking creatures you control", each excluding Tori herself. The
 * colour filters are matched against projected state, and none of the earlier passes changes colour
 * or attacking status, so gathering the group three times sees the same creatures. A creature that is
 * both red and white gets all three (per the 2022-09-09 ruling).
 */
val ToriDAvenantFuryRider = card("Tori D'Avenant, Fury Rider") {
    manaCost = "{1}{R}{R}{W}"
    typeLine = "Legendary Creature — Human Knight"
    power = 3
    toughness = 3
    oracleText = "Vigilance, trample\n" +
        "Whenever Tori D'Avenant attacks, all other attacking creatures you control get +1/+1 until end of turn. " +
        "Other red attacking creatures you control gain trample until end of turn. " +
        "Untap each other white attacking creature you control."

    keywords(Keyword.VIGILANCE, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Group.modifyStatsForAll(
            power = 1,
            toughness = 1,
            filter = GroupFilter(GameObjectFilter.Creature.attacking().youControl(), excludeSelf = true)
        ) then Patterns.Group.grantKeywordToAll(
            keyword = Keyword.TRAMPLE,
            filter = GroupFilter(
                GameObjectFilter.Creature.withColor(Color.RED).attacking().youControl(),
                excludeSelf = true
            )
        ) then Patterns.Group.untapGroup(
            filter = GroupFilter(
                GameObjectFilter.Creature.withColor(Color.WHITE).attacking().youControl(),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "223"
        artist = "Anna Podedworna"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c873c5b-61f6-49b9-8993-2d5e9823de62.jpg?1783921274"
        ruling(
            "2022-09-09",
            "If another attacking creature is both red and white, it will get +1/+1 until end of turn, gain trample until end of turn, and be untapped."
        )
    }
}
