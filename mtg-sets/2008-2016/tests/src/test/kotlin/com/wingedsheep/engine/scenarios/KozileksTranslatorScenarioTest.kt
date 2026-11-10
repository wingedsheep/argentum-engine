package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kozilek's Translator — {4}{B} 3/5 Eldrazi Drone. "Devoid. Pay 1 life: Add {C}. Activate only once
 * each turn."
 */
class KozileksTranslatorScenarioTest : ScenarioTestBase() {
    init {
        val manaId = cardRegistry.getCard("Kozilek's Translator")!!.script.activatedAbilities.single().id

        test("pay 1 life for {C}, only once each turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Kozilek's Translator")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val translator = game.findPermanent("Kozilek's Translator")!!
            game.state.projectedState.getColors(translator) shouldBe emptySet()

            val first = game.execute(ActivateAbility(game.player1Id, translator, manaId))
            withClue("first activation should succeed: ${first.error}") { first.error shouldBe null }
            withClue("a mana ability doesn't use the stack") { game.state.stack.size shouldBe 0 }
            game.getLifeTotal(1) shouldBe 19
            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.colorless shouldBe 1

            withClue("a second activation in the same turn is illegal") {
                game.execute(ActivateAbility(game.player1Id, translator, manaId)).error shouldNotBe null
            }
            game.getLifeTotal(1) shouldBe 19
        }
    }
}
