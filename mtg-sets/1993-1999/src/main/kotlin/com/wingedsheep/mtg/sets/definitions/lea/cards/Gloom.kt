package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.IncreaseActivatedAbilityCost
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Gloom
 * {2}{B}
 * Enchantment
 * White spells cost {3} more to cast.
 * Activated abilities of white enchantments cost {3} more to activate.
 *
 * Both halves are symmetrical taxes. The spell half is Chill's shape ([SpellCostTarget.AnyCaster]
 * over a white filter). The ability half is Suppression Field's [IncreaseActivatedAbilityCost]
 * narrowed to white enchantments and *without* `excludeManaAbilities` — the Oracle text makes no
 * mana-ability exception, so a white enchantment's mana ability is taxed too. The enchantment's
 * colour and type are read from projected state, so an enchantment that stops being white stops
 * being taxed.
 */
val Gloom = card("Gloom") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "White spells cost {3} more to cast.\n" +
        "Activated abilities of white enchantments cost {3} more to activate."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.AnyCaster(GameObjectFilter.Any.withColor(Color.WHITE)),
            modification = CostModification.IncreaseGeneric(3)
        )
    }

    staticAbility {
        ability = IncreaseActivatedAbilityCost(
            filter = GroupFilter(GameObjectFilter.Enchantment.withColor(Color.WHITE)),
            amount = DynamicAmounts.fixed(3),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "110"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8d10bc7-daeb-4c0d-9e4a-8eae8d11699f.jpg?1783948696"
    }
}
