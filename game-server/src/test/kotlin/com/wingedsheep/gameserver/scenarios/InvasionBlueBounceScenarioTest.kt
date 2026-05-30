package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for four Invasion blue cards that reuse existing primitives:
 *
 * - Breaking Wave ({2}{U}{U}) — "Simultaneously untap all tapped creatures and tap all
 *   untapped creatures." Verifies the gather-both-then-swap composition is atomic (no
 *   double flip).
 * - Wash Out ({3}{U}) — "Return all permanents of the color of your choice to their
 *   owners' hands." Verifies ChooseColorThen + gather(HasChosenColor) + move-to-hand.
 * - Tidal Visionary ({U}) — "{T}: Target creature becomes the color of your choice until
 *   end of turn." Verifies the single-color recolor via projected colors.
 * - Distorting Wake ({X}{U}{U}{U}) — "Return X target nonland permanents to their owners'
 *   hands." Verifies X-clamped multi-target bounce.
 */
class InvasionBlueBounceScenarioTest : ScenarioTestBase() {

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.has<TappedComponent>() == true

    // Mono-colored vanilla creatures so color/tap checks are unambiguous.
    private val greenBear = CardDefinition.creature(
        name = "Green Bear", manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Bear")), power = 2, toughness = 2
    )
    private val redOgre = CardDefinition.creature(
        name = "Red Ogre", manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Ogre")), power = 3, toughness = 3
    )

    init {
        cardRegistry.register(greenBear)
        cardRegistry.register(redOgre)

        context("Breaking Wave") {
            test("simultaneously untaps tapped creatures and taps untapped creatures") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Breaking Wave")
                    .withCardOnBattlefield(1, "Green Bear", tapped = true)  // should untap
                    .withCardOnBattlefield(1, "Red Ogre", tapped = false)   // should tap
                    .withCardOnBattlefield(2, "Green Bear", tapped = false) // should tap
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tappedBear = game.state.getBattlefield(game.player1Id)
                    .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Green Bear" }
                val untappedOgre = game.findPermanent("Red Ogre")!!
                val oppBear = game.state.getBattlefield(game.player2Id)
                    .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Green Bear" }

                game.castSpell(1, "Breaking Wave")
                game.resolveStack()

                withClue("Originally tapped creature should be untapped") {
                    isTapped(game, tappedBear) shouldBe false
                }
                withClue("Originally untapped creature should be tapped (not re-untapped)") {
                    isTapped(game, untappedOgre) shouldBe true
                }
                withClue("Opponent's untapped creature should also be tapped") {
                    isTapped(game, oppBear) shouldBe true
                }
            }
        }

        context("Wash Out") {
            test("returns all permanents of the chosen color to owners' hands") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Wash Out")
                    .withCardOnBattlefield(1, "Green Bear")
                    .withCardOnBattlefield(2, "Green Bear")
                    .withCardOnBattlefield(1, "Red Ogre")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Wash Out")
                game.resolveStack()

                val colorDecision = game.getPendingDecision()
                withClue("Should pause for a color choice") {
                    (colorDecision is ChooseColorDecision) shouldBe true
                }
                game.submitDecision(ColorChosenResponse(colorDecision!!.id, Color.GREEN))
                game.resolveStack()

                withClue("All green creatures bounced to hand") {
                    game.isOnBattlefield("Green Bear") shouldBe false
                }
                withClue("Each owner gets their green creature back") {
                    game.state.getHand(game.player1Id).count {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Green Bear"
                    } shouldBe 1
                    game.state.getHand(game.player2Id).count {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Green Bear"
                    } shouldBe 1
                }
                withClue("Red permanent of a different color is untouched") {
                    game.isOnBattlefield("Red Ogre") shouldBe true
                }
            }
        }

        context("Tidal Visionary") {
            test("recolors target creature to the chosen color") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Tidal Visionary")
                    .withCardOnBattlefield(2, "Green Bear")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val visionary = game.findPermanent("Tidal Visionary")!!
                val bear = game.findPermanent("Green Bear")!!

                withClue("Bear starts green") {
                    game.state.projectedState.getColors(bear) shouldBe setOf("GREEN")
                }

                val ability = cardRegistry.getCard("Tidal Visionary")!!.script.activatedAbilities[0]
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = visionary,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(bear))
                    )
                )
                withClue("Ability should activate: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                val colorDecision = game.getPendingDecision()
                withClue("Should pause for a color choice") {
                    (colorDecision is ChooseColorDecision) shouldBe true
                }
                game.submitDecision(ColorChosenResponse(colorDecision!!.id, Color.BLUE))
                game.resolveStack()

                withClue("Bear should now be blue") {
                    game.state.projectedState.getColors(bear) shouldBe setOf("BLUE")
                }
            }
        }

        context("Distorting Wake") {
            test("returns X target nonland permanents to owners' hands") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Distorting Wake")
                    .withCardOnBattlefield(1, "Green Bear")
                    .withCardOnBattlefield(2, "Red Ogre")
                    .withLandsOnBattlefield(1, "Island", 5) // X=2 → {2}{U}{U}{U}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Green Bear")!!
                val ogre = game.findPermanent("Red Ogre")!!
                val wakeId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Distorting Wake"
                }

                val result = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = wakeId,
                        targets = listOf(
                            ChosenTarget.Permanent(bear),
                            ChosenTarget.Permanent(ogre)
                        ),
                        xValue = 2
                    )
                )
                withClue("Cast should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("Both targeted permanents bounced") {
                    game.isOnBattlefield("Green Bear") shouldBe false
                    game.isOnBattlefield("Red Ogre") shouldBe false
                }
                withClue("Each owner gets their permanent back") {
                    game.state.getHand(game.player1Id).count {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Green Bear"
                    } shouldBe 1
                    game.state.getHand(game.player2Id).count {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Red Ogre"
                    } shouldBe 1
                }
            }
        }
    }
}
