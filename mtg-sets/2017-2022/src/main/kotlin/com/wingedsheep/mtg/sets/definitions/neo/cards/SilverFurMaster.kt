package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbilityReach
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Silver-Fur Master — Kamigawa: Neon Dynasty #236 (canonical printing)
 * {U}{B} · Creature — Rat Ninja · 2/2
 *
 * Ninjutsu {U}{B}
 * Ninjutsu abilities you activate cost {1} less to activate.
 * Other Ninja and Rogue creatures you control get +1/+1.
 *
 * The discount is [ReduceActivatedAbilityCost] over every ability you activate
 * ([ActivatedAbilityReach.ACTIVATED_BY_YOU] — ninjutsu is activated from hand) narrowed to the
 * ninjutsu keyword. It is generic-only (CR 118.7), so its own `{U}{B}` ninjutsu isn't reduced.
 */
val SilverFurMaster = card("Silver-Fur Master") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Rat Ninja"
    power = 2
    toughness = 2
    oracleText = "Ninjutsu {U}{B} ({U}{B}, Return an unblocked attacker you control to hand: Put " +
        "this card onto the battlefield from your hand tapped and attacking.)\n" +
        "Ninjutsu abilities you activate cost {1} less to activate.\n" +
        "Other Ninja and Rogue creatures you control get +1/+1."

    ninjutsu("{U}{B}")

    staticAbility {
        ability = ReduceActivatedAbilityCost(
            filter = GroupFilter(GameObjectFilter.Any),
            amount = DynamicAmounts.fixed(1),
            reach = ActivatedAbilityReach.ACTIVATED_BY_YOU,
            keyword = Keyword.NINJUTSU,
        )
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withAnySubtype("Ninja", "Rogue").youControl(),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/cacc36d9-4c4b-43b2-a8b4-d265deb1e6b2.jpg?1783923831"
    }
}
