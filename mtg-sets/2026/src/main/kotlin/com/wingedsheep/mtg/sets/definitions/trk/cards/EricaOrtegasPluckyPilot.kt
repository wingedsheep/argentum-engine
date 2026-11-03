package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Erica Ortegas, Plucky Pilot
 * {3}{R}
 * Legendary Creature — Human Pilot
 * 3/3
 * Haste
 * Whenever Erica Ortegas becomes tapped, you may put a land card from your hand onto the
 * battlefield. If you do, return a land you control to its owner's hand at the beginning of the
 * next end step.
 *
 * The land returned is chosen when the delayed trigger resolves and can be any land you control,
 * not necessarily the one put onto the battlefield. The delayed trigger never expires on a turn
 * boundary, so a tap during an end step (after it began) waits for the following end step.
 */
val EricaOrtegasPluckyPilot = card("Erica Ortegas, Plucky Pilot") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Pilot"
    oracleText = "Haste\n" +
        "Whenever Erica Ortegas becomes tapped, you may put a land card from your hand onto the " +
        "battlefield. If you do, return a land you control to its owner's hand at the beginning " +
        "of the next end step."
    power = 3
    toughness = 3

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        effect = Effects.Pipeline {
            val landsInHand = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Land))
            val putting = chooseUpTo(
                1,
                from = landsInHand,
                prompt = "You may put a land card from your hand onto the battlefield"
            )
            move(putting, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            ifNotEmpty(putting) {
                run(
                    Effects.CreateDelayedTrigger(
                        step = Step.END,
                        expiry = DelayedTriggerExpiry.Never,
                        fireOnce = true,
                        effect = Effects.Pipeline {
                            val lands = gather(
                                CardSource.BattlefieldMatching(filter = GameObjectFilter.Land, player = Player.You)
                            )
                            val returned = chooseExactly(
                                1,
                                from = lands,
                                prompt = "Return a land you control to its owner's hand",
                                useTargetingUI = true
                            )
                            toHand(returned)
                        }
                    )
                )
            }
        }
        description = "Whenever Erica Ortegas becomes tapped, you may put a land card from your " +
            "hand onto the battlefield. If you do, return a land you control to its owner's hand " +
            "at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "Romana Kendelic"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/3667eb41-598e-4543-97ed-6da961f8b567.jpg?1793013175"
    }
}
