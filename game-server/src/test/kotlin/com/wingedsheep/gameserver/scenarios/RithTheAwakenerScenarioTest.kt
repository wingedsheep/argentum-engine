package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Rith, the Awakener.
 *
 * Card reference:
 * - Rith, the Awakener ({3}{R}{G}{W}): 6/6 Legendary Creature — Dragon, Flying
 *   "Whenever Rith deals combat damage to a player, you may pay {2}{G}. If you do, choose a color,
 *    then create a 1/1 green Saproling creature token for each permanent of that color."
 *
 * Exercises the new `CardPredicate.HasChosenColor` filter: the token count is an
 * `AggregateBattlefield(Player.Each)` over the chosen-color filter, so it must count every
 * permanent of that color across both players and ignore permanents of other colors.
 */
class RithTheAwakenerScenarioTest : ScenarioTestBase() {

    // Mono-colored vanilla creatures so the chosen-color count is unambiguous.
    private val greenBear = CardDefinition.creature(
        name = "Green Bear", manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Bear")), power = 2, toughness = 2
    )
    private val greenOgre = CardDefinition.creature(
        name = "Green Ogre", manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Ogre")), power = 3, toughness = 3
    )
    private val blueDrake = CardDefinition.creature(
        name = "Blue Drake", manaCost = ManaCost.parse("{U}"),
        subtypes = setOf(Subtype("Drake")), power = 2, toughness = 2
    )

    init {
        cardRegistry.register(greenBear)
        cardRegistry.register(greenOgre)
        cardRegistry.register(blueDrake)

        context("Rith combat-damage trigger") {

            /**
             * Builds a board where the green permanents are: Rith (G/R/W), Green Bear (P1),
             * Green Ogre (P2) — three total. Blue permanents: Blue Drake (P2) — one total.
             * Basic Forests are colorless and never counted.
             */
            fun freshGame() = scenario()
                .withPlayers("Attacker", "Defender")
                .withCardOnBattlefield(1, "Rith, the Awakener")
                .withCardOnBattlefield(1, "Green Bear")
                .withCardOnBattlefield(2, "Green Ogre")
                .withCardOnBattlefield(2, "Blue Drake")
                .withLandsOnBattlefield(1, "Forest", 3) // pays {2}{G}
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun dealCombatDamageWithRith(game: TestGame) {
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Rith, the Awakener" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }
            }

            test("paying and choosing green creates a Saproling per green permanent") {
                val game = freshGame()
                dealCombatDamageWithRith(game)

                withClue("Rith's combat-damage trigger should ask whether to pay {2}{G}") {
                    game.hasPendingDecision() shouldBe true
                }

                // "Pay {2}{G}?" → yes, then auto-tap the three Forests.
                game.answerYesNo(true)
                game.submitManaSourcesAutoPay()

                // "Choose a color" → green.
                val colorDecision = game.getPendingDecision()
                withClue("Should pause for a color choice after paying") {
                    (colorDecision is ChooseColorDecision) shouldBe true
                }
                game.submitDecision(ColorChosenResponse(colorDecision!!.id, Color.GREEN))

                // Rith + Green Bear + Green Ogre = 3 green permanents → 3 Saprolings.
                withClue("Should create one Saproling per green permanent on the battlefield") {
                    game.findAllPermanents("Saproling Token").size shouldBe 3
                }
                withClue("Defender took 6 combat damage from Rith") {
                    game.getLifeTotal(2) shouldBe 14
                }
            }

            test("choosing blue only counts blue permanents (Rith is not blue)") {
                val game = freshGame()
                dealCombatDamageWithRith(game)

                game.answerYesNo(true)
                game.submitManaSourcesAutoPay()

                val colorDecision = game.getPendingDecision()
                game.submitDecision(ColorChosenResponse(colorDecision!!.id, Color.BLUE))

                // Only Blue Drake is blue — Rith (G/R/W) must not be counted.
                withClue("Only the single blue permanent should produce a Saproling") {
                    game.findAllPermanents("Saproling Token").size shouldBe 1
                }
            }

            test("declining the payment creates no tokens") {
                val game = freshGame()
                dealCombatDamageWithRith(game)

                game.answerYesNo(false)

                withClue("Declining {2}{G} should create no Saprolings") {
                    game.findAllPermanents("Saproling Token").size shouldBe 0
                }
                withClue("Defender still took 6 combat damage") {
                    game.getLifeTotal(2) shouldBe 14
                }
            }
        }
    }
}
