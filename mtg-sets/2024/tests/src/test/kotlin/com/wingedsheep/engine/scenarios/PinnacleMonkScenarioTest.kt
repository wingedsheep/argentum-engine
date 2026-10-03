package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Pinnacle Monk // Mystic Peak (MH3).
 *
 * Front: "Prowess. When this creature enters, return target instant or sorcery card from your
 * graveyard to your hand." Back: "As this land enters, you may pay 3 life. If you don't, it enters
 * tapped. {T}: Add {R}."
 */
class PinnacleMonkScenarioTest : ScenarioTestBase() {

    /** Let the stack resolve, answering the enters trigger's target prompt with [pick] if asked. */
    private fun TestGame.resolveWithTarget(pick: com.wingedsheep.sdk.model.EntityId) {
        var guard = 0
        while ((state.stack.isNotEmpty() || getPendingDecision() != null) && guard++ < 20) {
            if (getPendingDecision() is ChooseTargetsDecision) {
                selectTargets(listOf(pick)).error shouldBe null
            } else {
                passPriority()
            }
        }
    }

    init {
        context("Pinnacle Monk — the creature front") {

            test("enters and returns an instant from your graveyard; prowess then pumps it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Pinnacle Monk")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(2, "Shock")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
                game.castSpell(1, "Pinnacle Monk").error shouldBe null
                game.resolveWithTarget(bolt)

                val monk = game.findPermanent("Pinnacle Monk")!!
                withClue("the instant came back; the creature card and the opponent's Shock did not") {
                    game.isInHand(1, "Lightning Bolt") shouldBe true
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Shock") shouldBe true
                }

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                withClue("prowess triggers on the noncreature spell") {
                    var guard = 0
                    while (game.state.stack.isNotEmpty() && guard++ < 10) game.passPriority()
                    game.state.projectedState.getPower(monk) shouldBe 3
                    game.state.projectedState.getToughness(monk) shouldBe 3
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("with only creature cards in the graveyard, the trigger has no target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Pinnacle Monk")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Pinnacle Monk").error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Pinnacle Monk") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
        }

        context("Mystic Peak — the land back") {

            fun landGame() = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Pinnacle Monk")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("paying 3 life has it enter untapped") {
                val game = landGame()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
                game.answerYesNo(true).error shouldBe null

                val land = game.findPermanent("Mystic Peak")!!
                game.getLifeTotal(1) shouldBe 17
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe false
            }

            test("declining to pay has it enter tapped") {
                val game = landGame()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
                game.answerYesNo(false).error shouldBe null

                val land = game.findPermanent("Mystic Peak")!!
                game.getLifeTotal(1) shouldBe 20
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
