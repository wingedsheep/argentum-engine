package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Break Through the Line
 * {1}{R}
 * Enchantment
 * {R}: Target creature with power 2 or less gains haste until end of turn and can't be blocked
 * this turn.
 *
 * The power restriction lives on the target, so a creature pumped above 2 before resolution is an
 * illegal target and the ability doesn't resolve, while a pump after resolution doesn't undo the
 * haste or the evasion (ruling).
 */
val BreakThroughTheLine = card("Break Through the Line") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "{R}: Target creature with power 2 or less gains haste until end of turn and " +
        "can't be blocked this turn."

    activatedAbility {
        cost = Costs.Mana("{R}")
        val creature = target(TargetFilter.Creature.powerAtMost(2))
        effect = Effects.GrantKeyword(Keyword.HASTE, creature) then
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature)
        description = "{R}: Target creature with power 2 or less gains haste until end of turn and " +
            "can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Clint Cearley"
        flavorText = "\"If the Mardu were more in tune with their beasts, they would not scare so easily.\"\n" +
            "—Jilaya, Temur whisperer"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81c1cdbe-5ddb-4cc6-975b-42f3f1ee4749.jpg?1783938690"
        ruling(
            "2014-11-24",
            "If the creature's power is greater than 2 as the activated ability tries to resolve, the " +
                "ability won't resolve and none of its effects will happen. However, if the creature's " +
                "power is raised above 2 after the ability resolves, it will still have haste and can't " +
                "be blocked that turn.",
        )
    }
}
