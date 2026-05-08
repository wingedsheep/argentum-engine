package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mistform Sliver
 * {1}{U}
 * Creature — Illusion Sliver
 * 1/1
 * All Slivers have "{1}: This permanent becomes the creature type of your choice in addition to its other types until end of turn."
 */
val MistformSliver = card("Mistform Sliver") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Illusion Sliver"
    power = 1
    toughness = 1
    oracleText = "All Slivers have \"{1}: This permanent becomes the creature type of your choice in addition to its other types until end of turn.\""

    val sliverFilter = GroupFilter(GameObjectFilter.Creature.withSubtype("Sliver"))

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = Costs.Mana("{1}"),
                effect = BecomeCreatureTypeEffect(
                    target = EffectTarget.Self
                )
            ),
            filter = sliverFilter
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Ben Thompson"
        flavorText = "Taking the form of a junior researcher, the first sliver slipped out of Riptide."
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79a53c29-6753-4f6b-b4ee-00c1adf7e9c6.jpg?1562919442"
    }
}
