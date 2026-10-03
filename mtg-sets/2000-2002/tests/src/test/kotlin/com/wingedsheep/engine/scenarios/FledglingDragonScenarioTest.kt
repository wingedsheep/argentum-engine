package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.jud.cards.FledglingDragon
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fledgling Dragon (JUD #90) — Flying; threshold: +3/+3 and "{R}: +1/+0 until end of turn".
 *
 * Both threshold halves share one gate, so the tests prove the 2/2 flier below threshold (no
 * firebreathing available) and the 5/5 firebreather at seven cards in the graveyard.
 */
class FledglingDragonScenarioTest : ScenarioTestBase() {

    private val firebreathing = FledglingDragon.staticAbilities
        .filterIsInstance<ConditionalStaticAbility>()
        .mapNotNull { it.ability as? GrantActivatedAbility }
        .single()
        .ability.id

    private fun builder(graveyardCards: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Fledgling Dragon")
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .apply { repeat(graveyardCards) { withCardInGraveyard(1, "Mountain") } }

    init {
        context("Fledgling Dragon") {

            test("below threshold it is a 2/2 flier with no firebreathing") {
                val game = builder(6).build()
                val dragon = game.findPermanent("Fledgling Dragon")!!
                val projected = game.state.projectedState

                projected.getPower(dragon) shouldBe 2
                projected.getToughness(dragon) shouldBe 2
                projected.hasKeyword(dragon, Keyword.FLYING) shouldBe true

                val activation = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = dragon, abilityId = firebreathing)
                )
                withClue("the granted ability is gated off below threshold") { activation.error shouldNotBe null }
            }

            test("with threshold it is a 5/5 that can pump +1/+0 per {R}") {
                val game = builder(7).build()
                val dragon = game.findPermanent("Fledgling Dragon")!!

                game.state.projectedState.getPower(dragon) shouldBe 5
                game.state.projectedState.getToughness(dragon) shouldBe 5

                repeat(2) {
                    val activation = game.execute(
                        ActivateAbility(playerId = game.player1Id, sourceId = dragon, abilityId = firebreathing)
                    )
                    withClue("activation should succeed: ${activation.error}") { activation.error shouldBe null }
                    game.resolveStack()
                }

                game.state.projectedState.getPower(dragon) shouldBe 7
                game.state.projectedState.getToughness(dragon) shouldBe 5
            }
        }
    }
}
