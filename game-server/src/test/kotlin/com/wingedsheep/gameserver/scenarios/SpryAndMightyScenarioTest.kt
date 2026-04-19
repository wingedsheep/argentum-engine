package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Spry and Mighty.
 *
 * Spry and Mighty: {4}{G}
 * Sorcery
 * Choose exactly two creatures you control. You draw X cards and the chosen creatures
 * get +X/+X and gain trample until end of turn, where X is the difference between the
 * chosen creatures' powers.
 */
class SpryAndMightyScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private val onePower = CardDefinition.creature(
        name = "Test 1/1",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Elf")),
        power = 1, toughness = 1
    )
    private val fourPower = CardDefinition.creature(
        name = "Test 4/4",
        manaCost = ManaCost.parse("{3}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 4, toughness = 4
    )
    private val samePower = CardDefinition.creature(
        name = "Test 4/2",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 4, toughness = 2
    )

    init {
        cardRegistry.register(onePower)
        cardRegistry.register(fourPower)
        cardRegistry.register(samePower)

        context("Spry and Mighty") {
            test("draws X, pumps both, and grants trample where X = |p1 - p2|") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Spry and Mighty")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardOnBattlefield(1, "Test 1/1", summoningSickness = false)
                    .withCardOnBattlefield(1, "Test 4/4", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val small = game.findPermanent("Test 1/1")!!
                val big = game.findPermanent("Test 4/4")!!
                val handBefore = game.handSize(1)

                val result = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.findCardsInHand(1, "Spry and Mighty").first(),
                        listOf(ChosenTarget.Permanent(small), ChosenTarget.Permanent(big))
                    )
                )
                withClue("Spry and Mighty should cast: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                // X = |1 - 4| = 3
                withClue("Caster should draw 3 cards (X = 3)") {
                    game.handSize(1) shouldBe handBefore - 1 + 3
                }

                val projected = projector.project(game.state)
                withClue("Small creature should be 4/4 (1/1 + 3/3)") {
                    projected.getPower(small) shouldBe 4
                    projected.getToughness(small) shouldBe 4
                }
                withClue("Big creature should be 7/7 (4/4 + 3/3)") {
                    projected.getPower(big) shouldBe 7
                    projected.getToughness(big) shouldBe 7
                }
                withClue("Both chosen creatures should gain trample") {
                    projected.hasKeyword(small, Keyword.TRAMPLE) shouldBe true
                    projected.hasKeyword(big, Keyword.TRAMPLE) shouldBe true
                }
            }

            test("draws zero and pumps zero when chosen creatures have equal power") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Spry and Mighty")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardOnBattlefield(1, "Test 4/4", summoningSickness = false)
                    .withCardOnBattlefield(1, "Test 4/2", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val fourFour = game.findPermanent("Test 4/4")!!
                val fourTwo = game.findPermanent("Test 4/2")!!
                val handBefore = game.handSize(1)

                val result = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.findCardsInHand(1, "Spry and Mighty").first(),
                        listOf(ChosenTarget.Permanent(fourFour), ChosenTarget.Permanent(fourTwo))
                    )
                )
                withClue("Spry and Mighty should cast: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                // X = |4 - 4| = 0 — no draw, no pump, but trample is still granted
                withClue("Caster should not draw (Spry and Mighty leaves hand, no X cards)") {
                    game.handSize(1) shouldBe handBefore - 1
                }

                val projected = projector.project(game.state)
                withClue("Test 4/4 remains 4/4") {
                    projected.getPower(fourFour) shouldBe 4
                    projected.getToughness(fourFour) shouldBe 4
                }
                withClue("Test 4/2 remains 4/2") {
                    projected.getPower(fourTwo) shouldBe 4
                    projected.getToughness(fourTwo) shouldBe 2
                }
                withClue("Both chosen creatures still gain trample") {
                    projected.hasKeyword(fourFour, Keyword.TRAMPLE) shouldBe true
                    projected.hasKeyword(fourTwo, Keyword.TRAMPLE) shouldBe true
                }
            }

            test("requires exactly two target creatures") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Spry and Mighty")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardOnBattlefield(1, "Test 1/1", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val small = game.findPermanent("Test 1/1")!!
                val result = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.findCardsInHand(1, "Spry and Mighty").first(),
                        listOf(ChosenTarget.Permanent(small))
                    )
                )
                withClue("Should fail with only one target") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
