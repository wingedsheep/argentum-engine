package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Paragon of Gathering Mists
 * {3}{U}
 * Creature — Human Wizard
 * 2/2
 * Other blue creatures you control get +1/+1.
 * {U}, {T}: Another target blue creature you control gains flying until end of turn.
 */
val ParagonOfGatheringMists = card("Paragon of Gathering Mists") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "Other blue creatures you control get +1/+1.\n" +
        "{U}, {T}: Another target blue creature you control gains flying until end of turn."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withColor(Color.BLUE).youControl(),
                excludeSelf = true
            )
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.Tap)
        val creature = target(TargetFilter.Creature.withColor(Color.BLUE).youControl().other())
        effect = Effects.GrantKeyword(Keyword.FLYING, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Michael C. Hayes"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5afc32ed-c1a9-4c37-bfe0-562818c046fc.jpg"
    }
}
