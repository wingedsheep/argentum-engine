package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Eldrazi Ravager (MH3 #5) — {5}{C} Creature — Eldrazi 6/6
 *
 *   Annihilator 1
 *   Sacrifice two Eldrazi: Return this card from your graveyard to your hand.
 *   Cycling {2}
 */
class EldraziRavagerScenarioTest : ScenarioTestBase() {

    init {
        context("Eldrazi Ravager") {

            test("attacking makes the defending player sacrifice a permanent of their choice") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Eldrazi Ravager", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Eldrazi Ravager" to 2)).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the defending player chooses") { decision.playerId shouldBe game.player2Id }
                withClue("any permanent is a legal sacrifice") { decision.options.size shouldBe 2 }

                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Forest") shouldBe true
            }

            test("sacrificing two Eldrazi returns it from the graveyard to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInGraveyard(1, "Eldrazi Ravager")
                    .withCardOnBattlefield(1, "Nulldrifter", summoningSickness = false)
                    .withCardOnBattlefield(1, "Artisan of Kozilek", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ravager = game.findCardsInGraveyard(1, "Eldrazi Ravager").single()
                val eldrazi = listOf(
                    game.findPermanent("Nulldrifter")!!,
                    game.findPermanent("Artisan of Kozilek")!!
                )
                val abilityId = cardRegistry.getCard("Eldrazi Ravager")!!.activatedAbilities[0].id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ravager,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = eldrazi),
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                withClue("both Eldrazi were sacrificed as the cost") {
                    game.isInGraveyard(1, "Nulldrifter") shouldBe true
                    game.isInGraveyard(1, "Artisan of Kozilek") shouldBe true
                }
                game.resolveStack()

                game.isInHand(1, "Eldrazi Ravager") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("a non-Eldrazi can't pay the graveyard cost") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInGraveyard(1, "Eldrazi Ravager")
                    .withCardOnBattlefield(1, "Nulldrifter", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ravager = game.findCardsInGraveyard(1, "Eldrazi Ravager").single()
                val abilityId = cardRegistry.getCard("Eldrazi Ravager")!!.activatedAbilities[0].id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ravager,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(
                                game.findPermanent("Nulldrifter")!!,
                                game.findPermanent("Grizzly Bears")!!
                            )
                        ),
                    )
                )
                result.error shouldNotBe null
                game.isInGraveyard(1, "Eldrazi Ravager") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
