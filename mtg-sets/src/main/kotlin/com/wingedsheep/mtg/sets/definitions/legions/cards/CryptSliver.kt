package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbilityToCreatureGroup
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Crypt Sliver
 * {1}{B}
 * Creature — Sliver
 * 1/1
 * All Slivers have "{T}: Regenerate target Sliver."
 */
val CryptSliver = card("Crypt Sliver") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Sliver"
    power = 1
    toughness = 1
    oracleText = "All Slivers have \"{T}: Regenerate target Sliver.\""

    val sliverFilter = GroupFilter(GameObjectFilter.Creature.withSubtype("Sliver"))

    staticAbility {
        ability = GrantActivatedAbilityToCreatureGroup(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = Costs.Tap,
                effect = RegenerateEffect(EffectTarget.ContextTarget(0)),
                targetRequirement = TargetCreature(
                    filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Sliver"))
                )
            ),
            filter = sliverFilter
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "Edward P. Beard, Jr."
        flavorText = "\"Death couldn't contain the slivers. What made we think we could?\" —Riptide Project researcher"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/507097eb-6b50-47ae-a545-df76b743b2bd.jpg?1562911300"
    }
}
