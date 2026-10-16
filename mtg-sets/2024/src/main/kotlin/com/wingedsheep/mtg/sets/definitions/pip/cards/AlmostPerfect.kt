package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Almost Perfect
 * {4}{G}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has base power and toughness 9/10 and has indestructible.
 *
 * Base P/T is set in layer 7b, so pumps and counters still apply on top; indestructible is
 * granted in layer 6.
 */
val AlmostPerfect = card("Almost Perfect") {
    manaCost = "{4}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has base power and toughness 9/10 and has indestructible."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = SetBasePowerToughnessStatic(9, 10, GroupFilter.attachedCreature())
    }
    staticAbility {
        ability = GrantKeyword(Keyword.INDESTRUCTIBLE, GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Elizabeth Peiró"
        flavorText = "An incomplete suit of power armor is still a suit of power armor."
        imageUri = "https://cards.scryfall.io/normal/front/5/6/564a9bff-969f-49c1-be02-40cb45858f19.jpg?1783912393"
        ruling(
            "2024-03-08",
            "Almost Perfect overwrites all previous effects that set the creature's base power and " +
                "toughness to specific values. Any power- or toughness-setting effects that start to " +
                "apply after the ability resolves will overwrite this effect."
        )
        ruling(
            "2024-03-08",
            "Effects that modify a creature's power and/or toughness, such as the effect of the ability " +
                "granted by Idolized, will apply to the creature no matter when they started to take " +
                "effect. The same is true for any counters that change its power and/or toughness and " +
                "effects that switch its power and toughness."
        )
    }
}
