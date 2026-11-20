package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Searing Blood (BNG) — "Searing Blood deals 2 damage to target creature. When that creature dies
 * this turn, Searing Blood deals 3 damage to the creature's controller."
 *
 * The 3 damage goes to the dead creature's last-known controller, read after it has left the
 * battlefield; the rider watches the creature for any death that turn, and stays silent if it lives.
 */
class SearingBloodScenarioTest : ScenarioTestBase() {

    init {
        context("Searing Blood — 2 to a creature, 3 to its controller if it dies") {

            test("killing an opposing creature deals 3 damage to its controller") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Searing Blood")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Searing Blood", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("the Bears died and the opponent took 3") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    game.getLifeTotal(2) shouldBe 17
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("the caster takes the 3 when the creature was their own") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Searing Blood")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Searing Blood", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("\"the creature's controller\" is whoever controlled it, not the opponent") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.getLifeTotal(1) shouldBe 17
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("a creature that survives the damage but dies later this turn still triggers it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Searing Blood")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Searing Blood", targetId = giant).error shouldBe null
                game.resolveStack()

                withClue("a 3/3 survives 2 damage, so nothing has happened yet") {
                    game.findPermanent("Hill Giant") shouldBe giant
                    game.getLifeTotal(2) shouldBe 20
                }

                game.castSpell(1, "Shock", targetId = giant).error shouldBe null
                game.resolveStack()

                withClue("Shock finishes it; the watched death deals 3 to its controller") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("no damage when the creature survives the turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Searing Blood")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Searing Blood", targetId = giant).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.ENDING, Step.END)
                withClue("the Giant lived; the rider never fired") {
                    game.findPermanent("Hill Giant") shouldBe giant
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
