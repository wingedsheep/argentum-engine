package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scion of the Swarm — Zendikar Rising #121
 * {3}{B}{B} · Creature — Vampire Cleric · 3/3
 *
 * Flying
 * Whenever you gain life, put a +1/+1 counter on this creature.
 */
val ScionOfTheSwarm = card("Scion of the Swarm") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Cleric"
    oracleText = "Flying\n" +
        "Whenever you gain life, put a +1/+1 counter on this creature."
    power = 3
    toughness = 3
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Ekaterina Burmak"
        flavorText = "Vampires are now masters of their own fates, commanders of their own legions."
        imageUri = "https://cards.scryfall.io/normal/front/0/4/043926fe-d25f-40b4-b556-181503434e68.jpg"
    }
}
