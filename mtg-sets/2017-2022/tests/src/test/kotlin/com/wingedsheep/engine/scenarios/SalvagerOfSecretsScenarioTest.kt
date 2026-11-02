package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Salvager of Secrets (M19 #70) — "When this creature enters, return target instant or sorcery
 * card from your graveyard to your hand."
 */
class SalvagerOfSecretsScenarioTest : ScenarioTestBase() {
    init {
        test("returns an instant or sorcery from your own graveyard only") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Salvager of Secrets")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInGraveyard(1, "Divination")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(2, "Shock")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val divination = game.findCardsInGraveyard(1, "Divination").single()
            game.castSpell(1, "Salvager of Secrets").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldNotBeNull() as ChooseTargetsDecision
            decision.legalTargets[0].shouldNotBeNull() shouldContainExactlyInAnyOrder listOf(divination)
            game.selectTargets(listOf(divination)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Divination") shouldBe true
            game.isInGraveyard(2, "Shock") shouldBe true
        }
    }
}
