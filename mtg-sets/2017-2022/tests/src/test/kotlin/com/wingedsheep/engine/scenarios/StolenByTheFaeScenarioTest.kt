package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Stolen by the Fae (ELD #66) — {X}{U}{U} Sorcery.
 * "Return target creature with mana value X to its owner's hand. You create X 1/1 blue Faerie
 *  creature tokens with flying."
 */
class StolenByTheFaeScenarioTest : ScenarioTestBase() {

    init {
        context("Stolen by the Fae") {
            test("X=2 bounces a mana value 2 creature to its owner's hand and creates two 1/1 blue flying Faeries") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Stolen by the Fae")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castXSpell(1, "Stolen by the Fae", xValue = 2, targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears returns to its owner's (the opponent's) hand") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                }

                val faeries = game.findPermanents("Faerie Token")
                withClue("X = 2 Faerie tokens are created") { faeries shouldHaveSize 2 }
                withClue("Each Faerie is a 1/1 blue flier under the caster's control") {
                    faeries.forEach { faerie ->
                        game.state.getBattlefield(game.player1Id).contains(faerie) shouldBe true
                        game.state.projectedState.getPower(faerie) shouldBe 1
                        game.state.projectedState.getToughness(faerie) shouldBe 1
                        game.state.projectedState.hasKeyword(faerie, Keyword.FLYING) shouldBe true
                        game.state.projectedState.getColors(faerie) shouldBe setOf(Color.BLUE.name)
                    }
                }
                game.isInGraveyard(1, "Stolen by the Fae") shouldBe true
            }

            test("bouncing your own creature returns it to your hand and still creates X Faeries") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Stolen by the Fae")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castXSpell(1, "Stolen by the Fae", xValue = 4, targetId = giant).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Hill Giant") shouldBe true
                game.findPermanents("Faerie Token") shouldHaveSize 4
            }

            test("the target must have mana value exactly X") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Stolen by the Fae")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("X = 3 can't target a mana value 2 creature") {
                    game.castXSpell(1, "Stolen by the Fae", xValue = 3, targetId = bears).error shouldNotBe null
                }
                withClue("X = 1 can't target a mana value 2 creature") {
                    game.castXSpell(1, "Stolen by the Fae", xValue = 1, targetId = bears).error shouldNotBe null
                }
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isInHand(1, "Stolen by the Fae") shouldBe true
            }

            test("if the target becomes illegal, the spell doesn't resolve and no Faeries are created") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Stolen by the Fae")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(2, "Unsummon")
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castXSpell(1, "Stolen by the Fae", xValue = 2, targetId = bears).error shouldBe null

                // The opponent bounces their own Bears in response, leaving the spell with no legal target.
                game.passPriority()
                game.castSpell(2, "Unsummon", targetId = bears).error shouldBe null
                game.resolveStack()

                game.isInHand(2, "Grizzly Bears") shouldBe true
                withClue("Stolen by the Fae didn't resolve — no Faerie tokens") {
                    game.findPermanents("Faerie Token").shouldBeEmpty()
                }
                game.isInGraveyard(1, "Stolen by the Fae") shouldBe true
            }
        }
    }
}
