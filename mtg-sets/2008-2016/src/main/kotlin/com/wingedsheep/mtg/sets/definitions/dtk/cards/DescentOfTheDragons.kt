package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Descent of the Dragons
 * {4}{R}{R}
 * Sorcery
 *
 * Destroy any number of target creatures. For each creature destroyed this way, its controller
 * creates a 4/4 red Dragon creature token with flying.
 *
 * Pipeline (Builder's Bane shape):
 *   1. `gather(ChosenTargets)` — the targets still legal on resolution.
 *   2. `captureControllers` — snapshot each target's controller before it leaves the battlefield
 *      ("its controller" is the creature's last controller on the battlefield).
 *   3. `moveTracked(..., MoveType.Destroy)` — regenerated / indestructible targets drop out of the
 *      tracked collection, so they don't count (2015-02-25 ruling).
 *   4. `forEachCaptured` — once per player who lost creatures this way, that player creates one
 *      Dragon per creature of theirs destroyed.
 */
val DescentOfTheDragons = card("Descent of the Dragons") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy any number of target creatures. For each creature destroyed this way, " +
        "its controller creates a 4/4 red Dragon creature token with flying."

    spell {
        targets(TargetFilter.Creature, unlimited = true)
        effect = Effects.Pipeline {
            val targets = gather(CardSource.ChosenTargets)
            val controllers = captureControllers(targets)
            val destroyed = moveTracked(targets, CardDestination.ToZone(Zone.GRAVEYARD), moveType = MoveType.Destroy)
            forEachCaptured(destroyed, original = targets, controllers = controllers) { killCount ->
                run(
                    Effects.CreateToken(
                        count = killCount.amount,
                        power = 4,
                        toughness = 4,
                        colors = setOf(Color.RED),
                        creatureTypes = setOf("Dragon"),
                        keywords = setOf(Keyword.FLYING),
                        controller = EffectTarget.Controller,
                        imageUri = "https://cards.scryfall.io/normal/front/1/e/1ee0725f-8562-49c2-83fa-11a702e5c78d.jpg?1783938561"
                    )
                )
            }
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "133"
        artist = "Steve Prescott"
        flavorText = "Dragons emerge from tempests fully formed and terribly hungry."
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5ab919f3-5797-406b-ae1f-7a7c27739b2d.jpg?1783938591"
        ruling("2015-02-25", "If a creature is targeted but not destroyed (for example, if it regenerates or has indestructible), it won't count toward the number of Dragon tokens put onto the battlefield.")
    }
}
