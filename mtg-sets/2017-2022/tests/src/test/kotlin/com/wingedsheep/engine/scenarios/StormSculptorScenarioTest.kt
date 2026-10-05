package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Storm Sculptor (XLN) — "This creature can't be blocked. When this creature enters, return a
 * creature you control to its owner's hand." The return is chosen on resolution (no target), is
 * mandatory, and the Sculptor itself is a legal choice.
 */
class StormSculptorScenarioTest : ScenarioTestBase() {

    init {
        context("Storm Sculptor enters trigger") {
            test("alone: the Sculptor must return itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Storm Sculptor")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Storm Sculptor").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Storm Sculptor") shouldBe false
                game.isInHand(1, "Storm Sculptor") shouldBe true
            }

            test("with another creature: only your creatures are offered, and you may keep the Sculptor") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Storm Sculptor")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Storm Sculptor").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision() as SelectCardsDecision
                val bears = game.findPermanent("Grizzly Bears")
                bears shouldNotBe null
                decision.options.contains(game.findPermanent("Hill Giant")!!) shouldBe false
                decision.options.contains(game.findPermanent("Storm Sculptor")!!) shouldBe true
                game.selectCards(listOf(bears!!)).error shouldBe null

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Storm Sculptor") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
            }

            test("with another creature: the Sculptor may return itself instead") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Storm Sculptor")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Storm Sculptor").error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                val sculptor = game.findPermanent("Storm Sculptor")!!
                game.selectCards(listOf(sculptor)).error shouldBe null

                game.isInHand(1, "Storm Sculptor") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }

        context("Storm Sculptor can't be blocked") {
            test("an attacking Sculptor can't be blocked, and deals its 3 damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Storm Sculptor")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Storm Sculptor" to 2, "Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                game.declareBlockers(mapOf("Hill Giant" to listOf("Storm Sculptor"))).error shouldNotBe null
                withClue("control: the Hill Giant can still block a blockable attacker") {
                    game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
                }
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.getLifeTotal(2) shouldBe 17
            }
        }
    }
}
