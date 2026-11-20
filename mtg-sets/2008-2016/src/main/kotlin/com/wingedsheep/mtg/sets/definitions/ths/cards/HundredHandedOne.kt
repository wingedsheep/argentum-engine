package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanBlockAdditionalForCreatureGroup
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hundred-Handed One — Theros #20
 * {2}{W}{W} · Creature — Giant · 3/5
 *
 * Vigilance
 * {3}{W}{W}{W}: Monstrosity 3.
 * As long as this creature is monstrous, it has reach and can block an additional ninety-nine
 * creatures each combat.
 *
 * The two halves of the monstrous clause are separate self-scoped statics behind the same
 * [Conditions.SourceIsMonstrous] gate: the reach grant (layer 6) and the extra-blocks allowance.
 */
val HundredHandedOne = card("Hundred-Handed One") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Giant"
    power = 3
    toughness = 5
    oracleText = "Vigilance\n" +
        "{3}{W}{W}{W}: Monstrosity 3. (If this creature isn't monstrous, put three +1/+1 counters on it and it becomes monstrous.)\n" +
        "As long as this creature is monstrous, it has reach and can block an additional ninety-nine creatures each combat."

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Mana("{3}{W}{W}{W}")
        effect = Effects.Monstrosity(3)
    }

    staticAbility {
        ability = GrantKeyword(keyword = Keyword.REACH.name, filter = GroupFilter.source())
        condition = Conditions.SourceIsMonstrous
    }

    staticAbility {
        ability = CanBlockAdditionalForCreatureGroup(count = 99, filter = GroupFilter.source())
        condition = Conditions.SourceIsMonstrous
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Brad Rigney"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/817b8dae-6248-41a4-80a7-7e8102479de4.jpg?1783939811"
        ruling("2013-09-15", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2013-09-15", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
        ruling("2013-09-15", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
