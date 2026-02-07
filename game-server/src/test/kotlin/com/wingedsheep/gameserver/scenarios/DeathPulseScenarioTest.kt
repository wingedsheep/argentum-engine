package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class DeathPulseScenarioTest : ScenarioTestBase() {

    init {
        context("Death Pulse - cast as spell") {
            test("gives target creature -4/-4 until end of turn, killing a small creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Death Pulse")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targetId = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Death Pulse", targetId)
                game.resolveStack()

                withClue("Grizzly Bears should be dead from -4/-4") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("gives target creature -4/-4, large creature survives") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Death Pulse")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, "Towering Baloth") // 7/6
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targetId = game.findPermanent("Towering Baloth")!!
                game.castSpell(1, "Death Pulse", targetId)
                game.resolveStack()

                withClue("Towering Baloth should survive with 3/2") {
                    game.isOnBattlefield("Towering Baloth") shouldBe true
                }

                val clientState = game.getClientState(1)
                val cardInfo = clientState.cards[targetId]
                withClue("Towering Baloth card info should exist") {
                    cardInfo shouldNotBe null
                }
                withClue("Towering Baloth should have 3 power") {
                    cardInfo!!.power shouldBe 3
                }
                withClue("Towering Baloth should have 2 toughness") {
                    cardInfo!!.toughness shouldBe 2
                }
            }
        }

        context("Death Pulse - cycling trigger") {
            test("cycling trigger gives target creature -1/-1 when player chooses yes") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Death Pulse")
                    .withLandsOnBattlefield(1, "Swamp", 3) // {1}{B}{B} cycling cost
                    .withCardInLibrary(1, "Forest") // Card to draw from cycling
                    .withCardOnBattlefield(2, "Elvish Warrior") // 2/3 - survives -1/-1
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cycle Death Pulse
                game.cycleCard(1, "Death Pulse")

                // MayEffect asks yes/no
                withClue("Death Pulse cycling trigger should present may decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                // Select target creature
                val targetId = game.findPermanent("Elvish Warrior")!!
                game.selectTargets(listOf(targetId))

                // Resolve the triggered ability on the stack
                game.resolveStack()

                withClue("Elvish Warrior should survive with 1/2") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe true
                }

                val clientState = game.getClientState(1)
                val cardInfo = clientState.cards[targetId]
                withClue("Elvish Warrior card info should exist") {
                    cardInfo shouldNotBe null
                }
                withClue("Elvish Warrior should have 1 power") {
                    cardInfo!!.power shouldBe 1
                }
                withClue("Elvish Warrior should have 2 toughness") {
                    cardInfo!!.toughness shouldBe 2
                }

                withClue("Death Pulse should be in graveyard") {
                    game.isInGraveyard(1, "Death Pulse") shouldBe true
                }
            }

            test("cycling trigger kills a 1/1 creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Death Pulse")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardOnBattlefield(2, "Festering Goblin") // 1/1
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cycleCard(1, "Death Pulse")

                game.answerYesNo(true)

                val targetId = game.findPermanent("Festering Goblin")!!
                game.selectTargets(listOf(targetId))

                game.resolveStack()

                withClue("Festering Goblin should be dead from -1/-1") {
                    game.isOnBattlefield("Festering Goblin") shouldBe false
                }
            }

            test("cycling trigger does nothing when player declines") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Death Pulse")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cycleCard(1, "Death Pulse")

                // Decline the may ability
                game.answerYesNo(false)

                withClue("Glory Seeker should be unaffected") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }

                val targetId = game.findPermanent("Glory Seeker")!!
                val clientState = game.getClientState(1)
                val cardInfo = clientState.cards[targetId]
                withClue("Glory Seeker card info should exist") {
                    cardInfo shouldNotBe null
                }
                withClue("Glory Seeker should still have 2 power") {
                    cardInfo!!.power shouldBe 2
                }
                withClue("Glory Seeker should still have 2 toughness") {
                    cardInfo!!.toughness shouldBe 2
                }
            }
        }
    }
}
