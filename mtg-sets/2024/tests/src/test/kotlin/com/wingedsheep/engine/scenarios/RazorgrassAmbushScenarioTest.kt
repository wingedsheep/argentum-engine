package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Razorgrass Ambush // Razorgrass Field (MH3).
 *
 * Front: "Razorgrass Ambush deals 3 damage to target attacking or blocking creature." Back: "As this
 * land enters, you may pay 3 life. If you don't, it enters tapped. {T}: Add {W}."
 */
class RazorgrassAmbushScenarioTest : ScenarioTestBase() {

    private fun combatGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Razorgrass Ambush")
        .withLandsOnBattlefield(1, "Plains", 2)
        .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun landGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Razorgrass Ambush")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Razorgrass Ambush — the instant front") {

            test("deals 3 damage to target attacking creature") {
                val game = combatGame()
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Razorgrass Ambush", targetId = giant).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isInGraveyard(1, "Hill Giant") shouldBe true
            }

            test("deals 3 damage to target blocking creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Razorgrass Ambush")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
                game.passPriority() // blocking player passes; active player gets priority

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Razorgrass Ambush", targetId = giant).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }

            test("can't target a creature that isn't attacking or blocking") {
                val game = combatGame()
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Razorgrass Ambush", targetId = bears).error shouldNotBe null
                (game.state.getEntity(bears)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
            }
        }

        context("Razorgrass Field — the land back") {

            test("paying 3 life has it enter untapped") {
                val game = landGame()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
                game.answerYesNo(true).error shouldBe null

                val land = game.findPermanent("Razorgrass Field")!!
                game.getLifeTotal(1) shouldBe 17
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe false
            }

            test("declining to pay has it enter tapped") {
                val game = landGame()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
                game.answerYesNo(false).error shouldBe null

                val land = game.findPermanent("Razorgrass Field")!!
                game.getLifeTotal(1) shouldBe 20
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
