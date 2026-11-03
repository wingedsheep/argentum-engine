package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Excalbian Gamemaster
 * {5}{W}
 * Creature — Excalbian
 * 6/6
 *
 * Strange new worlds — When this creature enters, create X 1/1 red Officer creature tokens, where X
 * is the number of differently named lands you control.
 * Whenever this creature or another creature you control dies, put a +1/+1 counter on target
 * creature you control.
 *
 * "Strange new worlds" is an ability word with no rules meaning.
 */
val ExcalbianGamemaster = card("Excalbian Gamemaster") {
    manaCost = "{5}{W}"
    typeLine = "Creature — Excalbian"
    power = 6
    toughness = 6
    oracleText = "Strange new worlds — When this creature enters, create X 1/1 red Officer creature tokens, " +
        "where X is the number of differently named lands you control.\n" +
        "Whenever this creature or another creature you control dies, put a +1/+1 counter on target " +
        "creature you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = DynamicAmounts.differentlyNamedLandsYouControl(),
            power = 1, toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Officer"),
            imageUri = "https://cards.scryfall.io/normal/front/d/6/d676cbef-8e31-4ae6-8dfe-6b84160f71e0.jpg?1793363893"
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "16"
        artist = "Charles Chaisson"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31b5780c-8c5f-4836-8848-1eea1d416a93.jpg?1792753846"
    }
}
