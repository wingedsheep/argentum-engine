package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hobgoblin Bandit Lord
 * {1}{R}{R}
 * Creature — Goblin Rogue
 * 2/3
 *
 * Other Goblins you control get +1/+1.
 * {R}, {T}: This creature deals damage equal to the number of Goblins that entered the
 * battlefield under your control this turn to any target.
 *
 * - The anthem pumps every other Goblin *permanent* you control ("Goblins", not "Goblin
 *   creatures"), as on Goblin Trashmaster.
 * - The damage reads the per-player entry log on resolution
 *   ([DynamicAmounts.subtypeEnteredUnderControlThisTurn]), so Goblins that have since left the
 *   battlefield still count, as do Goblins that entered before this creature did (2021-07-23 ruling).
 */
val HobgoblinBanditLord = card("Hobgoblin Bandit Lord") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Rogue"
    power = 2
    toughness = 3
    oracleText = "Other Goblins you control get +1/+1.\n" +
        "{R}, {T}: This creature deals damage equal to the number of Goblins that entered the " +
        "battlefield under your control this turn to any target."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Permanent.withSubtype(Subtype.GOBLIN).youControl(),
                excludeSelf = true
            )
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.Tap)
        val any = target(Targets.Any)
        effect = Effects.DealDamage(
            DynamicAmounts.subtypeEnteredUnderControlThisTurn(Subtype.GOBLIN),
            any,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "147"
        artist = "Mark Zug"
        flavorText = "In a hobgoblin's view of the world, the weak owe tribute and fealty to the strong."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09e9dc36-f2d8-4384-98cb-e44c00b02433.jpg?1783926478"

        ruling("2021-07-23", "Hobgoblin Bandit Lord's activated ability only counts the number of Goblins that entered the battlefield this turn. It doesn't matter if those Goblins are still on the battlefield as it resolves.")
        ruling("2021-07-23", "If no Goblins have entered the battlefield this turn, no damage will be dealt. Abilities that trigger whenever damage is dealt won't trigger.")
    }
}
