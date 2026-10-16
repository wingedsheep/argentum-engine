package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Scavenger Grounds (HOU #182).
 *
 * "{2}, {T}, Sacrifice a Desert: Exile all graveyards."
 *
 * Per the 2017-07-14 rulings, Scavenger Grounds may sacrifice itself to pay its own cost, and the
 * sacrificed Desert is then in the graveyard when the ability resolves, so it is exiled too.
 */
class ScavengerGroundsScenarioTest : ScenarioTestBase() {

    init {
        context("Scavenger Grounds") {

            test("sacrificing itself exiles every graveyard, including the sacrificed Grounds") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Scavenger Grounds")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(2, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val groundsId = game.findPermanent("Scavenger Grounds")!!
                val ability = cardRegistry.getCard("Scavenger Grounds")!!.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = groundsId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(groundsId)),
                    )
                )
                withClue("Activating by sacrificing itself should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.isOnBattlefield("Scavenger Grounds") shouldBe false
                game.isInGraveyard(1, "Scavenger Grounds") shouldBe true

                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                game.isInGraveyard(2, "Lightning Bolt") shouldBe false
                game.isInGraveyard(1, "Scavenger Grounds") shouldBe false
                game.isInExile(1, "Grizzly Bears") shouldBe true
                game.isInExile(2, "Lightning Bolt") shouldBe true
                game.isInExile(1, "Scavenger Grounds") shouldBe true
            }

            test("a non-Desert land cannot pay the Desert sacrifice") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Scavenger Grounds")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val groundsId = game.findPermanent("Scavenger Grounds")!!
                val swampId = game.findPermanents("Swamp").first()
                val ability = cardRegistry.getCard("Scavenger Grounds")!!.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = groundsId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(swampId)),
                    )
                )
                withClue("Sacrificing a Swamp must not pay \"Sacrifice a Desert\"") {
                    (result.error != null) shouldBe true
                }
                game.isOnBattlefield("Scavenger Grounds") shouldBe true
            }
        }
    }
}
