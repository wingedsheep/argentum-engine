package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mid.cards.LarderZombie
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/** Larder Zombie (MID #58) — Defender; "Tap three untapped creatures you control: Surveil 1." */
class LarderZombieScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = LarderZombie.activatedAbilities.single().id

        test("tapping three creatures (itself included, summoning sick) surveils 1") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Larder Zombie")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val zombie = game.findPermanent("Larder Zombie")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.state.projectedState.hasKeyword(zombie, Keyword.DEFENDER) shouldBe true

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = zombie,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(zombie, bears, giant)),
                ),
            ).error shouldBe null
            listOf(zombie, bears, giant).forEach { game.state.getEntity(it)!!.has<TappedComponent>() shouldBe true }
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("surveil 1 looks at the top card") { decision.options.size shouldBe 1 }
            game.selectCards(decision.options)
            if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()

            game.isInGraveyard(1, "Island") shouldBe true
        }

        test("cannot be activated with only two untapped creatures") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Larder Zombie")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val zombie = game.findPermanent("Larder Zombie")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = zombie,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(zombie, bears)),
                ),
            ).error shouldNotBe null
        }
    }
}
