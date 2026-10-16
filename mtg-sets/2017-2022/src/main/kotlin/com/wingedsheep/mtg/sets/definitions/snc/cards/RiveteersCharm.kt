package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Riveteers Charm
 * {B}{R}{G}
 * Instant
 * Choose one —
 * • Target opponent sacrifices a creature or planeswalker they control with the greatest mana value
 *   among creatures and planeswalkers they control.
 * • Exile the top three cards of your library. Until your next end step, you may play those cards.
 * • Exile target player's graveyard.
 *
 * Mode one is Break Under Pressure's pipeline: gather the opponent's creatures and planeswalkers,
 * narrow to those tied for the greatest mana value, and the targeted opponent picks which tied
 * permanent to sacrifice (the ruling). Mode two is [Patterns.Exile.impulse] with an
 * [MayPlayExpiry.UntilNextEndStep] window; mode three is Tormod's Crypt's graveyard exile.
 */
val RiveteersCharm = card("Riveteers Charm") {
    manaCost = "{B}{R}{G}"
    colorIdentity = "BRG"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Target opponent sacrifices a creature or planeswalker they control with the greatest mana value " +
        "among creatures and planeswalkers they control.\n" +
        "• Exile the top three cards of your library. Until your next end step, you may play those cards.\n" +
        "• Exile target player's graveyard."

    spell {
        modal(chooseCount = 1) {
            mode("Target opponent sacrifices a creature or planeswalker with the greatest mana value among those they control") {
                val opponent = target(Targets.Opponent)
                effect = Effects.Pipeline {
                    val candidates = gather(
                        CardSource.ControlledPermanents(
                            player = opponent.asPlayer,
                            filter = GameObjectFilter.CreatureOrPlaneswalker
                        )
                    )
                    val greatest = filter(candidates, CollectionFilter.GreatestManaValue)
                    val sacrificed = chooseExactly(
                        1,
                        from = greatest,
                        chooser = Chooser.TargetPlayer,
                        prompt = "Choose a creature or planeswalker with the greatest mana value to sacrifice",
                        useTargetingUI = true
                    )
                    sacrifice(sacrificed)
                }
            }
            mode("Exile the top three cards of your library. Until your next end step, you may play those cards") {
                effect = Patterns.Exile.impulse(
                    count = 3,
                    expiry = MayPlayExpiry.UntilNextEndStep,
                )
            }
            mode("Exile target player's graveyard") {
                val player = target(Targets.Player)
                effect = Effects.Pipeline {
                    val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, player.asPlayer))
                    exile(graveyard, player.asPlayer)
                }
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Steve Argyle"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/784a5915-bc42-49d6-8a1b-45da7749f03a.jpg?1783923074"
        ruling(
            "2022-04-29",
            "For Riveteers Charm's first mode, if the target opponent controls more than one creature or " +
                "planeswalker with the highest mana value among creatures and planeswalkers they control, that " +
                "player chooses which one to sacrifice as Riveteers Charm resolves."
        )
    }
}
