package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbilityReach
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.IncreaseActivatedAbilityCost
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

private val merfolkYouControl = GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.MERFOLK).youControl())

/**
 * Kopala, Warden of Waves
 * {1}{U}{U}
 * Legendary Creature — Merfolk Wizard
 * 2/2
 * Spells your opponents cast that target a Merfolk you control cost {2} more to cast.
 * Abilities your opponents activate that target a Merfolk you control cost {2} more to activate.
 */
val KopalaWardenOfWaves = card("Kopala, Warden of Waves") {
    manaCost = "{1}{U}{U}"
    typeLine = "Legendary Creature — Merfolk Wizard"
    power = 2
    toughness = 2
    oracleText = "Spells your opponents cast that target a Merfolk you control cost {2} more to cast.\n" +
        "Abilities your opponents activate that target a Merfolk you control cost {2} more to activate."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.OpponentsCastTargeting(merfolkYouControl),
            modification = CostModification.IncreaseGeneric(2),
        )
    }

    staticAbility {
        ability = IncreaseActivatedAbilityCost(
            filter = GroupFilter(GameObjectFilter.Any),
            amount = DynamicAmounts.fixed(2),
            reach = ActivatedAbilityReach.ACTIVATED_BY_OPPONENTS,
            targeting = merfolkYouControl,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Magali Villeneuve"
        flavorText = "\"I know these waters better than you know yourselves.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34561f87-09d9-4f23-8a05-907fe1303e70.jpg?1783935779"
    }
}
