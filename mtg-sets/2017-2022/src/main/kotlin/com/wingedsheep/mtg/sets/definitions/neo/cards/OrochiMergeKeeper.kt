package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Orochi Merge-Keeper
 * {1}{G}
 * Creature — Snake Druid
 * 1/1
 * {T}: Add {G}.
 * As long as this creature is modified, it has "{T}: Add {G}{G}." (Equipment, Auras you control,
 * and counters are modifications.)
 *
 * The {G}{G} ability is a *granted* mana ability, scoped to the creature itself while it is
 * modified: a battlefield-scope [GrantActivatedAbility] whose filter is "this permanent"
 * ([StatePredicate.IsSource]) and [StatePredicate.IsModified] (CR 700.9). Using the battlefield
 * scope rather than a `ConditionalStaticAbility` around a self-scoped grant keeps the ability
 * visible to the mana solver, so auto-pay can tap the Merge-Keeper for {G}{G}.
 */
val OrochiMergeKeeper = card("Orochi Merge-Keeper") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Druid"
    power = 1
    toughness = 1
    oracleText = "{T}: Add {G}.\n" +
        "As long as this creature is modified, it has \"{T}: Add {G}{G}.\" " +
        "(Equipment, Auras you control, and counters are modifications.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {G}."
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddMana(Color.GREEN, 2),
                isManaAbility = true,
                timing = TimingRule.ManaAbility,
                descriptionOverride = "{T}: Add {G}{G}."
            ),
            filter = GroupFilter(
                GameObjectFilter.Permanent.sourceItself().withStatePredicate(StatePredicate.IsModified)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "203"
        artist = "Brent Hollowell"
        flavorText = "One day the spirit and mortal realms will be one. Until then, she faithfully guards the sites where they merge."
        imageUri = "https://cards.scryfall.io/normal/front/7/3/736c3278-6ee2-47f9-aab0-1457e90137b1.jpg?1783923843"
    }
}
