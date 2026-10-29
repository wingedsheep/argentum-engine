package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Celestial Enforcer
 * {2}{W}
 * Creature — Human Cleric
 * 2/3
 *
 * {1}{W}, {T}: Tap target creature. Activate only if you control a creature with flying.
 */
val CelestialEnforcer = card("Celestial Enforcer") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 3
    oracleText = "{1}{W}, {T}: Tap target creature. Activate only if you control a creature with flying."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouControl(GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
            )
        )
        description = "Tap target creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Bastien L. Deharme"
        flavorText = "\"I look to the heavens for hope, but you will kneel.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46666fba-d4a7-4687-8747-a42e4c6d853e.jpg?1783930745"
        ruling(
            "2020-06-23",
            "Once you begin activating Celestial Enforcer's ability, players can't take any other actions " +
                "until you're done activating it. Notably, they can't try to remove your creatures with flying " +
                "to make it illegal to activate the ability."
        )
    }
}
