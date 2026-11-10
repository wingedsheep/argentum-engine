package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Null Caller — {3}{B} 2/4 Vampire Shaman.
 * "{3}{B}, Exile a creature card from your graveyard: Create a tapped 2/2 black Zombie creature token."
 */
class NullCallerScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = cardRegistry.getCard("Null Caller")!!.script.activatedAbilities.single().id

        fun board(graveyardCard: String) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Null Caller")
            .withLandsOnBattlefield(1, "Swamp", 4)
            .withCardInGraveyard(1, graveyardCard)
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("exiling a creature card from your graveyard creates a tapped 2/2 black Zombie") {
            val game = board("Grizzly Bears")
            val caller = game.findPermanent("Null Caller")!!
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()

            val result = game.execute(
                ActivateAbility(
                    game.player1Id, caller, abilityId,
                    costPayment = AdditionalCostPayment(exiledCards = listOf(bears)),
                )
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isInExile(1, "Grizzly Bears") shouldBe true
            val zombies = game.findPermanents("Zombie Token")
            zombies shouldHaveSize 1
            val zombie = zombies.single()
            game.state.projectedState.getPower(zombie) shouldBe 2
            game.state.projectedState.getToughness(zombie) shouldBe 2
            game.state.projectedState.getColors(zombie) shouldBe setOf(Color.BLACK.name)
            withClue("the token enters tapped") {
                game.state.getEntity(zombie)?.has<TappedComponent>() shouldBe true
            }
            withClue("Null Caller itself doesn't tap") {
                game.state.getEntity(caller)?.has<TappedComponent>() shouldBe false
            }
        }

        test("a noncreature card can't pay the exile cost") {
            val game = board("Swamp")
            val caller = game.findPermanent("Null Caller")!!
            val swamp = game.findCardsInGraveyard(1, "Swamp").single()

            game.execute(
                ActivateAbility(
                    game.player1Id, caller, abilityId,
                    costPayment = AdditionalCostPayment(exiledCards = listOf(swamp)),
                )
            ).error shouldNotBe null
            game.findPermanents("Zombie Token") shouldHaveSize 0
        }
    }
}
