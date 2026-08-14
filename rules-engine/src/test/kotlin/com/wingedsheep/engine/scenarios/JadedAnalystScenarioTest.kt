package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mkm.cards.JadedAnalyst
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Jaded Analyst (MKM #62).
 *
 * "Defender. Whenever you draw your second card each turn, this creature loses defender and gains
 *  vigilance until end of turn."
 *
 * The interesting half is that the payoff has to *remove a printed keyword* and have attack
 * legality notice — a defender that merely gains vigilance is still stuck at home. These tests
 * drive real draws through free instants (so the only variable is the draw count, per
 * [com.wingedsheep.sdk.dsl.Triggers.NthCardDrawn] / CR 121.2), then assert both the projected
 * keywords and that the Analyst can actually be declared as an attacker.
 */
class JadedAnalystScenarioTest : ScenarioTestBase() {

    // A free instant so a cast costs no mana and the only thing under test is the draw count.
    private val drawOne = card("Jaded Draw One Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Draw a card."
        spell { effect = Effects.DrawCards(1) }
    }

    init {
        cardRegistry.register(JadedAnalyst)
        cardRegistry.register(drawOne)

        test("the second draw of the turn strips defender and grants vigilance; the first does not") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Jaded Analyst", summoningSickness = false)
                .withCardsInHand(1, "Jaded Draw One Test", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardsDrawnThisTurn(1, 0)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val analyst = game.findPermanent("Jaded Analyst")!!

            game.castSpell(1, "Jaded Draw One Test").error shouldBe null
            game.resolveStack()

            withClue("the first draw of the turn advances no NthCardDrawn(2)") {
                val projected = StateProjector().project(game.state)
                projected.hasKeyword(analyst, Keyword.DEFENDER) shouldBe true
                projected.hasKeyword(analyst, Keyword.VIGILANCE) shouldBe false
            }

            game.castSpell(1, "Jaded Draw One Test").error shouldBe null
            game.resolveStack()

            withClue("the second draw sheds defender and grants vigilance") {
                val projected = StateProjector().project(game.state)
                projected.hasKeyword(analyst, Keyword.DEFENDER) shouldBe false
                projected.hasKeyword(analyst, Keyword.VIGILANCE) shouldBe true
            }
        }

        test("the unlocked Analyst can be declared as an attacker") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Jaded Analyst", summoningSickness = false)
                .withCardsInHand(1, "Jaded Draw One Test", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardsDrawnThisTurn(1, 0)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Jaded Draw One Test").error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Jaded Draw One Test").error shouldBe null
            game.resolveStack()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            val attack = game.declareAttackers(mapOf("Jaded Analyst" to 2))
            withClue("with defender gone the Analyst may attack: ${attack.error}") {
                attack.error shouldBe null
            }
        }

        test("without a second draw the Analyst still can't attack") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Jaded Analyst", summoningSickness = false)
                .withCardsDrawnThisTurn(1, 0)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            val attack = game.declareAttackers(mapOf("Jaded Analyst" to 2))
            withClue("defender is still on, so the attack is illegal") {
                attack.error shouldNotBe null
            }
        }
    }
}
