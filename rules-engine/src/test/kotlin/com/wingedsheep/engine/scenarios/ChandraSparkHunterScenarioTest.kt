package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Chandra, Spark Hunter (DFT #116, {3}{R}, Loyalty 4).
 *
 *   At the beginning of combat on your turn, choose up to one target Vehicle you control. Until end
 *   of turn, it becomes an artifact creature and gains haste.
 *   +2: You may sacrifice an artifact or discard a card. If you do, draw a card.
 *   0: Create a 3/2 colorless Vehicle artifact token with crew 1.
 *   −7: You get an emblem with "Whenever an artifact you control enters, this emblem deals 3 damage
 *       to any target."
 *
 * Covers the three abilities whose composition could silently go wrong: the combat trigger's
 * type-plus-haste animation (haste is what makes a freshly-cast Vehicle able to attack, so the test
 * actually attacks with it), the +2's may-then-choose shape (declining must not draw), and the
 * triggered emblem, which has to outlive Chandra dying to her own −7.
 */
class ChandraSparkHunterScenarioTest : ScenarioTestBase() {

    init {
        context("the begin-combat animation") {

            test("animates a Vehicle you control and hastes it into an attack") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Spark Hunter")
                    .withCardOnBattlefield(1, "Adventurer's Airship", summoningSickness = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val airship = game.findPermanent("Adventurer's Airship")!!

                withClue("an uncrewed Vehicle is a noncreature artifact") {
                    game.state.projectedState.isCreature(airship) shouldBe false
                }

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.selectTargets(listOf(airship))
                game.resolveStack()

                withClue("the Vehicle is now an artifact creature with its printed 3/2") {
                    val projected = game.state.projectedState
                    projected.isCreature(airship) shouldBe true
                    projected.getProjectedValues(airship)?.power shouldBe 3
                    projected.getProjectedValues(airship)?.toughness shouldBe 2
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("haste lets it attack the turn it became a creature") {
                    game.declareAttackers(mapOf("Adventurer's Airship" to 2)).error shouldBe null
                }
            }
        }

        context("the +2") {

            test("discarding draws a replacement") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Spark Hunter")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Spark Hunter")!!
                setLoyalty(game, chandra, 4)
                val libraryBefore = game.librarySize(1)

                activate(game, chandra, index = 0)
                game.resolveStack()
                game.answerYesNo(true)
                // Chandra is the only permanent and she isn't an artifact, so "Sacrifice an
                // artifact" is filtered out and the discard branch is auto-selected.
                chooseIfOffered(game, "Discard a card")
                if (game.state.pendingDecision is SelectCardsDecision) {
                    game.selectCards(game.findCardsInHand(1, "Grizzly Bears"))
                }
                game.resolveStack()

                withClue("the chosen card was discarded") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("and \"if you do\" drew one") {
                    game.librarySize(1) shouldBe libraryBefore - 1
                    game.handSize(1) shouldBe 1
                    game.isInHand(1, "Hill Giant") shouldBe true
                }
                withClue("+2 moved Chandra from 4 to 6 loyalty") {
                    loyalty(game, chandra) shouldBe 6
                }
            }

            test("declining draws nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Spark Hunter")
                    .withCardInHand(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Spark Hunter")!!
                val libraryBefore = game.librarySize(1)

                activate(game, chandra, index = 0)
                game.resolveStack()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("no discard, no sacrifice, and therefore no draw") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                    game.librarySize(1) shouldBe libraryBefore
                    game.handSize(1) shouldBe 1
                }
            }
        }

        context("the 0") {

            test("creates a 3/2 Vehicle token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Spark Hunter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Spark Hunter")!!
                setLoyalty(game, chandra, 4)
                activate(game, chandra, index = 1)
                game.resolveStack()

                val token = game.findPermanent("Vehicle")
                withClue("a Vehicle token entered — noncreature until something crews it") {
                    token shouldNotBe null
                    game.state.projectedState.isCreature(token!!) shouldBe false
                }
                withClue("0 leaves loyalty where it was") {
                    loyalty(game, chandra) shouldBe 4
                }
            }
        }

        context("the −7 emblem") {

            test("outlives Chandra and burns for 3 when an artifact enters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Spark Hunter")
                    .withCardInHand(1, "Sol Ring")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Spark Hunter")!!
                setLoyalty(game, chandra, 7)

                activate(game, chandra, index = 2)
                game.resolveStack()

                withClue("Chandra died to the 0-loyalty state-based action; the emblem is global") {
                    game.findPermanent("Chandra, Spark Hunter") shouldBe null
                    game.state.globalGrantedTriggeredAbilities.size shouldBe 1
                }

                game.castSpell(1, "Sol Ring").error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.selectTargets(listOf(game.player2Id))
                game.resolveStack()

                withClue("the emblem's trigger dealt 3 to the chosen target") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }
        }
    }

    private fun activate(game: TestGame, source: com.wingedsheep.sdk.model.EntityId, index: Int) {
        val ability = cardRegistry.getCard("Chandra, Spark Hunter")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id
            )
        ).error shouldBe null
    }

    /** The choose-an-action prompt only appears when more than one branch is feasible. */
    private fun chooseIfOffered(game: TestGame, label: String) {
        val decision = game.state.pendingDecision as? ChooseOptionDecision ?: return
        val index = decision.options.indexOfFirst { it.contains(label) }
        if (index >= 0) game.submitDecision(OptionChosenResponse(decision.id, index))
    }

    private fun loyalty(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: com.wingedsheep.sdk.model.EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
