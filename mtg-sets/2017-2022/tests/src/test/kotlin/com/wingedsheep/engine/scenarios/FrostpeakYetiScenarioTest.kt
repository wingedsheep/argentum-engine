package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Frostpeak Yeti (KHM #57).
 *
 *   {1}{S}: This creature can't be blocked this turn.
 *
 * The `{S}` pip is payable only with mana from a snow source (CR 107.4h): one Snow-Covered Island
 * and one Island pay `{1}{S}` (auto-pay must spend the snow land on the `{S}`, not on the `{1}`),
 * while two plain Islands can't activate it at all.
 */
class FrostpeakYetiScenarioTest : ScenarioTestBase() {

    private val abilityId = cardRegistry.getCard("Frostpeak Yeti")!!.activatedAbilities.first().id

    private fun TestGame.activateYeti() = execute(
        ActivateAbility(playerId = player1Id, sourceId = findPermanent("Frostpeak Yeti")!!, abilityId = abilityId)
    )

    private fun TestGame.yetiUnblockable() =
        state.projectedState.hasKeyword(findPermanent("Frostpeak Yeti")!!, AbilityFlag.CANT_BE_BLOCKED)

    init {
        context("Frostpeak Yeti") {

            test("one snow land and one plain land pay {1}{S}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Frostpeak Yeti")
                    .withLandsOnBattlefield(1, "Snow-Covered Island", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("the ability is offered as affordable") {
                    game.getLegalActions(1)
                        .firstOrNull { (it.action as? ActivateAbility)?.abilityId == abilityId && it.isAffordable }
                        .shouldNotBeNull()
                }
                game.activateYeti().error shouldBe null
                game.resolveStack()

                withClue("the Yeti can't be blocked this turn") { game.yetiUnblockable() shouldBe true }
                withClue("both lands were tapped") {
                    (game.findPermanents("Snow-Covered Island") + game.findPermanents("Island"))
                        .all { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
                }
            }

            test("floating snow mana pays the {S}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Frostpeak Yeti")
                    .withLandsOnBattlefield(1, "Snow-Covered Island", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val snowIsland = game.findPermanent("Snow-Covered Island")!!
                val manaAbility = cardRegistry.getCard("Snow-Covered Island")!!.activatedAbilities.first().id
                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = snowIsland, abilityId = manaAbility))
                    .error shouldBe null

                game.activateYeti().error shouldBe null
                game.resolveStack()
                withClue("the floated snow {U} paid the {S} and the Island the {1}") {
                    game.yetiUnblockable() shouldBe true
                }
            }

            test("two plain lands can't pay the {S}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Frostpeak Yeti")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("the ability isn't affordable without a snow source") {
                    game.getLegalActions(1)
                        .filter { (it.action as? ActivateAbility)?.abilityId == abilityId }
                        .none { it.isAffordable } shouldBe true
                }
                withClue("and a hand-built activation is rejected") {
                    (game.activateYeti().error != null) shouldBe true
                }
                game.yetiUnblockable() shouldBe false
            }
        }
    }
}
