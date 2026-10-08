package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.MeldedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Gisela, the Broken Blade (EMN) — "At the beginning of your end step, if you both own and control
 * Gisela and a creature named Bruna, the Fading Light, exile them, then meld them into Brisela,
 * Voice of Nightmares." (CR 701.42)
 */
class GiselaTheBrokenBladeScenarioTest : ScenarioTestBase() {

    init {
        test("at your end step with Bruna, the pair melds into Brisela, Voice of Nightmares") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Gisela, the Broken Blade")
                .withCardOnBattlefield(1, "Bruna, the Fading Light")
                .withActivePlayer(1)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()
            val gisela = game.findPermanent("Gisela, the Broken Blade")!!

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            val brisela = game.findPermanent("Brisela, Voice of Nightmares").shouldNotBeNull()
            withClue("Gisela's entity is the melded permanent") { brisela shouldBe gisela }
            game.findPermanent("Gisela, the Broken Blade") shouldBe null
            game.findPermanent("Bruna, the Fading Light") shouldBe null
            game.isInExile(1, "Gisela, the Broken Blade") shouldBe false
            game.isInExile(1, "Bruna, the Fading Light") shouldBe false
            game.state.getEntity(brisela)!!.has<MeldedComponent>() shouldBe true
            game.state.projectedState.getPower(brisela) shouldBe 9
            game.state.projectedState.getToughness(brisela) shouldBe 10
        }

        test("with Bruna under an opponent's control, the end-step trigger doesn't fire") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Gisela, the Broken Blade")
                .withCardOnBattlefield(2, "Bruna, the Fading Light")
                .withActivePlayer(1)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.ENDING, Step.END)
            withClue("the intervening-if fails, so the trigger never goes on the stack") {
                game.state.stack shouldBe emptyList()
            }
            game.resolveStack()

            game.findPermanent("Gisela, the Broken Blade").shouldNotBeNull()
            game.findPermanent("Bruna, the Fading Light").shouldNotBeNull()
            game.findPermanent("Brisela, Voice of Nightmares") shouldBe null
        }
    }
}
