package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Debt to the Kami
 * {2}{B}
 * Instant
 *
 * Choose one —
 * • Target opponent exiles a creature they control.
 * • Target opponent exiles an enchantment they control.
 *
 * Both modes follow Early Winter's second mode: the opponent is the target and picks
 * which of their own permanents to exile, so hexproof on the permanent is irrelevant
 * and the mode is legal even if they control none.
 */
val DebtToTheKami = card("Debt to the Kami") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Target opponent exiles a creature they control.\n" +
        "• Target opponent exiles an enchantment they control."

    spell {
        modal(chooseCount = 1) {
            mode("Target opponent exiles a creature they control") {
                val opponent = target(Targets.Opponent)
                effect = Effects.Pipeline {
                    val creatures = gather(
                        CardSource.FromZone(Zone.BATTLEFIELD, opponent.asPlayer, GameObjectFilter.Creature)
                    )
                    val chosen = chooseExactly(
                        1, from = creatures,
                        chooser = Chooser.TargetPlayer,
                        prompt = "Choose a creature to exile"
                    )
                    exile(chosen, owner = opponent.asPlayer)
                }
            }
            mode("Target opponent exiles an enchantment they control") {
                val opponent = target(Targets.Opponent)
                effect = Effects.Pipeline {
                    val enchantments = gather(
                        CardSource.FromZone(Zone.BATTLEFIELD, opponent.asPlayer, GameObjectFilter.Enchantment)
                    )
                    val chosen = chooseExactly(
                        1, from = enchantments,
                        chooser = Chooser.TargetPlayer,
                        prompt = "Choose an enchantment to exile"
                    )
                    exile(chosen, owner = opponent.asPlayer)
                }
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "92"
        artist = "Jodie Muir"
        flavorText = "The masters weren't speaking figuratively when they said guilt could be all-consuming."
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1a7e728-ba01-4e3d-b269-9e2dd85a1d1b.jpg?1783923889"
    }
}
