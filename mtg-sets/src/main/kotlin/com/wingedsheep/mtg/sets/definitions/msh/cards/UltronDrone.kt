package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ultron Drone — Marvel Super Heroes #253 (common)
 * {3} · Artifact Creature — Robot Villain · 2/3
 *
 * Power-up — {6}: Put two +1/+1 counters on this creature and create a 2/2 colorless Robot
 * Villain artifact creature token. (Activate each power-up ability only once. Reduce the cost by
 * its mana cost if it entered this turn.)
 *
 * The only wholly colorless power-up in the cycle, so it's also the only one whose reduction is
 * the ordinary generic-mana kind every other cost reduction in the engine already did: `{6}` −
 * `{3}` = `{3}`. The rest of the cycle is why the reduction had to be made pip-wise.
 *
 * The token matches the printed one exactly — colorless (an empty color set, *not* an absent one),
 * `artifactToken = true` so it is an artifact creature, and both of its printed subtypes.
 */
val UltronDrone = card("Ultron Drone") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Robot Villain"
    oracleText = "Power-up — {6}: Put two +1/+1 counters on this creature and create a 2/2 " +
        "colorless Robot Villain artifact creature token. (Activate each power-up ability only " +
        "once. Reduce the cost by its mana cost if it entered this turn.)"
    power = 2
    toughness = 3

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{6}")
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self),
            Effects.CreateToken(
                power = 2,
                toughness = 2,
                creatureTypes = setOf(Subtype.ROBOT.value, Subtype.VILLAIN.value),
                artifactToken = true,
                imageUri = "https://cards.scryfall.io/normal/front/8/e/8eb1de03-fc45-45bd-bd1f-5b164104426e.jpg?1783902799"
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "253"
        artist = "Rafater"
        flavorText = "Many eyes. Many hands. One mind."
        imageUri = "https://cards.scryfall.io/normal/front/1/4/149a0a3b-c470-414c-a7de-d773b8b4cc82.jpg?1783902888"
    }
}
