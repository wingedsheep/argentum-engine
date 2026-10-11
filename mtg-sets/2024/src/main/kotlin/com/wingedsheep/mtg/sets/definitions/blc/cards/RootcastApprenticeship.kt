package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rootcast Apprenticeship — Bloomburrow Commander #32
 * {3}{G} · Sorcery
 *
 * Choose three. You may choose the same mode more than once.
 * • Put two +1/+1 counters on target creature.
 * • Create a token that's a copy of target token you control.
 * • Target player creates a 1/1 green Squirrel creature token.
 * • Target opponent sacrifices a nontoken artifact of their choice.
 *
 * A Confluence-style modal: `chooseCount = 3, allowRepeat = true`. Each mode declares its own
 * target, so a repeated mode announces a fresh (or the same) target each time it's chosen.
 */
val RootcastApprenticeship = card("Rootcast Apprenticeship") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose three. You may choose the same mode more than once.\n" +
        "• Put two +1/+1 counters on target creature.\n" +
        "• Create a token that's a copy of target token you control.\n" +
        "• Target player creates a 1/1 green Squirrel creature token.\n" +
        "• Target opponent sacrifices a nontoken artifact of their choice."

    spell {
        modal(chooseCount = 3, allowRepeat = true) {
            mode("Put two +1/+1 counters on target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature)
            }
            mode("Create a token that's a copy of target token you control") {
                val token = target(TargetFilter(GameObjectFilter.Token.youControl()))
                effect = Effects.CreateTokenCopyOfTarget(token)
            }
            mode("Target player creates a 1/1 green Squirrel creature token") {
                val player = target(Targets.Player)
                effect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Squirrel"),
                    controller = player,
                    imageUri = "https://cards.scryfall.io/normal/front/5/a/5a6ec62e-0e9b-4312-bfe8-cc85d76fd9e0.jpg?1783909765"
                )
            }
            mode("Target opponent sacrifices a nontoken artifact of their choice") {
                val opponent = target(Targets.Opponent)
                effect = Effects.Sacrifice(filter = GameObjectFilter.Artifact.nontoken(), target = opponent)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "32"
        artist = "Andreia Ugrai"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00756441-a734-4960-9c69-f03a508a1522.jpg?1783910728"

        ruling("2024-07-26", "You can select a mode only if there's a legal target available for that mode. Ignore the targeting requirements for modes you don't choose. Each time you select that mode, you can choose a different target, or you can choose the same target.")
        ruling("2024-07-26", "No matter which combination of modes you choose, you always follow the instructions in the order they are written.")
        ruling("2024-07-26", "If all targets for the chosen modes become illegal before Rootcast Apprenticeship resolves, the spell will be countered and none of its effects will happen. If at least one target is still legal, the spell will resolve but will have no effect on any illegal targets.")
    }
}
