package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Survivor of Korlis (BRO #28) — first strike; {1}{W}, Exile this card from your graveyard: Scry 2.
 */
class SurvivorOfKorlisScenarioTest : ScenarioTestBase() {

    init {
        test("exiling it from the graveyard scries 2") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Survivor of Korlis")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val survivor = game.findCardsInGraveyard(1, "Survivor of Korlis").single()
            val abilityId = cardRegistry.getCard("Survivor of Korlis")!!.activatedAbilities.first().id

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = survivor, abilityId = abilityId)
            ).error shouldBe null

            game.isInExile(1, "Survivor of Korlis") shouldBe true
            game.isInGraveyard(1, "Survivor of Korlis") shouldBe false

            game.resolveStack()
            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            decision.options.size shouldBe 2
            game.selectCards(decision.options).error shouldBe null
            game.resolveStack()

            game.librarySize(1) shouldBe 3
            game.state.getEntity(game.state.getLibrary(game.player1Id).first())!!
                .get<CardComponent>()!!.name shouldBe "Island"
        }
    }
}
