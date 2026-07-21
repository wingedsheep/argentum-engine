package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.arn.cards.CuombajjWitches
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Cuombajj Witches (ARN):
 *   "{T}: This creature deals 1 damage to any target and 1 damage to any target of an
 *    opponent's choice."
 *
 * Exercises the "… of an opponent's choice" target machinery
 * ([com.wingedsheep.sdk.scripting.targets.TargetChooser.Opponent]): the controller picks their own
 * "any target" up front, the engine pauses at announcement and routes a target-selection decision
 * to the opponent for the second damage, and the opponent's pick is a real target whose legality is
 * measured relative to the *controller* (the printed hexproof ruling). Both pings are dealt when
 * the ability resolves.
 */
class CuombajjWitchesScenarioTest : ScenarioTestBase() {

    private fun abilityId() = cardRegistry.getCard("Cuombajj Witches")!!.activatedAbilities.first().id

    init {
        context("Cuombajj Witches — 1 damage to any target + 1 to any target of an opponent's choice") {

            test("controller pings the opponent; the opponent then pings the controller") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cuombajj Witches", summoningSickness = false)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val witches = game.findPermanent("Cuombajj Witches")!!

                // Controller activates, choosing the opponent (Player 2) for the first damage.
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = witches,
                        abilityId = abilityId(),
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                    )
                )
                withClue("Activation should succeed: ${result.error}") { result.error shouldBe null }

                // The ability is not on the stack yet — it pauses for the opponent's target choice,
                // routed to Player 2.
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("The opponent's-choice decision is routed to Player 2") {
                    decision.playerId shouldBe game.player2Id
                }

                // Opponent (Player 2) aims the second damage at the controller (Player 1).
                game.selectTargets(listOf(game.player1Id))
                game.resolveStack()

