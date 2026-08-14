package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding

/**
 * Zahur, Glory's Past — Aetherdrift #229
 * {W}{B} · Legendary Creature — Zombie Cat Warrior · 3/2
 *
 * Start your engines!
 * Sacrifice another creature: Surveil 1. Activate only once each turn.
 * Max speed — Whenever a nontoken creature you control dies, create a tapped 2/2 black Zombie
 * creature token.
 *
 * Two details the wording turns on:
 *
 * - **The death trigger is not "another".** Zahur is itself a nontoken creature you control, so its
 *   own death fires the ability — hence [TriggerBinding.ANY] rather than `OTHER`. The engine reads
 *   last-known information off the `ZoneChangeEvent`, which is what makes the self-death case work at
 *   all (the entity is gone by resolution).
 * - **Sacrificing to the activated ability feeds the death trigger.** At max speed, `Sacrifice
 *   another creature: Surveil 1` also produces a Zombie, because a sacrifice is a battlefield →
 *   graveyard move that matches the trigger. That is the card's whole engine, so the trigger is
 *   deliberately *not* built with `excludeSacrifice`.
 *
 * The activated ability has no mana cost at all — sacrificing another creature is the entire cost —
 * and [ActivationRestriction.OncePerTurn] carries "Activate only once each turn" (reset each turn,
 * unlike the per-object `Once` used by exhaust).
 */
val ZahurGlorysPast = card("Zahur, Glory's Past") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Zombie Cat Warrior"
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "Sacrifice another creature: Surveil 1. Activate only once each turn.\n" +
        "Max speed — Whenever a nontoken creature you control dies, create a tapped 2/2 black " +
        "Zombie creature token."
    power = 3
    toughness = 2

    startYourEngines()

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Creature)
        effect = Patterns.Library.surveil(1)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        description = "Surveil 1. Activate only once each turn."
    }

    maxSpeed {
        triggeredAbility {
            trigger = Triggers.leavesBattlefield(
                filter = GameObjectFilter.Creature.youControl().nontoken(),
                to = Zone.GRAVEYARD,
                binding = TriggerBinding.ANY,
            )
            effect = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Zombie"),
                tapped = true,
                imageUri = "https://cards.scryfall.io/normal/front/b/8/b82be730-c63b-4c2b-99f4-476befdb95cb.jpg?1783907681",
            )
            description = "Whenever a nontoken creature you control dies, create a tapped 2/2 " +
                "black Zombie creature token."
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "229"
        artist = "Leroy Steinmann"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31944ea5-045d-481d-9aff-3c7ed663813a.jpg?1783907851"
    }
}
