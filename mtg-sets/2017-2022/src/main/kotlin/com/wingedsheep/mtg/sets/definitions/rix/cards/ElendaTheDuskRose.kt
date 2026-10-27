package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elenda, the Dusk Rose
 * {2}{W}{B}
 * Legendary Creature — Vampire Knight
 * 1/1
 *
 * Lifelink
 * Whenever another creature dies, put a +1/+1 counter on Elenda.
 * When Elenda dies, create X 1/1 white Vampire creature tokens with lifelink, where X is Elenda's power.
 *
 * The growth trigger is Swarm of Bloodflies' `another(Creature).dies()`. X is Nested Shambler's
 * `sourcePower()`, which falls back to the battlefield-exit snapshot once Elenda has left, so the
 * count is her power as she last existed on the battlefield — counters and pumps included.
 */
val ElendaTheDuskRose = card("Elenda, the Dusk Rose") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Vampire Knight"
    power = 1
    toughness = 1
    oracleText = "Lifelink\n" +
        "Whenever another creature dies, put a +1/+1 counter on Elenda.\n" +
        "When Elenda dies, create X 1/1 white Vampire creature tokens with lifelink, where X is Elenda's power."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature).dies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            count = DynamicAmounts.sourcePower(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Vampire"),
            keywords = setOf(Keyword.LIFELINK),
            imageUri = "https://cards.scryfall.io/normal/front/0/9/09293ae7-0629-417b-9eda-9bd3f6d8e118.jpg?1783935812"
        )
        description = "When Elenda dies, create X 1/1 white Vampire creature tokens with lifelink, where X is " +
            "Elenda's power."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "157"
        artist = "Chris Rahn"
        flavorText = "Her centuries-long search is ended."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a52af49b-cfeb-47a8-9ce5-5d35bfa9eb75.jpg?1783935277"
        ruling(
            "2018-01-19",
            "If Elenda dies at the same time as another creature, both of its triggered abilities trigger. " +
                "However, the first one won't do anything since you can't put a +1/+1 counter on Elenda."
        )
        ruling(
            "2018-01-19",
            "To determine how many Vampire tokens are created, use Elenda's power as it last existed on the battlefield."
        )
    }
}
