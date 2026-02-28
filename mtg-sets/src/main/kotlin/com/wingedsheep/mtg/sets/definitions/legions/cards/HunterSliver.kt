package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup
import com.wingedsheep.sdk.scripting.GrantTriggeredAbilityToCreatureGroup
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hunter Sliver
 * {1}{R}
 * Creature — Sliver
 * 1/1
 * All Sliver creatures have provoke. (Whenever a Sliver attacks, its controller may have
 * target creature defending player controls untap and block it if able.)
 */
val HunterSliver = card("Hunter Sliver") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Sliver"
    power = 1
    toughness = 1
    oracleText = "All Sliver creatures have provoke. (Whenever a Sliver attacks, its controller may have target creature defending player controls untap and block it if able.)"

    val sliverFilter = GroupFilter(GameObjectFilter.Creature.withSubtype("Sliver"))

    // Grant provoke keyword to all Slivers (display in layer system)
    staticAbility {
        ability = GrantKeywordToCreatureGroup(
            keyword = Keyword.PROVOKE,
            filter = sliverFilter
        )
    }

    // Grant the provoke triggered ability to all Slivers (functional)
    staticAbility {
        ability = GrantTriggeredAbilityToCreatureGroup(
            ability = TriggeredAbility.create(
                trigger = Triggers.Attacks.event,
                binding = Triggers.Attacks.binding,
                effect = Effects.Provoke(EffectTarget.ContextTarget(0)),
                optional = true,
                targetRequirement = Targets.CreatureOpponentControls
            ),
            filter = sliverFilter
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Kev Walker"
        flavorText = "Once they get the scent, there is no escape."
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca9aea1a-6f50-4f66-9f36-2e214dce41b4.jpg?1562935688"
    }
}
