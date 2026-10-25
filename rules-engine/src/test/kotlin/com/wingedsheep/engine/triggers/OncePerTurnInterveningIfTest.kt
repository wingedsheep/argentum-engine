package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A once-each-turn trigger whose intervening-if is false never triggers (CR 603.4), so it must not
 * take the once-per-turn slot from a simultaneous instance whose condition holds (Tataru Taru:
 * "Whenever an opponent draws a card, if it isn't that player's turn, … This ability triggers
 * only once each turn.").
 *
 * *Off-Turn Ledger* watches every player's draws; *Shared Draw* makes both players draw in one
 * event batch on Player1's turn. Player1's draw fails the condition, Player2's passes — the
 * condition filter must run before the per-turn cap collapses the batch to one.
 */
private val OffTurnLedger = card("Off-Turn Ledger") {
    manaCost = "{2}"
    typeLine = "Enchantment"
    oracleText = "Whenever a player draws a card, if it isn't that player's turn, you gain 1 life. " +
        "This ability triggers only once each turn."

    triggeredAbility {
        trigger = Triggers.anyPlayer.draws()
        interveningIf = Conditions.Not(Conditions.IsPlayersTurn(Player.TriggeringPlayer))
        effect = Effects.GainLife(1)
        oncePerTurn = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "T1"
    }
}

private val SharedDraw = card("Shared Draw") {
    manaCost = "{0}"
    typeLine = "Sorcery"
    oracleText = "Each player draws a card."

    spell {
        effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.Each))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "T2"
    }
}

class OncePerTurnInterveningIfTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(OffTurnLedger, SharedDraw))

        test("a simultaneous instance with a false intervening-if doesn't spend the once-each-turn cap") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Off-Turn Ledger")
                .withCardInHand(1, "Shared Draw")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withLifeTotal(1, 20)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Shared Draw").error shouldBe null
            game.resolveStack()

            withClue("Player2's off-turn draw triggers even though Player1's on-turn draw came first") {
                game.getLifeTotal(1) shouldBe 21
            }
        }
    }
}
