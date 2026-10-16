package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Dogmeat, Ever Loyal (PIP #2).
 *
 * - ETB: mill five, then return an Aura or Equipment card from your graveyard to your hand. The
 *   return is mandatory and may pick a just-milled card or one that was already there (ruling).
 * - Whenever a creature you control that's enchanted or equipped attacks, create a Junk token —
 *   once per such attacker, never for a bare one.
 */
class DogmeatEverLoyalScenarioTest : ScenarioTestBase() {

    private fun TestGame.junkCount() = findAllPermanents("Junk").size

    private fun TestGame.castDogmeat() {
        castSpell(1, "Dogmeat, Ever Loyal").error shouldBe null
        var guard = 0
        while (getPendingDecision() == null && state.stack.isNotEmpty() && guard++ < 20) passPriority()
    }

    init {
        context("Dogmeat, Ever Loyal — enters") {
            test("mills five, then returns a just-milled Aura or an Equipment already in the graveyard") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Dogmeat, Ever Loyal")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Holy Strength")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Bonesplitter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castDogmeat()

                withClue("all five cards were milled") {
                    game.librarySize(1) shouldBe 0
                    game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 4
                }
                val decision = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected a SelectCardsDecision, got ${game.getPendingDecision()}")
                val holy = game.findCardsInGraveyard(1, "Holy Strength").single()
                val bonesplitter = game.findCardsInGraveyard(1, "Bonesplitter").single()
                withClue("exactly one, chosen among the Aura and Equipment cards only") {
                    decision.minSelections shouldBe 1
                    decision.maxSelections shouldBe 1
                    decision.options shouldContainExactlyInAnyOrder listOf(holy, bonesplitter)
                }

                game.selectCards(listOf(holy)).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Holy Strength") shouldBe true
                game.isInGraveyard(1, "Bonesplitter") shouldBe true
            }

            test("with no Aura or Equipment card in the graveyard, it only mills") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Dogmeat, Ever Loyal")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castDogmeat()
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.librarySize(1) shouldBe 1
                game.graveyardSize(1) shouldBe 5
                game.isOnBattlefield("Dogmeat, Ever Loyal") shouldBe true
            }
        }

        context("Dogmeat, Ever Loyal — attacks") {
            test("each enchanted or equipped attacker makes a Junk; a bare attacker doesn't") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Dogmeat, Ever Loyal", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardAttachedTo(1, "Bonesplitter", "Hill Giant")
                    .withCardOnBattlefield(1, "Raging Goblin", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(
                    mapOf("Grizzly Bears" to 2, "Hill Giant" to 2, "Raging Goblin" to 2, "Dogmeat, Ever Loyal" to 2)
                ).error shouldBe null
                game.resolveStack()

                withClue("one Junk for the enchanted Bears, one for the equipped Giant") {
                    game.junkCount() shouldBe 2
                }
            }

            test("an opponent's enchanted attacker doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Dogmeat, Ever Loyal")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(2, "Holy Strength", "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.resolveStack()

                game.junkCount() shouldBe 0
            }
        }
    }
}
