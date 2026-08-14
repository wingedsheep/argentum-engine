package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Daily Bugle Reporters
 * {3}{W}
 * Creature — Human Citizen
 * 2/3
 * When this creature enters, choose one —
 * • Puff Piece — Put a +1/+1 counter on each of up to two target creatures.
 * • Investigative Journalism — Return target creature card with mana value 2 or less
 *   from your graveyard to your hand.
 *
 * A modal "choose one" ETB built with [ModalEffect.chooseOne]:
 *  - Puff Piece is an optional `count = 2` [TargetCreature] (0–2 targets, any creature)
 *    fanned out with [ForEachTargetEffect] so each chosen creature receives exactly one
 *    +1/+1 counter (not split between them).
 *  - Investigative Journalism targets a single creature card in your graveyard restricted
 *    to `manaValueAtMost(2)` and [Effects.Move]s it to your hand.
 */
val DailyBugleReporters = card("Daily Bugle Reporters") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Citizen"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, choose one —\n" +
        "• Puff Piece — Put a +1/+1 counter on each of up to two target creatures.\n" +
        "• Investigative Journalism — Return target creature card with mana value 2 or less " +
        "from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ModalEffect.chooseOne(
            Mode.withTarget(
                effect = ForEachTargetEffect(
                    listOf(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)))
                ),
                target = TargetCreature(count = 2, optional = true),
                description = "Puff Piece — Put a +1/+1 counter on each of up to two target creatures."
            ),
            Mode.withTarget(
                effect = Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND),
                target = TargetObject(
                    filter = TargetFilter(
                        GameObjectFilter.Creature.ownedByYou().manaValueAtMost(2),
                        zone = Zone.GRAVEYARD
                    )
                ),
                description = "Investigative Journalism — Return target creature card with mana value 2 or less from your graveyard to your hand."
            )
        )
        description = "When this creature enters, choose one — Puff Piece — Put a +1/+1 counter on each of up to two target creatures. • Investigative Journalism — Return target creature card with mana value 2 or less from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "6"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/530dbeb0-b0cd-473e-a43e-7b23c88650a3.jpg?1783905364"
    }
}
