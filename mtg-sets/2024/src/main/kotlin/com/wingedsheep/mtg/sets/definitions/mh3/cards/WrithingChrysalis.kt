package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Writhing Chrysalis {2}{R}{G} — Modern Horizons 3 #208 (common)
 * Creature — Eldrazi Drone 2/3
 * Devoid
 * When you cast this spell, create two 0/1 colorless Eldrazi Spawn creature tokens with
 * "Sacrifice this token: Add {C}."
 * Reach
 * Whenever you sacrifice another Eldrazi, put a +1/+1 counter on this creature.
 *
 * The sacrifice trigger is the per-permanent, OTHER-binding `Triggers.you.sacrificesAnother`, so
 * sacrificing two Eldrazi at once grows it twice, and sacrificing the Chrysalis itself does not fire it.
 */
val WrithingChrysalis = card("Writhing Chrysalis") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "When you cast this spell, create two 0/1 colorless Eldrazi Spawn creature tokens with " +
        "\"Sacrifice this token: Add {C}.\"\n" +
        "Reach\n" +
        "Whenever you sacrifice another Eldrazi, put a +1/+1 counter on this creature."

    keywords(Keyword.DEVOID, Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.CreateEldraziSpawn(2)
    }

    triggeredAbility {
        trigger = Triggers.you.sacrificesAnother(GameObjectFilter.Permanent.withSubtype("Eldrazi"))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "208"
        artist = "Domenico Cava"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f54dbeb1-51f8-40e2-912a-ec25457de5a2.jpg?1783911244"
        ruling("2024-06-07", "Writhing Chrysalis's second ability will resolve before Writhing Chrysalis does. If Writhing Chrysalis is countered or otherwise leaves the stack in response to that triggered ability, the triggered ability will still resolve as normal.")
    }
}
