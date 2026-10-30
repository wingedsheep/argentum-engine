package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantStaticAbility
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Jubilant Skybonder
 * {1}{W/U}{W/U}
 * Creature — Human Wizard
 * 2/2
 * Flying
 * Creatures you control with flying have "Spells your opponents cast that target this creature
 * cost {2} more to cast."
 */
val JubilantSkybonder = card("Jubilant Skybonder") {
    manaCost = "{1}{W/U}{W/U}"
    colorIdentity = "WU"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "Flying\nCreatures you control with flying have \"Spells your opponents cast that target this creature cost {2} more to cast.\""

    keywords(Keyword.FLYING)

    staticAbility {
        ability = GrantStaticAbility(
            ability = ModifySpellCost(
                target = SpellCostTarget.OpponentsCastTargeting(GroupFilter.source()),
                modification = CostModification.IncreaseGeneric(2),
            ),
            filter = GroupFilter(GameObjectFilter.Creature.youControl().withKeyword(Keyword.FLYING)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "223"
        artist = "Jesper Ejsing"
        flavorText = "She was born unable to walk. Through the *eludha*, she was able to fly."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f8f2d69-fba9-40e5-8ef7-75e14ef4070a.jpg?1783931012"
    }
}
