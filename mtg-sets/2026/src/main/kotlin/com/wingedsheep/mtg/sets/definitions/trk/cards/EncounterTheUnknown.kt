package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Encounter the Unknown
 * {1}{R}{R}
 * Instant
 *
 * Choose one —
 * • Come in Peace — Exile the top two cards of your library. Until the end of your next turn, you may
 *   play those cards.
 * • Shoot to Kill — Encounter the Unknown deals 2 damage to each creature and each player.
 */
val EncounterTheUnknown = card("Encounter the Unknown") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Come in Peace — Exile the top two cards of your library. Until the end of your next turn, " +
        "you may play those cards.\n" +
        "• Shoot to Kill — Encounter the Unknown deals 2 damage to each creature and each player."

    spell {
        modal(chooseCount = 1) {
            mode("Come in Peace — Exile the top two cards of your library. You may play them until the end of your next turn") {
                effect = Patterns.Exile.impulse(2, MayPlayExpiry.UntilEndOfNextTurn)
            }

            mode("Shoot to Kill — Deal 2 damage to each creature and each player") {
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature),
                    Effects.DealDamage(2, EffectTarget.IterationEntity)
                ) then
                    Effects.ForEachPlayer(
                        Player.Each,
                        listOf(Effects.DealDamage(2, EffectTarget.Controller))
                    )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "148"
        artist = "Neil Blevins"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a49f66c-7017-4ddd-8787-21d68fe1bff5.jpg?1793274490"
    }
}
