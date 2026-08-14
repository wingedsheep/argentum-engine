package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gollum the Abandoned (HOB) — {1}{B} Legendary Creature — Halfling Horror 2/2
 *
 * Gollum can't block.
 * When Gollum enters, exile up to one target card from an opponent's graveyard. Each opponent
 * loses 2 life.
 * {2}, Sacrifice an artifact or creature: Return this card from your graveyard to your hand.
 * Activate only as a sorcery.
 *
 * The two shapes worth proving: the ETB still drains with no target chosen ("up to one"), and the
 * recursion ability is enumerable and payable *from the graveyard*.
 */
class GollumTheAbandonedScenarioTest : ScenarioTestBase() {

    init {
        context("Gollum the Abandoned") {

            test("ETB exiles the targeted opposing graveyard card and drains 2") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Gollum the Abandoned")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()
                val lifeBefore = game.getLifeTotal(2)

                game.castSpell(1, "Gollum the Abandoned").error shouldBe null
                game.resolveStack()

                // The ETB trigger targets as it goes on the stack.
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(bears))
                }
                game.resolveStack()

                withClue("The targeted card left the opponent's graveyard for exile") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                }
                withClue("Each opponent lost 2 life") {
                    game.getLifeTotal(2) shouldBe lifeBefore - 2
                }
            }

            test("ETB still drains when there is nothing to exile") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Gollum the Abandoned")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lifeBefore = game.getLifeTotal(2)

                game.castSpell(1, "Gollum the Abandoned").error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.skipTargets()
                }
                game.resolveStack()

                withClue("\"Up to one target\" means an empty opposing graveyard is fine") {
                    game.getLifeTotal(2) shouldBe lifeBefore - 2
                }
                withClue("Gollum is on the battlefield") {
                    game.isOnBattlefield("Gollum the Abandoned") shouldBe true
                }
            }

            test("{2}, Sacrifice an artifact or creature returns Gollum from the graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Gollum the Abandoned")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gollum = game.findCardsInGraveyard(1, "Gollum the Abandoned").single()
                val fodder = game.findPermanent("Grizzly Bears")!!
                val ability = cardRegistry.getCard("Gollum the Abandoned")!!
                    .script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = gollum,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder))
                    )
                )
                withClue("The graveyard ability should activate: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("The sacrificed creature is gone") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
                withClue("Gollum is back in hand") {
                    game.isInHand(1, "Gollum the Abandoned") shouldBe true
                    game.isInGraveyard(1, "Gollum the Abandoned") shouldBe false
                }
            }

            test("the graveyard ability can't be activated at instant speed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Gollum the Abandoned")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                    .build()

                val gollum = game.findCardsInGraveyard(1, "Gollum the Abandoned").single()
                val fodder = game.findPermanent("Grizzly Bears")!!
                val ability = cardRegistry.getCard("Gollum the Abandoned")!!
                    .script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = gollum,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder))
                    )
                )
                withClue("\"Activate only as a sorcery\" blocks it outside a main phase") {
                    (result.error != null) shouldBe true
                }
            }

            test("Gollum can't block") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Gollum the Abandoned")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null

                val result = game.declareBlockers(
                    mapOf("Gollum the Abandoned" to listOf("Grizzly Bears"))
                )
                withClue("The can't-block static must reject the block") {
                    (result.error != null) shouldBe true
                }
            }
        }
    }
}
