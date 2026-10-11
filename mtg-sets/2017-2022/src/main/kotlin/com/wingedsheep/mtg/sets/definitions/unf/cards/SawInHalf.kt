package com.wingedsheep.mtg.sets.definitions.unf.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.divRoundedUp
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType

/**
 * Saw in Half — Unfinity #88
 * {2}{B} · Instant
 *
 * Destroy target creature. If that creature dies this way, its controller creates two tokens
 * that are copies of that creature, except their power is half that creature's power and their
 * toughness is half that creature's toughness. Round up each time.
 *
 * The halves are snapshotted *before* the destroy — the creature's power and toughness as it last
 * existed on the battlefield (per the rulings), counters and pumps included. "Dies this way" is
 * the destroyed creature landing in a graveyard: indestructible, regeneration or an exile
 * replacement leave nothing for [forEachCaptured] to iterate, so no tokens. The copy's controller
 * is the creature's pre-destroy controller, and the copy reads its last-known copiable values, so
 * a dead Clone yields copies of what it was copying.
 */
val SawInHalf = card("Saw in Half") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature. If that creature dies this way, its controller creates two tokens " +
        "that are copies of that creature, except their power is half that creature's power and their " +
        "toughness is half that creature's toughness. Round up each time."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Pipeline {
            val halfPower = storeNumber(DynamicAmounts.powerOf(creature) divRoundedUp 2)
            val halfToughness = storeNumber(DynamicAmounts.toughnessOf(creature) divRoundedUp 2)
            val target = gather(CardSource.ChosenTargets)
            val controllers = captureControllers(target)
            val destroyed = moveTracked(target, CardDestination.ToZone(Zone.GRAVEYARD), moveType = MoveType.Destroy)
            val died = filter(destroyed, GameObjectFilter.Any.currentlyIn(Zone.GRAVEYARD))
            forEachCaptured(died, original = target, controllers = controllers) {
                run(
                    Effects.CreateTokenCopyOfTarget(
                        target = creature,
                        count = 2,
                        basePower = halfPower.amount,
                        baseToughness = halfToughness.amount,
                    )
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Sebastian Giacobino"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05e6a7bc-a35a-4e68-99a0-be264553b5de.jpg?1783920577"
    }
}
