package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sudden Disappearance
 * {5}{W}
 * Sorcery
 * Exile all nonland permanents target player controls. Return the exiled cards to the battlefield
 * under their owner's control at the beginning of the next end step.
 *
 * Same mass-flicker shape as Planar Guide: the delayed trigger carries the exiled pile itself.
 * Tokens cease to exist in exile, so only cards come back.
 */
val SuddenDisappearance = card("Sudden Disappearance") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText =
        "Exile all nonland permanents target player controls. Return the exiled cards to the " +
            "battlefield under their owner's control at the beginning of the next end step."

    spell {
        target(Targets.Player)
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.BattlefieldMatching(GameObjectFilter.NonlandPermanent, Player.TargetPlayer))
            exile(exiled)
            run(Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.Pipeline {
                    move(exiled, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
                },
                carryCollections = listOf(exiled.key)
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "Cliff Childs"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a51b792c-b987-49b6-9cc6-80d613c7d065.jpg?1783940849"
        ruling(
            "2011-01-22",
            "If Sudden Disappearance's delayed triggered ability is countered or otherwise removed from the " +
                "stack after triggering, the exiled cards will remain in exile indefinitely. The delayed " +
                "triggered ability will only trigger once."
        )
    }
}
