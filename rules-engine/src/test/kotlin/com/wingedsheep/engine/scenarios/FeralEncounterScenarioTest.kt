package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Feral Encounter.
 *
 * Two pieces of new vocabulary get exercised here:
 *  - [com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect.additionalTargetRequirements]
 *    — the delayed combat trigger picks *two* targets when it fires.
 *  - [com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming.THIS_TURN_ONLY] — "the next combat
 *    phase **this turn**", so a copy cast after combat never fires.
 */
class FeralEncounterScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.ScenarioBuilder.withFiveCardLibrary() = this
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Mountain")

    init {
        context("the dig") {
            test("exiles a creature card from the top five and lets you cast it this turn") {
                val game = scenario()
                    .withPlayers("Hunter", "Opponent")
                    .withCardInHand(1, "Feral Encounter")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withFiveCardLibrary()
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Feral Encounter").error shouldBe null
                game.resolveStack()

                val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.isInExile(1, "Grizzly Bears") shouldBe true
                // The other four went to the bottom rather than anywhere else.
                game.librarySize(1) shouldBe 4

                game.castSpellFromExile(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("declining the exile leaves all five cards in the library") {
                val game = scenario()
                    .withPlayers("Hunter", "Opponent")
                    .withCardInHand(1, "Feral Encounter")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withFiveCardLibrary()
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Feral Encounter").error shouldBe null
                game.resolveStack()

                game.skipSelection().error shouldBe null
                game.resolveStack()

                game.librarySize(1) shouldBe 5
                game.isInExile(1, "Grizzly Bears") shouldBe false
            }
        }

        context("the delayed combat trigger") {
            test("picks both targets when it fires and deals power-equal damage") {
                val game = scenario()
                    .withPlayers("Hunter", "Opponent")
                    .withCardInHand(1, "Feral Encounter")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withFiveCardLibrary()
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mine = game.findPermanents("Grizzly Bears").first { entityId ->
                    game.state.projectedState.getController(entityId) == game.player1Id
                }
                val theirs = game.findPermanents("Grizzly Bears").first { entityId ->
                    game.state.projectedState.getController(entityId) == game.player2Id
                }

                game.castSpell(1, "Feral Encounter").error shouldBe null
                game.resolveStack()
                game.skipSelection().error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

                val decision = game.getPendingDecision()
                decision.shouldBeChooseTargets()
                (decision as ChooseTargetsDecision).targetRequirements.size shouldBe 2

                game.submitDecision(
                    TargetsResponse(decision.id, mapOf(0 to listOf(mine), 1 to listOf(theirs)))
                ).error shouldBe null
                game.resolveStack()

                // A 2/2 dealing 2 to a 2/2 kills it; the attacker is untouched (not a fight).
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.state.projectedState.getController(theirs) shouldBe null
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }

            test("up to one — the trigger still fires with no creature to shoot") {
                val game = scenario()
                    .withPlayers("Hunter", "Opponent")
                    .withCardInHand(1, "Feral Encounter")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withFiveCardLibrary()
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Feral Encounter").error shouldBe null
                game.resolveStack()
                game.skipSelection().error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

                val decision = game.getPendingDecision()
                decision.shouldBeChooseTargets()
                val mine = game.findPermanent("Grizzly Bears")!!
                game.submitDecision(
                    TargetsResponse(
                        (decision as ChooseTargetsDecision).id,
                        mapOf(0 to listOf(mine), 1 to emptyList())
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.getLifeTotal(2) shouldBe 20
            }

            test("cast after combat, the trigger never fires — it is scoped to this turn") {
                val game = scenario()
                    .withPlayers("Hunter", "Opponent")
                    .withCardInHand(1, "Feral Encounter")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withFiveCardLibrary()
                    // The opponent takes a turn below — they need something to draw.
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Feral Encounter").error shouldBe null
                game.resolveStack()
                game.skipSelection().error shouldBe null
                game.resolveStack()

                // Next combat phase in the game belongs to the opponent's turn.
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.state.step shouldBe Step.BEGIN_COMBAT
                game.state.activePlayerId shouldBe game.player2Id

                game.hasPendingDecision() shouldBe false
                game.findPermanents("Grizzly Bears").size shouldBe 2
            }
        }
    }

    private fun Any?.shouldBeChooseTargets() {
        this shouldNotBe null
        (this is ChooseTargetsDecision) shouldBe true
    }
}
