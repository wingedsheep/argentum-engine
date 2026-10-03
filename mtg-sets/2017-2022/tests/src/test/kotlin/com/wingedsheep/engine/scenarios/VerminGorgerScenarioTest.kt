package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh2.cards.VerminGorger
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Vermin Gorger (MH2 #107) — {T}, sacrifice another creature: drain each opponent for 2. */
class VerminGorgerScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = VerminGorger.activatedAbilities.first().id

        test("sacrificing another creature drains each opponent for 2") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Vermin Gorger")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val r = game.execute(
                ActivateAbility(
                    game.player1Id, game.findPermanent("Vermin Gorger")!!, abilityId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(game.findPermanent("Grizzly Bears")!!)),
                )
            )
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 22
        }

        test("the Gorger can't sacrifice itself") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Vermin Gorger")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val gorger = game.findPermanent("Vermin Gorger")!!
            game.execute(
                ActivateAbility(
                    game.player1Id, gorger, abilityId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(gorger)),
                )
            ).error shouldNotBe null
        }
    }
}
