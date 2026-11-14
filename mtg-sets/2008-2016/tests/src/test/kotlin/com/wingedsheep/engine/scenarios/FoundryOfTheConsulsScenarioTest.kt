package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ori.cards.FoundryOfTheConsuls
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Foundry of the Consuls (ORI #247) — Land.
 *
 *   {T}: Add {C}.
 *   {5}, {T}, Sacrifice this land: Create two 1/1 colorless Thopter artifact creature tokens with
 *   flying.
 */
class FoundryOfTheConsulsScenarioTest : ScenarioTestBase() {

    private val manaAbilityId = FoundryOfTheConsuls.activatedAbilities[0].id
    private val thopterAbilityId = FoundryOfTheConsuls.activatedAbilities[1].id

    init {
        context("Foundry of the Consuls") {

            test("taps for one colorless mana") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Foundry of the Consuls")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val foundry = game.findPermanent("Foundry of the Consuls")!!
                val result = game.execute(ActivateAbility(game.player1Id, foundry, manaAbilityId))
                withClue("mana ability should activate: ${result.error}") { result.error shouldBe null }

                val pool = game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!
                withClue("one colorless mana in the pool") {
                    pool.colorless shouldBe 1
                    pool.total shouldBe 1
                }
            }

            test("{5}, {T}, sacrifice: creates two 1/1 flying Thopters and the Foundry goes to the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Foundry of the Consuls")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val foundry = game.findPermanent("Foundry of the Consuls")!!
                val result = game.execute(ActivateAbility(game.player1Id, foundry, thopterAbilityId))
                withClue("token ability should activate: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("the Foundry was sacrificed") {
                    game.isOnBattlefield("Foundry of the Consuls") shouldBe false
                    game.isInGraveyard(1, "Foundry of the Consuls") shouldBe true
                }
                val thopters = game.findPermanents("Thopter Token")
                withClue("two Thopter tokens were created") { thopters.size shouldBe 2 }
                val projected = game.state.projectedState
                thopters.forEach { thopter ->
                    withClue("each Thopter is a 1/1 flying artifact creature") {
                        projected.getPower(thopter) shouldBe 1
                        projected.getToughness(thopter) shouldBe 1
                        projected.hasKeyword(thopter, Keyword.FLYING) shouldBe true
                        projected.hasType(thopter, "ARTIFACT") shouldBe true
                        projected.isCreature(thopter) shouldBe true
                    }
                }
            }
        }
    }
}
