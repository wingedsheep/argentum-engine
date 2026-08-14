package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mirkwood Pathmaker (HOB) — {2}{G} Creature — Elf Ranger 0/0.
 * "Mirkwood Pathmaker's power and toughness are each equal to the number of lands you control."
 *
 * A characteristic-defining ability, so the value has to be *live*: it must track lands appearing
 * and disappearing rather than being snapshotted once, and must count only lands you control.
 */
class MirkwoodPathmakerScenarioTest : ScenarioTestBase() {

    init {
        context("Mirkwood Pathmaker") {

            test("entering with no lands it is a 0/0 and dies to state-based actions") {
                // Driven through the real cast/resolve path (mana granted directly, so the
                // battlefield really is landless when it enters) — CR 704.5f.
                val driver = GameTestDriver()
                driver.registerCards(TestCards.all)
                driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, skipMulligans = true)
                val p1 = driver.activePlayer!!
                driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

                val card = driver.putCardInHand(p1, "Mirkwood Pathmaker")
                driver.giveMana(p1, Color.GREEN, 1)
                driver.giveColorlessMana(p1, 2)
                withClue("the scenario really has no lands in play") {
                    driver.getLands(p1).size shouldBe 0
                }

                driver.castSpell(p1, card, emptyList()).error shouldBe null
                driver.bothPass()

                withClue("a 0/0 body cannot survive state-based actions") {
                    driver.findPermanent(p1, "Mirkwood Pathmaker") shouldBe null
                    driver.getGraveyardCardNames(p1).contains("Mirkwood Pathmaker") shouldBe true
                }
            }

            test("its power and toughness equal the number of lands you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mirkwood Pathmaker")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pathmaker = game.findPermanent("Mirkwood Pathmaker")!!
                withClue("three lands → 3/3") {
                    game.state.projectedState.getPower(pathmaker) shouldBe 3
                    game.state.projectedState.getToughness(pathmaker) shouldBe 3
                }
            }

            test("the value tracks a land entering — it is not snapshotted") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mirkwood Pathmaker")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInHand(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pathmaker = game.findPermanent("Mirkwood Pathmaker")!!
                game.state.projectedState.getPower(pathmaker) shouldBe 2

                game.execute(
                    PlayLand(game.player1Id, game.findCardsInHand(1, "Forest").single())
                ).error shouldBe null
                game.resolveStack()

                withClue("the third land immediately makes it a 3/3") {
                    game.state.projectedState.getPower(pathmaker) shouldBe 3
                    game.state.projectedState.getToughness(pathmaker) shouldBe 3
                }
            }

            test("lands an opponent controls are not counted") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mirkwood Pathmaker")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(2, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pathmaker = game.findPermanent("Mirkwood Pathmaker")!!
                withClue("only the two lands *you* control count") {
                    game.state.projectedState.getPower(pathmaker) shouldBe 2
                    game.state.projectedState.getToughness(pathmaker) shouldBe 2
                }
            }
        }
    }
}
