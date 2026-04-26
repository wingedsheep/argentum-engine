package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class GrubStoriedMatriarchScenarioTest : ScenarioTestBase() {
    init {
        context("Grub, Storied Matriarch // Grub, Notorious Auntie") {
            test("Notorious Auntie blights a creature and creates a tapped attacking copy token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grub, Notorious Auntie", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val originalBears = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attackResult = game.declareAttackers(mapOf("Grub, Notorious Auntie" to 2))
                withClue("Attack declaration should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                game.resolveStack()
                withClue("Grub's attack trigger should ask whether to blight") {
                    game.hasPendingDecision() shouldBe true
                }

                game.answerYesNo(true)
                withClue("Choosing to blight should prompt for the creature to receive the -1/-1 counter") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectCards(listOf(originalBears))
                game.resolveStack()

                val counters = game.state.getEntity(originalBears)?.get<CountersComponent>()
                withClue("Original Grizzly Bears should have one -1/-1 counter from blight 1") {
                    counters.shouldNotBeNull()
                    counters.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 1
                }

                val bears = game.state.getBattlefield(game.player1Id).filter { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                withClue("Expected original Grizzly Bears plus one token copy") {
                    bears.size shouldBe 2
                }

                val tokenCopy = bears.firstOrNull { entityId ->
                    game.state.getEntity(entityId)?.has<TokenComponent>() == true
                }
                withClue("One Grizzly Bears should be a token copy") {
                    tokenCopy.shouldNotBeNull()
                }

                val tokenContainer = game.state.getEntity(tokenCopy!!).shouldNotBeNull()
                withClue("The copy token should enter tapped and attacking") {
                    tokenContainer.has<TappedComponent>() shouldBe true
                    tokenContainer.has<AttackingComponent>() shouldBe true
                }

                withClue("The copy token should have its end-step sacrifice trigger granted") {
                    game.state.grantedTriggeredAbilities.any { it.entityId == tokenCopy } shouldBe true
                }
            }
        }
    }
}
