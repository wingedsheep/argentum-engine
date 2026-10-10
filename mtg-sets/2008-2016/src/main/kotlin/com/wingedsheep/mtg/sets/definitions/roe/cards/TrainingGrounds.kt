package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Training Grounds — Rise of the Eldrazi #91
 * {U}
 * Enchantment
 * Activated abilities of creatures you control cost {2} less to activate. This effect can't reduce
 * the mana in that cost to less than one mana.
 *
 * [ReduceActivatedAbilityCost] over creatures you control on the battlefield with `manaFloor = 1`:
 * only generic mana is reduced, and never below one total mana — `{2}` becomes `{1}`, `{2}{G}`
 * becomes `{G}`, and a cost with no generic mana (`{R}{R}`, `{T}`) is untouched.
 */
val TrainingGrounds = card("Training Grounds") {
    manaCost = "{U}"
    typeLine = "Enchantment"
    oracleText = "Activated abilities of creatures you control cost {2} less to activate. This effect " +
        "can't reduce the mana in that cost to less than one mana."

    staticAbility {
        ability = ReduceActivatedAbilityCost(
            filter = GroupFilter(GameObjectFilter.Creature.youControl()),
            amount = DynamicAmounts.fixed(2),
            manaFloor = 1
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "James Ryman"
        flavorText = "\"Under the master's eye, skills are honed sharper and spells cut deeper.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2cf16f8-6e69-46b3-8453-1d1a2a5670e2.jpg?1783941990"
        ruling("2025-10-02", "Training Grounds affects only creatures you control on the battlefield. The costs of activated abilities that work in other zones (such as cycling or unearth) won't be reduced.")
        ruling("2025-10-02", "If an activated ability of a creature you control costs no generic mana to activate (for example, if it costs {R}{R}, it costs {0}, or it costs only nonmana actions such as {T} or \"Sacrifice a creature\"), Training Grounds simply won't affect it. In particular, it won't increase the cost to include a mana payment of {1}.")
        ruling("2025-10-02", "Training Grounds can reduce the part of an activation cost represented by generic mana symbols down to nothing, as long as it still costs at least one mana. For example, if an activation cost is {2}{G}, you'd have to pay only {G}. If an activation cost is {2}, though, you'd still have to pay {1}.")
    }
}
