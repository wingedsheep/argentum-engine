package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.dungeon.DungeonService
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ranger's Hawk (AFR #37): a 1/1 flier whose "{3}, {T}, Tap another untapped creature you
 * control" ability ventures into the dungeon at sorcery speed. The hawk itself can't pay the
 * "another creature" part.
 */
class RangersHawkScenarioTest : ScenarioTestBase() {

    private fun board(vararg others: String) = others.fold(
        scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Ranger's Hawk", summoningSickness = false)
    ) { builder, name -> builder.withCardOnBattlefield(1, name, summoningSickness = false) }
        .withLandsOnBattlefield(1, "Plains", 3)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private val venture get() = cardRegistry.getCard("Ranger's Hawk")!!.activatedAbilities[0].id

    init {
        test("tapping the hawk and another creature ventures into the dungeon") {
            val game = board("Grizzly Bears")
            val hawk = game.findPermanent("Ranger's Hawk")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.state.projectedState.hasKeyword(hawk, Keyword.FLYING) shouldBe true

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id, sourceId = hawk, abilityId = venture,
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(bears))
                )
            ).error shouldBe null

            game.state.getEntity(hawk)!!.has<TappedComponent>() shouldBe true
            game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true

            game.resolveStack()
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
            game.submitDecision(
                OptionChosenResponse(decision.id, decision.options.indexOf("Lost Mine of Phandelver"))
            ).error shouldBe null
            game.resolveStack()

            DungeonService.marker(game.state, game.player1Id)!!.dungeon shouldBe "Lost Mine of Phandelver"
        }

        test("the hawk can't pay the 'another creature' cost with itself") {
            val game = board()
            val hawk = game.findPermanent("Ranger's Hawk")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id, sourceId = hawk, abilityId = venture,
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(hawk))
                )
            ).error shouldNotBe null

            DungeonService.marker(game.state, game.player1Id) shouldBe null
        }
    }
}
