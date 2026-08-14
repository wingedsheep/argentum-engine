package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Moment of Glory (HOB #21) — {W} Sorcery.
 *
 * "Put a +1/+1 counter on target creature you control. If this spell was cast from a graveyard,
 *  also put a +1/+1 counter on each other creature you control.
 *  Flashback {4}{W}"
 *
 * Two things worth proving: the bonus clause is off on a hand cast and on for the flashback cast,
 * and "each **other** creature" is other than the *target* — the target must not be counted twice.
 */
class MomentOfGloryScenarioTest : ScenarioTestBase() {

    init {
        context("Moment of Glory") {

            test("cast from hand it only grows the target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Moment of Glory")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val courser = game.findPermanent("Centaur Courser")!!

                game.castSpell(1, "Moment of Glory", bears).error shouldBe null
                game.resolveStack()

                withClue("only the target got a counter") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getPower(courser) shouldBe 3
                }
            }

            test("cast from the graveyard with flashback it also grows each other creature you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Moment of Glory")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myBears = game.findAllPermanents("Grizzly Bears")
                    .single { game.state.projectedState.getController(it) == game.player1Id }
                val theirBears = game.findAllPermanents("Grizzly Bears")
                    .single { game.state.projectedState.getController(it) == game.player2Id }
                val courser = game.findPermanent("Centaur Courser")!!

                game.castSpellFromGraveyard(1, "Moment of Glory", myBears).error shouldBe null
                game.resolveStack()

                withClue("the target got exactly one counter — 'each other' excludes it") {
                    game.state.projectedState.getPower(myBears) shouldBe 3
                    game.state.projectedState.getToughness(myBears) shouldBe 3
                }
                withClue("your other creature got the bonus counter") {
                    game.state.projectedState.getPower(courser) shouldBe 4
                    game.state.projectedState.getToughness(courser) shouldBe 4
                }
                withClue("'you control' keeps the opponent's creature out of it") {
                    game.state.projectedState.getPower(theirBears) shouldBe 2
                }
                withClue("flashback exiles the card") {
                    game.isInGraveyard(1, "Moment of Glory") shouldBe false
                    game.isInExile(1, "Moment of Glory") shouldBe true
                }
            }
        }
    }
}