                withClue("Controller's 1 damage hits Player 2 (20 -> 19)") {
                    game.getLifeTotal(2) shouldBe 19
                }
                withClue("Opponent's-choice 1 damage hits Player 1 (20 -> 19)") {
                    game.getLifeTotal(1) shouldBe 19
                }
            }

            test("opponent target legality is relative to the controller — a hexproof creature the opponent controls can't be chosen") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cuombajj Witches", summoningSickness = false)
                    .withCardOnBattlefield(2, "Cold-Water Snapper") // 4/5 with hexproof, controlled by P2
                    .withCardOnBattlefield(2, "Mons's Goblin Raiders") // 0/1 vanilla, controlled by P2
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val witches = game.findPermanent("Cuombajj Witches")!!
                val snapper = game.findPermanent("Cold-Water Snapper")!!
                val raiders = game.findPermanent("Mons's Goblin Raiders")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = witches,
                        abilityId = abilityId(),
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                    )
                )
                withClue("Activation should succeed: ${result.error}") { result.error shouldBe null }

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                val legal = decision.legalTargets[0]!!

                withClue("The opponent can't pick their own hexproof creature — hexproof is measured vs. the controller") {
                    legal shouldNotContain snapper
                }
                withClue("A non-hexproof creature the opponent controls is a legal opponent's-choice target") {
                    legal shouldContain raiders
                }
                withClue("Both players are legal opponent's-choice targets") {
                    legal shouldContain game.player1Id
                    legal shouldContain game.player2Id
                }
            }

            test("opponent may aim the second damage at one of the controller's own creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cuombajj Witches", summoningSickness = false)
                    .withCardOnBattlefield(1, "Mons's Goblin Raiders") // 0/1 controlled by the controller
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val witches = game.findPermanent("Cuombajj Witches")!!
                val raiders = game.findPermanent("Mons's Goblin Raiders")!!

                // Controller pings the opponent for the first damage.
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = witches,
                        abilityId = abilityId(),
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                    )
                )
                withClue("Activation should succeed: ${result.error}") { result.error shouldBe null }

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("The controller's own 0/1 is a legal opponent's-choice target") {
                    decision.legalTargets[0]!! shouldContain raiders
                }

                // Opponent retaliates by killing the controller's 0/1.
                game.selectTargets(listOf(raiders))
                game.resolveStack()

                withClue("The controller's 0/1 dies to the opponent's-choice 1 damage") {
                    game.isOnBattlefield("Mons's Goblin Raiders") shouldBe false
                }
                withClue("Controller's 1 damage still hit Player 2 (20 -> 19)") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }

            test("controller's ping kills a creature while the opponent pings a player") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cuombajj Witches", summoningSickness = false)
                    .withCardOnBattlefield(2, "Mons's Goblin Raiders") // 0/1 controlled by the opponent
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val witches = game.findPermanent("Cuombajj Witches")!!
                val raiders = game.findPermanent("Mons's Goblin Raiders")!!

                // Controller aims the first damage at the opponent's 0/1.
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = witches,
                        abilityId = abilityId(),
                        targets = listOf(ChosenTarget.Permanent(raiders)),
                    )
                )
                withClue("Activation should succeed: ${result.error}") { result.error shouldBe null }

                // Opponent's forced choice: aim the second damage at the controller.
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(game.player1Id))
                game.resolveStack()

                withClue("The opponent's 0/1 dies to the controller's 1 damage") {
                    game.isOnBattlefield("Mons's Goblin Raiders") shouldBe false
                }
                withClue("The controller takes the opponent's-choice 1 damage (20 -> 19)") {
                    game.getLifeTotal(1) shouldBe 19
                }
            }

            test("controller chooses which opponent chooses the second target in multiplayer") {
                val driver = GameTestDriver()
                driver.registerCards(TestCards.all)
                driver.registerCard(CuombajjWitches)
                val players = driver.initMultiplayer(
                    decks = List(3) { Deck.of("Forest" to 40) },
                    startingLife = 20
                )
                val controller = players[0]
                val nextOpponent = players[1]
                val chosenOpponent = players[2]

                driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
                val witches = driver.putCreatureOnBattlefield(controller, "Cuombajj Witches")
                driver.removeSummoningSickness(witches)

                val activation = driver.submit(
                    ActivateAbility(
                        playerId = controller,
                        sourceId = witches,
                        abilityId = CuombajjWitches.activatedAbilities.single().id,
                        targets = listOf(ChosenTarget.Player(nextOpponent))
                    )
                )

                activation.isPaused shouldBe true
                val opponentChoice = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                opponentChoice.playerId shouldBe controller
                opponentChoice.options shouldBe listOf("Player 2", "Player 3")

                driver.submitDecision(controller, OptionChosenResponse(opponentChoice.id, optionIndex = 1))

                val targetChoice = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
                targetChoice.playerId shouldBe chosenOpponent

                driver.submitTargetSelection(chosenOpponent, listOf(controller))

                var guard = 0
                while (driver.stackSize > 0 && guard++ < 12) {
                    driver.passPriority(driver.priorityPlayer!!)
                }

                driver.getLifeTotal(controller) shouldBe 19
                driver.getLifeTotal(nextOpponent) shouldBe 19
                driver.getLifeTotal(chosenOpponent) shouldBe 20
            }

            test("a client cannot set the internal opponentTargetsChosen flag to skip the opponent's choice") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cuombajj Witches", summoningSickness = false)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val witches = game.findPermanent("Cuombajj Witches")!!

                // A non-conforming client tries to pre-set the internal resume marker, supplying
                // only its own target — which would otherwise skip the opponent-target pause and
                // resolve the opponent-chosen damage with no target. validate() must reject it.
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = witches,
                        abilityId = abilityId(),
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        opponentTargetsChosen = true,
                    )
                )

                withClue("Activation carrying the internal flag is rejected") {
                    result.error shouldBe "Internal resume flag cannot be set by a player"
                }
                withClue("No decision was raised and no damage dealt") {
                    game.getLifeTotal(1) shouldBe 20
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
