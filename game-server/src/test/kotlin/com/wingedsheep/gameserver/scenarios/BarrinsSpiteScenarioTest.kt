package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Barrin's Spite.
 *
 * {2}{U}{B} Sorcery — "Choose two target creatures controlled by the same player.
 * Their controller chooses and sacrifices one of them. Return the other to its
 * owner's hand."
 *
 * Exercises the new `sameController` targeting constraint and the
 * `Chooser.ControllerOfSelection` selection chooser.
 */
class BarrinsSpiteScenarioTest : ScenarioTestBase() {

    init {
        context("Barrin's Spite") {
            test("controller of the two creatures sacrifices one and returns the other to hand") {
                val game = scenario()
                    .withPlayers("Caster", "Victim")
                    .withCardInHand(1, "Barrin's Spite")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(2, "Rampant Elephant")
                    .withCardOnBattlefield(2, "Capashen Unicorn")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elephant = game.findPermanent("Rampant Elephant")!!
                val unicorn = game.findPermanent("Capashen Unicorn")!!
                val spellId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Barrin's Spite"
                }

                val castResult = game.execute(
                    CastSpell(
                        game.player1Id,
                        spellId,
                        listOf(ChosenTarget.Permanent(elephant), ChosenTarget.Permanent(unicorn))
                    )
                )
                withClue("Barrin's Spite should cast targeting two creatures the opponent controls") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // The creatures' controller (player 2) must choose which to sacrifice.
                val decision = game.getPendingDecision()
                withClue("There should be a pending sacrifice-choice decision") {
                    decision shouldNotBe null
                }
                withClue("The decision belongs to the creatures' controller (player 2)") {
                    decision!!.playerId shouldBe game.player2Id
                }

                // Player 2 sacrifices the Elephant; the Unicorn is "the other".
                game.selectCards(listOf(elephant))

                withClue("Chosen creature (Elephant) is sacrificed to its owner's graveyard") {
                    game.isInGraveyard(2, "Rampant Elephant") shouldBe true
                }
                withClue("The other creature (Unicorn) is returned to its owner's hand") {
                    game.isInHand(2, "Capashen Unicorn") shouldBe true
                }
                withClue("Neither creature remains on the battlefield") {
                    game.isOnBattlefield("Rampant Elephant") shouldBe false
                    game.isOnBattlefield("Capashen Unicorn") shouldBe false
                }
            }

            test("cannot target two creatures controlled by different players") {
                val game = scenario()
                    .withPlayers("Caster", "Other")
                    .withCardInHand(1, "Barrin's Spite")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Rampant Elephant")
                    .withCardOnBattlefield(2, "Capashen Unicorn")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elephant = game.findPermanent("Rampant Elephant")!!
                val unicorn = game.findPermanent("Capashen Unicorn")!!
                val spellId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Barrin's Spite"
                }

                val castResult = game.execute(
                    CastSpell(
                        game.player1Id,
                        spellId,
                        listOf(ChosenTarget.Permanent(elephant), ChosenTarget.Permanent(unicorn))
                    )
                )
                withClue("Targeting two differently-controlled creatures must be rejected") {
                    castResult.error shouldNotBe null
                }
                withClue("Both creatures remain on the battlefield after the illegal cast") {
                    game.isOnBattlefield("Rampant Elephant") shouldBe true
                    game.isOnBattlefield("Capashen Unicorn") shouldBe true
                }
            }
        }
    }
}
