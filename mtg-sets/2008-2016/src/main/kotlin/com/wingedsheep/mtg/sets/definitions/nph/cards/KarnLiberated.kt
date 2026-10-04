package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Karn Liberated
 * {7}
 * Legendary Planeswalker — Karn
 * Loyalty 6
 * +4: Target player exiles a card from their hand.
 * −3: Exile target permanent.
 * −14: Restart the game, leaving in exile all non-Aura permanent cards exiled with Karn. Then put
 * those cards onto the battlefield under your control.
 *
 * Both exiling abilities link what they exile to Karn, and the −14 reads that pile — so only cards
 * this Karn exiled stay in exile, not cards another Karn did. The pile survives Karn leaving the
 * battlefield, which matters: paying −14 from 14 loyalty puts Karn in the graveyard before the
 * ability resolves. The restart and its "then" are one effect (CR 727.4): the cards are put onto
 * the battlefield once the new game's mulligans are over, just before its first untap step.
 */
val KarnLiberated = card("Karn Liberated") {
    manaCost = "{7}"
    typeLine = "Legendary Planeswalker — Karn"
    startingLoyalty = 6
    oracleText = "+4: Target player exiles a card from their hand.\n" +
        "−3: Exile target permanent.\n" +
        "−14: Restart the game, leaving in exile all non-Aura permanent cards exiled with Karn. " +
        "Then put those cards onto the battlefield under your control."

    loyaltyAbility(+4) {
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, player.asPlayer))
            val chosen = chooseExactly(1, hand, chooser = Chooser.TargetPlayer, prompt = "Choose a card to exile")
            exile(chosen, owner = player.asPlayer, linkToSource = true)
        }
    }

    loyaltyAbility(-3) {
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.ExileLinkedToSource(permanent)
    }

    loyaltyAbility(-14) {
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.FromLinkedExile())
            val kept = filter(exiled, GameObjectFilter.Permanent.notSubtype(Subtype.AURA))
            run(Effects.RestartGame(
                exempt = kept,
                afterRestart = Effects.Pipeline {
                    move(kept, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
                }
            ))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Jason Chan"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9287151-95df-4f5a-b32a-4b0aea825452.jpg?1783941329"
        ruling("2020-08-07", "The player who controlled the ability that restarted the game is the starting player in the new game. The new game starts like a game normally does: Each player shuffles their deck (except the cards left in exile by Karn's ability). Each player's life total becomes 20 (or the starting life total for whatever format you're playing). Players draw a hand of seven cards. Players may take mulligans. Players may take actions based on cards in their opening hands, such as those of Leylines.")
        ruling("2020-08-07", "Karn's first and third abilities are linked. Similarly, Karn's second and third abilities are linked. Only non-Aura permanent cards exiled by either of Karn's first two abilities will remain in exile when the game restarts.")
        ruling("2020-08-07", "After the pregame procedure is complete but before the new game's first turn, Karn's ability finishes resolving and the cards left in exile are put onto the battlefield. If this causes any triggered abilities to trigger, those abilities are put onto the stack at the beginning of the first upkeep step.")
        ruling("2020-08-07", "Permanents put onto the battlefield due to Karn's ability will have been under the starting controller's control continuously since the beginning of that player's first turn. Creatures among them can attack and their activated abilities with {T} in the cost can be activated.")
        ruling("2020-08-07", "No actions taken in the game that was restarted apply to the new game. For example, if you were dealt damage by Stigma Lasher in the original game, the effect that states you can't gain life doesn't carry over to the new game.")
        ruling("2020-08-07", "Players won't have any counters or emblems they had in the original game.")
    }
}
