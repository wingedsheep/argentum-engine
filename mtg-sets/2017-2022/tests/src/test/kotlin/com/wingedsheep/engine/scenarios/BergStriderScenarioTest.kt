package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Berg Strider (KHM #47).
 *
 *   When this creature enters, tap target artifact or creature an opponent controls. If {S} was
 *   spent to cast this spell, that permanent doesn't untap during its controller's next untap step.
 *
 * "{S} was spent" means any mana from a snow source went into the cost (CR 107.4h) — Berg Strider
 * has no {S} pip — so one Snow-Covered Island among five lands is enough, whether it is tapped by
 * auto-pay or floated beforehand. Five plain Islands only tap the target.
 */
class BergStriderScenarioTest : ScenarioTestBase() {

    private fun TestGame.isTapped(id: EntityId) = state.getEntity(id)?.has<TappedComponent>() == true

    /** Cast Berg Strider, aim the enters trigger at the opponent's Hill Giant, resolve everything. */
    private fun TestGame.castAtGiant(): EntityId {
        val giant = findPermanent("Hill Giant")!!
        castSpell(1, "Berg Strider").error shouldBe null
        resolveStack()
        if (hasPendingDecision()) selectTargets(listOf(giant))
        resolveStack()
        return giant
    }

    /** From player 1's main phase into player 2's upkeep, past player 2's untap step. */
    private fun TestGame.toOpponentsUpkeep() {
        passUntilPhase(Phase.ENDING, Step.END)
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        state.activePlayerId shouldBe player2Id
    }

    init {
        context("Berg Strider") {

            test("snow mana spent on the generic part keeps the target tapped through its untap step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Berg Strider")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(1, "Snow-Covered Island", 1)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.castAtGiant()
                withClue("the enters trigger taps the target") { game.isTapped(giant) shouldBe true }

                game.toOpponentsUpkeep()
                withClue("{S} was spent, so the giant skips its controller's next untap step") {
                    game.isTapped(giant) shouldBe true
                }
            }

            test("snow mana floated before casting counts as {S} spent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Berg Strider")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(1, "Snow-Covered Island", 1)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val snowIsland = game.findPermanent("Snow-Covered Island")!!
                val manaAbility = cardRegistry.getCard("Snow-Covered Island")!!.activatedAbilities.first().id
                game.execute(
                    com.wingedsheep.engine.core.ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = snowIsland,
                        abilityId = manaAbility,
                    )
                ).error shouldBe null

                val giant = game.castAtGiant()
                game.toOpponentsUpkeep()
                withClue("the floated snow {U} paid part of the cost, so the giant stays tapped") {
                    game.isTapped(giant) shouldBe true
                }
            }

            test("without snow mana the target is only tapped and untaps normally") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Berg Strider")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.castAtGiant()
                withClue("the enters trigger still taps the target") { game.isTapped(giant) shouldBe true }

                game.toOpponentsUpkeep()
                withClue("no {S} was spent, so the giant untaps as usual") {
                    game.isTapped(giant) shouldBe false
                }
            }
        }
    }
}
