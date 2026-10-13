package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Nipton Lottery
 * {2}{B}{R}
 * Sorcery
 *
 * Choose a creature at random. You gain control of that creature until end of turn. Untap it. It
 * gains haste until end of turn. Then destroy all other creatures.
 *
 * One pipeline: every creature on the battlefield is gathered, the engine picks one at random
 * (`chooseRandomSplit` — no targeting, so shroud and hexproof don't matter, per the ruling), the
 * winner is stolen, untapped and hasted, and the remainder — "all other creatures" — is destroyed
 * in one simultaneous move. Nothing can act between the pick and the destruction (ruling): it all
 * happens inside the one resolution.
 */
val TheNiptonLottery = card("The Nipton Lottery") {
    manaCost = "{2}{B}{R}"
    typeLine = "Sorcery"
    oracleText = "Choose a creature at random. You gain control of that creature until end of turn. Untap it. " +
        "It gains haste until end of turn. Then destroy all other creatures."

    spell {
        effect = Effects.Pipeline {
            val creatures = gather(CardSource.BattlefieldMatching(filter = GameObjectFilter.Creature))
            val (winner, others) = chooseRandomSplit(1, from = creatures)
            run(
                Effects.ForEachInCollection(
                    winner,
                    Effects.GainControl(EffectTarget.IterationEntity, Duration.EndOfTurn) then
                        Effects.Untap(EffectTarget.IterationEntity) then
                        Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.EndOfTurn)
                )
            )
            destroy(others)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "113"
        artist = "Olivier Bernard"
        flavorText = "Oliver Swanick won the only lottery that matters: a chance to live another day."
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0abd847-1c9d-45cd-a6d1-3a0cda3687cb.jpg?1783912386"
        ruling("2024-03-08", "The creature chosen at random isn’t targeted, so a creature with shroud or an opponent’s creature with hexproof can be chosen this way.")
        ruling("2024-03-08", "Players can’t take actions between the time the creature is randomly chosen and the time at which the rest of the effects occur.")
    }
}
