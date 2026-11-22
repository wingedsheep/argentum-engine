package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AnimateLandGroup
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Awaken the Ancient — Magic 2014 #126 (canonical printing)
 * {1}{R}{R}{R} · Enchantment — Aura
 *
 * Enchant Mountain
 * Enchanted Mountain is a 7/7 red Giant creature with haste. It's still a land.
 *
 * The animation is [AnimateLandGroup] over the attached land (red, Giant subtype added, land types
 * kept), with haste granted alongside — the Crackling Emergence shape.
 */
val AwakenTheAncient = card("Awaken the Ancient") {
    manaCost = "{1}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant Mountain\n" +
        "Enchanted Mountain is a 7/7 red Giant creature with haste. It's still a land."
    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN)))

    staticAbility {
        ability = AnimateLandGroup(
            filter = GroupFilter(GameObjectFilter.Land, scope = Scope.AttachedTo),
            power = 7,
            toughness = 7,
            creatureSubtypes = listOf("Giant"),
            colors = setOf(Color.RED)
        )
    }

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter(GameObjectFilter.Land, scope = Scope.AttachedTo))
    }

    metadata {
        rarity = Rarity.RARE
        flavorText = "Some days you stand to greet the horizon. Other days the horizon stands to greet you."
        collectorNumber = "126"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4125304-fd68-4051-96d5-625ffa9b0d3c.jpg?1783939917"
        ruling("2013-07-01", "If the enchanted Mountain stops being a Mountain for any reason, Awaken the Ancient will be put into its owner's graveyard as a state-based action.")
        ruling("2013-07-01", "Copies of the enchanted Mountain won't be 7/7 red Giant creatures with haste. For example, if Awaken the Ancient is enchanting a basic Mountain, a copy of that creature would just be a basic Mountain.")
    }
}
