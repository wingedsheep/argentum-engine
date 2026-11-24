package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Racecourse Fury
 * {R}
 * Enchantment — Aura
 *
 * Enchant land
 * Enchanted land has "{T}: Target creature gains haste until end of turn."
 *
 * The granted ability is Battle Rampart's haste tap, carried by the enchanted land the way
 * Security Blockade grants its shield; the target is bound by id as on Poultice Sliver.
 */
val RacecourseFury = card("Racecourse Fury") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "Enchanted land has \"{T}: Target creature gains haste until end of turn.\""

    auraTarget = TargetObject(filter = TargetFilter.Land)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                cost = Costs.Tap,
                effect = Effects.GrantKeyword(Keyword.HASTE, target = EffectTarget.BoundVariable("target")),
                targetRequirements = listOf(TargetObject(filter = TargetFilter.Creature, id = "target"))
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "Sam Burley"
        flavorText = "Close finishes are settled by who can dismount and get to weapons first."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15d13d35-b5f7-4d3d-a1fa-84178b28acae.jpg?1783940353"
    }
}
