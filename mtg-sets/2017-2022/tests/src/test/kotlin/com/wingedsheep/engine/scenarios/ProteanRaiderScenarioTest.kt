package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Protean Raider (RIX #167) — "Raid — If you attacked this turn, you may have this creature enter
 * as a copy of any creature on the battlefield."
 */
class ProteanRaiderScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Protean Raider")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withLandsOnBattlefield(1, "Island", 2)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.attackWithBears() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
    }

    init {
        test("without having attacked it enters as a plain 2/2 with no copy choice") {
            val game = board().build()

            game.castSpell(1, "Protean Raider").error shouldBe null
            game.resolveStack()

            withClue("raid not met: no copy prompt") { game.hasPendingDecision() shouldBe false }
            val raider = game.findPermanent("Protean Raider")!!
            game.state.projectedState.getPower(raider) shouldBe 2
            game.state.projectedState.getToughness(raider) shouldBe 2
        }

        test("after attacking it may enter as a copy of any creature, including an opponent's") {
            val game = board().build()
            game.attackWithBears()

            game.castSpell(1, "Protean Raider").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            decision.minSelections shouldBe 0
            val giant = game.findPermanent("Hill Giant")!!
            decision.options shouldContainExactlyInAnyOrder listOf(game.findPermanent("Grizzly Bears")!!, giant)
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.findPermanents("Hill Giant").size shouldBe 2
            game.isOnBattlefield("Protean Raider") shouldBe false
        }

        test("after attacking, declining the copy leaves it as Protean Raider") {
            val game = board().build()
            game.attackWithBears()

            game.castSpell(1, "Protean Raider").error shouldBe null
            game.resolveStack()
            game.skipSelection().error shouldBe null
            game.resolveStack()

            val raider = game.findPermanent("Protean Raider")!!
            game.state.getEntity(raider)!!.get<CardComponent>()!!.name shouldBe "Protean Raider"
        }

        test("raid is checked for effect entries too: reanimated without an attack, no copy choice") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Protean Raider")
                .withCardInHand(1, "Zombify")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val raider = game.findCardsInGraveyard(1, "Protean Raider").single()
            game.castSpellTargetingGraveyardCard(1, "Zombify", listOf(raider)).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.isOnBattlefield("Protean Raider") shouldBe true
        }
    }
}
