package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Witch Enchanter // Witch-Blessed Meadow (MH3).
 *
 * Front: "When this creature enters, destroy target artifact or enchantment an opponent controls."
 * Back: "As this land enters, you may pay 3 life. If you don't, it enters tapped. {T}: Add {W}."
 */
class WitchEnchanterScenarioTest : ScenarioTestBase() {

    private fun castGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Witch Enchanter")
        .withLandsOnBattlefield(1, "Plains", 4)
        .withCardOnBattlefield(1, "Ornithopter")
        .withCardOnBattlefield(2, "Ornithopter")
        .withCardOnBattlefield(2, "Glorious Anthem")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun landGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Witch Enchanter")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Witch Enchanter — the creature front") {

            test("ETB destroys target enchantment an opponent controls") {
                val game = castGame()
                game.castSpell(1, "Witch Enchanter").error shouldBe null
                game.resolveStack()
                game.getPendingDecision() shouldNotBe null
                game.selectTargets(listOf(game.findPermanent("Glorious Anthem")!!)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Witch Enchanter") shouldBe true
                game.isOnBattlefield("Glorious Anthem") shouldBe false
                game.findPermanents("Ornithopter").size shouldBe 2
            }

            test("ETB destroys target artifact an opponent controls") {
                val game = castGame()
                game.castSpell(1, "Witch Enchanter").error shouldBe null
                game.resolveStack()
                val theirs = game.findPermanents("Ornithopter").single {
                    game.state.getBattlefield(game.player2Id).contains(it)
                }
                game.selectTargets(listOf(theirs)).error shouldBe null
                game.resolveStack()

                game.state.getBattlefield(game.player2Id).contains(theirs) shouldBe false
                game.findPermanents("Ornithopter").size shouldBe 1
                game.isOnBattlefield("Glorious Anthem") shouldBe true
            }

            test("can't target an artifact you control") {
                val game = castGame()
                game.castSpell(1, "Witch Enchanter").error shouldBe null
                game.resolveStack()
                val mine = game.findPermanents("Ornithopter").single {
                    game.state.getBattlefield(game.player1Id).contains(it)
                }
                game.selectTargets(listOf(mine)).error shouldNotBe null
            }
        }

        context("Witch-Blessed Meadow — the land back") {

            test("paying 3 life has it enter untapped") {
                val game = landGame()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
                game.answerYesNo(true).error shouldBe null

                val land = game.findPermanent("Witch-Blessed Meadow")!!
                game.getLifeTotal(1) shouldBe 17
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe false
            }

            test("declining to pay has it enter tapped") {
                val game = landGame()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
                game.answerYesNo(false).error shouldBe null

                val land = game.findPermanent("Witch-Blessed Meadow")!!
                game.getLifeTotal(1) shouldBe 20
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
