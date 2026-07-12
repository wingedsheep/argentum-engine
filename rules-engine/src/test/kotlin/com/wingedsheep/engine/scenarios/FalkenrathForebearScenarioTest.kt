package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Falkenrath Forebear (VOW #334) — {2}{B} Creature — Vampire, 3/1, Flying.
 *
 *   Flying
 *   This creature can't block.
 *   Whenever this creature deals combat damage to a player, create a Blood token.
 *   {B}, Sacrifice two Blood tokens: Return this card from your graveyard to the battlefield.
 *
 * Exercises the printed Flying/can't-block statics, the combat-damage-to-player -> Blood token
 * trigger, and the graveyard-activated recursion ability paid with two sacrificed Blood tokens.
 */
class FalkenrathForebearScenarioTest : ScenarioTestBase() {

    init {
        context("Falkenrath Forebear") {

            test("has flying and can't block") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Falkenrath Forebear", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forebear = game.findPermanent("Falkenrath Forebear")!!

                withClue("Falkenrath Forebear has flying") {
                    game.state.projectedState.hasKeyword(forebear, Keyword.FLYING) shouldBe true
                }
                withClue("Falkenrath Forebear can't block") {
                    game.state.projectedState.cantBlock(forebear) shouldBe true
                }
            }

            test("dealing combat damage to a player creates a Blood token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Falkenrath Forebear", summoningSickness = false)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Falkenrath Forebear" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("the opponent takes 3 combat damage (20 -> 17)") {
                    game.getLifeTotal(2) shouldBe 17
                }
                withClue("a Blood token was created") {
                    game.findPermanents("Blood").size shouldBe 1
                }
            }

            test("{B}, Sacrifice two Blood tokens returns it from the graveyard to the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Falkenrath Forebear")
                    .withCardOnBattlefield(1, "Blood", isToken = true)
                    .withCardOnBattlefield(1, "Blood", isToken = true)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Starts in the graveyard, not on the battlefield") {
                    game.isInGraveyard(1, "Falkenrath Forebear") shouldBe true
                    game.isOnBattlefield("Falkenrath Forebear") shouldBe false
                }

                val bloodTokens = game.findPermanents("Blood")
                withClue("Two Blood tokens are on the battlefield to sacrifice") {
                    bloodTokens.size shouldBe 2
                }

                val graveyardCard = game.state.getGraveyard(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Falkenrath Forebear"
                }
                val abilityId = cardRegistry.getCard("Falkenrath Forebear")!!
                    .activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = graveyardCard,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = bloodTokens),
                    )
                )
                withClue("Activating the {B}, sacrifice two Blood tokens ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                if (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }

                withClue("Both Blood tokens were sacrificed as a cost") {
                    game.findPermanents("Blood").size shouldBe 0
                }

                game.resolveStack()

                withClue("Falkenrath Forebear is back on the battlefield") {
                    game.isOnBattlefield("Falkenrath Forebear") shouldBe true
                }
                withClue("Falkenrath Forebear is no longer in the graveyard") {
                    game.isInGraveyard(1, "Falkenrath Forebear") shouldBe false
                }
            }
        }
    }
}
