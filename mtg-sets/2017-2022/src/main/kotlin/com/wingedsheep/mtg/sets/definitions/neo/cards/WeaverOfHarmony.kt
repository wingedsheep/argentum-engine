package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Weaver of Harmony
 * {1}{G}
 * Enchantment Creature — Snake Druid
 * 2/2
 *
 * Other enchantment creatures you control get +1/+1.
 * {G}, {T}: Copy target activated or triggered ability you control from an enchantment source.
 * You may choose new targets for the copy. (Mana abilities can't be targeted.)
 *
 * The copy ability is Echo, Perceptive Prodigy's shape with an enchantment source instead of a
 * creature one: the "activated or triggered ability you control" stack filter narrowed by
 * `abilitySourceMatches(Enchantment)`, which reads the ability's source wherever it is — so a
 * channel ability of an enchantment card activated from hand qualifies (the 2022-02-18 ruling).
 * [Effects.CopyTargetSpellOrAbility] reprompts for new targets per CR 707.10c.
 */
val WeaverOfHarmony = card("Weaver of Harmony") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Snake Druid"
    power = 2
    toughness = 2
    oracleText = "Other enchantment creatures you control get +1/+1.\n" +
        "{G}, {T}: Copy target activated or triggered ability you control from an enchantment " +
        "source. You may choose new targets for the copy. (Mana abilities can't be targeted.)"

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.youControl() and GameObjectFilter.Enchantment,
                excludeSelf = true
            )
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap)
        val ability = target(
            TargetFilter.ActivatedOrTriggeredAbilityOnStack.youControl()
                .abilitySourceMatches(GameObjectFilter.Enchantment),
        )
        effect = Effects.CopyTargetSpellOrAbility(ability)
        holdPriority = true
        description = "{G}, {T}: Copy target activated or triggered ability you control from an " +
            "enchantment source. You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "213"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1682673-e947-44e2-b9c9-cbcb9a16892b.jpg?1783923838"
        ruling(
            "2022-02-18",
            "Activated and triggered abilities from enchantment sources include abilities of " +
                "enchantment cards that can be activated or triggered from other zones, such as " +
                "channel abilities."
        )
    }
}
