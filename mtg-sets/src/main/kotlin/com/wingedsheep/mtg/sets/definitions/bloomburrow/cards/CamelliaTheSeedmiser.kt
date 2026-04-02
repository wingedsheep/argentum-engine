package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Camellia, the Seedmiser
 * {1}{B}{G}
 * Legendary Creature — Squirrel Warlock
 * 3/3
 *
 * Menace
 * Other Squirrels you control have menace.
 * Whenever you sacrifice one or more Foods, create a 1/1 green Squirrel creature token.
 * {2}, Forage: Put a +1/+1 counter on each other Squirrel you control.
 * (To forage, exile three cards from your graveyard or sacrifice a Food.)
 */
val CamelliaTheSeedmiser = card("Camellia, the Seedmiser") {
    manaCost = "{1}{B}{G}"
    typeLine = "Legendary Creature — Squirrel Warlock"
    power = 3
    toughness = 3
    oracleText = "Menace\nOther Squirrels you control have menace.\nWhenever you sacrifice one or more Foods, create a 1/1 green Squirrel creature token.\n{2}, Forage: Put a +1/+1 counter on each other Squirrel you control. (To forage, exile three cards from your graveyard or sacrifice a Food.)"

    keywords(Keyword.MENACE)

    // Other Squirrels you control have menace
    staticAbility {
        ability = GrantKeywordToCreatureGroup(
            keyword = Keyword.MENACE,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype("Squirrel").youControl(),
                excludeSelf = true
            )
        )
    }

    // Whenever you sacrifice one or more Foods, create a 1/1 green Squirrel creature token
    triggeredAbility {
        trigger = Triggers.YouSacrificeOneOrMore(GameObjectFilter.Artifact.withSubtype("Food"))
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Squirrel"),
            imageUri = "https://cards.scryfall.io/normal/front/5/a/5a6ec62e-0e9b-4312-bfe8-cc85d76fd9e0.jpg?1721425294"
        )
    }

    // {2}, Forage: Put a +1/+1 counter on each other Squirrel you control
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Forage()
        )
        effect = ForEachInGroupEffect(
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype("Squirrel").youControl(),
                excludeSelf = true
            ),
            effect = AddCountersEffect(
                counterType = Counters.PLUS_ONE_PLUS_ONE,
                count = 1,
                target = EffectTarget.Self
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "207"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c16eaec-924c-42f6-9fea-07edd7ed93b9.jpg?1721933903"
        ruling("2024-11-08", "Food is an artifact type. Even though it appears on some creatures, it's never a creature type.")
    }
}
