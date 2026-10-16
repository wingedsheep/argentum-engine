package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Downwind Ambusher
 * {3}{B}
 * Creature — Skunk Assassin
 * 4/2
 *
 * Flash
 * When this creature enters, choose one —
 * • Target creature an opponent controls gets -1/-1 until end of turn.
 * • Destroy target creature an opponent controls that was dealt damage this turn.
 */
val DownwindAmbusher = card("Downwind Ambusher") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Skunk Assassin"
    oracleText = "Flash\nWhen this creature enters, choose one —\n" +
        "• Target creature an opponent controls gets -1/-1 until end of turn.\n" +
        "• Destroy target creature an opponent controls that was dealt damage this turn."
    power = 4
    toughness = 2

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            // Mode 1: Target creature an opponent controls gets -1/-1 until end of turn
            mode("Target creature an opponent controls gets -1/-1 until end of turn") {
                val creatureOpponentControls = target(TargetFilter.CreatureOpponentControls)
                effect = Effects.ModifyStats(-1, -1, creatureOpponentControls)
            },
            // Mode 2: Destroy target creature an opponent controls that was dealt damage this turn
            mode("Destroy target creature an opponent controls that was dealt damage this turn") {
                val creature = target(
                    TargetFilter(
                        GameObjectFilter.Creature.opponentControls().copy(
                            statePredicates = listOf(StatePredicate.WasDealtDamageThisTurn())
                        )
                    ),
                )
                effect = Effects.Destroy(creature)
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55cfd628-933a-4d3d-b2e5-70bc86960d1c.jpg?1721426398"
    }
}
