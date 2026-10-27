package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class GlitteringFrostScenarioTest : ScenarioTestBase() {

    private fun board(withTroll: Boolean = false) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Forest")
        .withCardAttachedTo(1, "Glittering Frost", "Forest")
        .apply { if (withTroll) withCardOnBattlefield(1, "Icehide Troll") }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("enchanted land is snow") {
            val game = board()
            game.state.projectedState.isSnow(game.findPermanent("Forest")!!) shouldBe true
        }

        test("tapping the land adds its {G} plus one mana of any color, all of it snow") {
            val game = board()
            val forest = game.findPermanent("Forest")!!
            val manaAbility = cardRegistry.getCard("Forest")!!.activatedAbilities.first { it.isManaAbility }.id
            game.execute(ActivateAbility(game.player1Id, forest, manaAbility)).error shouldBe null

            val decision = game.getPendingDecision() as? ChooseColorDecision
            withClue("the additional mana asks for its color") { decision shouldNotBe null }
            game.submitDecision(ColorChosenResponse(decision!!.id, Color.BLUE)).error shouldBe null

            val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
            withClue("pool was $pool") {
                pool.green shouldBe 1
                pool.blue shouldBe 1
                pool.snowTotal shouldBe 2
            }
        }

        test("one enchanted land pays Icehide Troll's {S}{S}") {
            val game = board(withTroll = true)
            val troll = game.findPermanent("Icehide Troll")!!
            val ability = cardRegistry.getCard("Icehide Troll")!!.activatedAbilities.first().id
            game.execute(ActivateAbility(game.player1Id, troll, ability)).error shouldBe null
            game.resolveStack()

            withClue("the Forest was tapped once for both snow mana") {
                game.state.getEntity(game.findPermanent("Forest")!!)?.has<TappedComponent>() shouldBe true
            }
            game.state.projectedState.getPower(troll) shouldBe 4
            game.state.projectedState.hasKeyword(troll, Keyword.INDESTRUCTIBLE) shouldBe true
        }
    }
}
