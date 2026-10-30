package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantStaticAbility
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Callaphe, Beloved of the Sea
 * {1}{U}{U}
 * Legendary Enchantment Creature — Demigod
 * * /3
 * Callaphe's power is equal to your devotion to blue.
 * Creatures and enchantments you control have "Spells your opponents cast that target this
 * permanent cost {1} more to cast."
 *
 * Power is a `dynamicPower` CDA over [DynamicAmounts.devotionTo]. The tax is Jubilant Skybonder's
 * granted [ModifySpellCost]: each creature or enchantment holds one copy (a creature enchantment
 * matches the filter once), so a spell targeting two of them costs {2} more.
 */
val CallapheBelovedOfTheSea = card("Callaphe, Beloved of the Sea") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Enchantment Creature — Demigod"
    power = 0
    toughness = 3
    oracleText = "Callaphe's power is equal to your devotion to blue. (Each {U} in the mana costs of " +
        "permanents you control counts toward your devotion to blue.)\n" +
        "Creatures and enchantments you control have \"Spells your opponents cast that target this " +
        "permanent cost {1} more to cast.\""

    dynamicPower(DynamicAmounts.devotionTo(Color.BLUE))

    staticAbility {
        ability = GrantStaticAbility(
            ability = ModifySpellCost(
                target = SpellCostTarget.OpponentsCastTargeting(GroupFilter.source()),
                modification = CostModification.IncreaseGeneric(1),
            ),
            filter = GroupFilter(GameObjectFilter.CreatureOrEnchantment.youControl()),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5d433f2-0a3b-4f45-83a3-ed600a6d10b1.jpg?1783931586"
        ruling("2020-01-24", "Callaphe's last ability applies to itself as well as other creatures and enchantments you control.")
        ruling("2020-01-24", "Callaphe's last ability gives only one instance of the ability to creatures you control that are also enchantments.")
        ruling("2020-01-24", "An opponent's spell that targets a single creature or enchantment you control more than once costs only {1} more to cast. An opponent's spell that targets multiple creatures and/or enchantments you control costs {1} more for each of them.")
    }
}
