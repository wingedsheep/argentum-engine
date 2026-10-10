package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for The Water Crystal (FIN #85).
 *
 * {2}{U}{U} Legendary Artifact
 *  - Blue spells you cast cost {1} less to cast.
 *  - If an opponent would mill one or more cards, they mill that many cards plus four instead.
 *  - {4}{U}{U}, {T}: Each opponent mills cards equal to the number of cards in your hand.
 *
 * Exercises the new [com.wingedsheep.sdk.scripting.ModifyKeywordActionAmount] mill-amount replacement
 * (the +4 boost on opponent mills, including stacking and the "would mill one or more" gate) and
 * the activated ability (each opponent mills equal to *your* hand size). The {1}-less cost
 * reduction reuses the existing ModifySpellCost primitive (covered by The Wind Crystal), so it
 * isn't re-exercised here.
 */
class TheWaterCrystalScenarioTest : ScenarioTestBase() {

    init {
        context("The Water Crystal") {

            test("opponent who would mill 2 mills 6 instead (Millstone + the +4 replacement)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "The Water Crystal")
                    // Millstone targets the opponent to mill exactly two.
                    .withCardOnBattlefield(1, "Millstone")
                    .withLandsOnBattlefield(1, "Island", 2) // pays Millstone's {2}
                    // Opponent's library: enough to absorb 2 + 4 = 6.
                    .also { b -> repeat(8) { b.withCardInLibrary(2, "Forest") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val millstoneId = game.findPermanent("Millstone")!!
                val ability = cardRegistry.getCard("Millstone")!!.script.activatedAbilities[0]

                val libBefore = game.librarySize(2)
                val graveBefore = game.graveyardSize(2)

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = millstoneId,
                        abilityId = ability.id,
                        targets = listOf(entityIdToChosenTarget(game.state, game.player2Id)),
                    )
                )
                withClue("Activating Millstone should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Millstone's 2-card mill is boosted to 6 by The Water Crystal") {
                    game.graveyardSize(2) shouldBe graveBefore + 6
                    game.librarySize(2) shouldBe libBefore - 6
                }
            }

            test("the +4 boost does NOT apply to the controller's own mills") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "The Water Crystal")
                    // Millstone targets the controller (player 1) — the replacement is opponent-only.
                    .withCardOnBattlefield(1, "Millstone")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .also { b -> repeat(8) { b.withCardInLibrary(1, "Plains") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val millstoneId = game.findPermanent("Millstone")!!
                val ability = cardRegistry.getCard("Millstone")!!.script.activatedAbilities[0]

                val graveBefore = game.graveyardSize(1)

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = millstoneId,
                        abilityId = ability.id,
                        targets = listOf(entityIdToChosenTarget(game.state, game.player1Id)),
                    )
                )
                withClue("Activating Millstone should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Your own mill is unmodified — only opponents get the +4") {
                    game.graveyardSize(1) shouldBe graveBefore + 2
                }
            }

            test("two copies of The Water Crystal stack: opponent's 2-card mill becomes 2 + 4 + 4 = 10") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    // Two crystals, each contributing its own +4 replacement (Scryfall ruling).
                    // The Water Crystal is legendary: without the waiver the legend rule (CR 704.5j)
                    // would remove one copy the moment a player next gets priority.
                    .withCardOnBattlefield(1, "Legend Rule Waiver")
                    .withCardOnBattlefield(1, "The Water Crystal")
                    .withCardOnBattlefield(1, "The Water Crystal")
                    .withCardOnBattlefield(1, "Millstone")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .also { b -> repeat(15) { b.withCardInLibrary(2, "Forest") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.findPermanents("The Water Crystal").size shouldBe 2

                val millstoneId = game.findPermanent("Millstone")!!
                val ability = cardRegistry.getCard("Millstone")!!.script.activatedAbilities[0]

                val graveBefore = game.graveyardSize(2)
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = millstoneId,
                        abilityId = ability.id,
                        targets = listOf(entityIdToChosenTarget(game.state, game.player2Id)),
                    )
                )
                result.error shouldBe null
                game.resolveStack()

                withClue("Two crystals add 4 each: 2 + 4 + 4 = 10") {
                    game.graveyardSize(2) shouldBe graveBefore + 10
                }
            }

            test("{4}{U}{U}, {T}: each opponent mills equal to your hand size (3 in hand -> 7 milled with the +4)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "The Water Crystal", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 6) // pays {4}{U}{U}
                    // Three cards in the controller's hand → base mill of 3.
                    .withCardInHand(1, "Island")
                    .withCardInHand(1, "Island")
                    .withCardInHand(1, "Island")
                    .also { b -> repeat(12) { b.withCardInLibrary(2, "Forest") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crystal = game.findPermanent("The Water Crystal")!!
                val abilityId = cardRegistry.getCard("The Water Crystal")!!
                    .script.activatedAbilities[0].id

                val graveBefore = game.graveyardSize(2)

                val activate = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crystal,
                        abilityId = abilityId,
                    )
                )
                withClue("Activating The Water Crystal should succeed: ${activate.error}") {
                    activate.error shouldBe null
                }
                if (game.getPendingDecision() is com.wingedsheep.engine.core.SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                withClue("Base mill = your hand size (3), boosted by the opponent +4 to 7") {
                    game.graveyardSize(2) shouldBe graveBefore + 7
                }
            }
        }
    }
}
