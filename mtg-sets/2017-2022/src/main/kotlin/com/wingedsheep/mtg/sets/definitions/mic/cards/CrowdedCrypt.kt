package com.wingedsheep.mtg.sets.definitions.mic.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crowded Crypt — Innistrad: Midnight Hunt Commander #17 (canonical printing)
 * {2}{B} · Artifact
 *
 * {T}: Add {B}.
 * Whenever a creature you control dies, put a corpse counter on this artifact.
 * {4}{B}{B}, {T}, Sacrifice this artifact: Create a 2/2 black Zombie creature token with decayed for
 * each corpse counter on this artifact.
 *
 * The sacrifice is a cost, so the counters are gone by resolution: the token count is the pre-cost
 * corpse count, read as last-known information via `DynamicAmounts.lastKnownSourceCounters` (the
 * Dragonspark Reactor shape).
 */
val CrowdedCrypt = card("Crowded Crypt") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Artifact"
    oracleText = "{T}: Add {B}.\n" +
        "Whenever a creature you control dies, put a corpse counter on this artifact.\n" +
        "{4}{B}{B}, {T}, Sacrifice this artifact: Create a 2/2 black Zombie creature token with decayed " +
        "for each corpse counter on this artifact. (A creature with decayed can't block. When it " +
        "attacks, sacrifice it at end of combat.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.AddCounters(CounterType.CORPSE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{B}{B}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.CreateToken(
            count = DynamicAmounts.lastKnownSourceCounters(CounterType.CORPSE),
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            keywords = setOf(Keyword.DECAYED),
            imageUri = "https://cards.scryfall.io/normal/front/6/a/6adb8607-1066-451d-a719-74ad32358278.jpg?1783925226",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "17"
        artist = "Jarel Threat"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d6114bd-d5d3-441a-80a5-67eb7a355506.jpg?1783925378"
    }
}
