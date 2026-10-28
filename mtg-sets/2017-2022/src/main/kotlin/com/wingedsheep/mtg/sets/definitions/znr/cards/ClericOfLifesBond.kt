package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cleric of Life's Bond — Zendikar Rising #222
 * {W}{B} · Creature — Vampire Cleric · 2/2
 *
 * Whenever another Cleric you control enters, you gain 1 life.
 * Whenever you gain life for the first time each turn, put a +1/+1 counter on this creature.
 */
val ClericOfLifesBond = card("Cleric of Life's Bond") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Vampire Cleric"
    oracleText = "Whenever another Cleric you control enters, you gain 1 life.\n" +
        "Whenever you gain life for the first time each turn, put a +1/+1 counter on this creature."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Permanent.withSubtype("Cleric").youControl()).enters()
        effect = Effects.GainLife(1)
    }

    triggeredAbility {
        trigger = Triggers.you.gainsLife(true)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "222"
        artist = "Andrey Kuzinskiy"
        flavorText = "\"No gods. Only each other.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/5/953db280-6999-4b91-bc40-c6e7f7dd13ef.jpg?1783929324"

        ruling(
            "2020-09-25",
            "An ability that triggers whenever you gain life \"for the first time each turn\" won't trigger " +
                "if you gain life during a turn before the permanent with that ability is on the battlefield, " +
                "even if you gain life again later in the turn.",
        )
        ruling(
            "2020-09-25",
            "In a Two-Headed Giant game, life gained by your teammate won't cause the ability to trigger, " +
                "even though it caused your team's life total to increase.",
        )
    }
}
