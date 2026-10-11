package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Saw in Half (UNF #88) — "Destroy target creature. If that creature dies this way, its controller
 * creates two tokens that are copies of that creature, except their power is half that creature's
 * power and their toughness is half that creature's toughness. Round up each time."
 */
class SawInHalfScenarioTest : ScenarioTestBase() {
    init {
        fun TestGame.tokensNamed(name: String): List<EntityId> =
            findPermanents(name).filter { state.getEntity(it)?.has<TokenComponent>() == true }

        fun TestGame.controllerOf(id: EntityId): EntityId? =
            state.getEntity(id)?.get<ControllerComponent>()?.playerId

        test("the creature's controller gets two half-size copies, rounded up") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardInHand(1, "Saw in Half")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wurm = game.findPermanent("Craw Wurm")!!
            game.castSpell(1, "Saw in Half", wurm).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Craw Wurm") shouldBe true
            val tokens = game.tokensNamed("Craw Wurm")
            tokens shouldHaveSize 2
            for (token in tokens) {
                withClue("a 6/4 halves to 3/2") {
                    game.state.projectedState.getPower(token) shouldBe 3
                    game.state.projectedState.getToughness(token) shouldBe 2
                }
                game.controllerOf(token) shouldBe game.player2Id
            }
        }

        test("the halves use the creature's last-known power and toughness, pumps included") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Giant Growth")
                .withCardInHand(1, "Saw in Half")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Giant Growth", bears).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Saw in Half", bears).error shouldBe null
            game.resolveStack()

            val tokens = game.tokensNamed("Grizzly Bears")
            tokens shouldHaveSize 2
            for (token in tokens) {
                withClue("a pumped 5/5 halves to 3/3 (the printed 2/2 would give 1/1)") {
                    game.state.projectedState.getPower(token) shouldBe 3
                    game.state.projectedState.getToughness(token) shouldBe 3
                }
                game.controllerOf(token) shouldBe game.player1Id
            }
        }

        test("a creature that doesn't die gives no tokens") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(2, "Darksteel Myr")
                .withCardInHand(1, "Saw in Half")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val myr = game.findPermanent("Darksteel Myr")!!
            game.castSpell(1, "Saw in Half", myr).error shouldBe null
            game.resolveStack()

            game.findPermanents("Darksteel Myr") shouldBe listOf(myr)
        }

        test("a dead Clone is copied as the creature it was copying") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardInHand(2, "Clone")
                .withCardInHand(1, "Saw in Half")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withLandsOnBattlefield(2, "Island", 4)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wurm = game.findPermanent("Craw Wurm")!!
            game.castSpell(2, "Clone").error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(wurm)).error shouldBe null
            val clone = game.findPermanents("Craw Wurm").single { it != wurm }
            game.passPriority() // the active player passes; Saw in Half is cast in response

            game.castSpell(1, "Saw in Half", clone).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Clone") shouldBe true
            val tokens = game.tokensNamed("Craw Wurm")
            tokens shouldHaveSize 2
            for (token in tokens) {
                game.state.getEntity(token)?.get<CardComponent>()?.name shouldBe "Craw Wurm"
                game.state.projectedState.getPower(token) shouldBe 3
                game.state.projectedState.getToughness(token) shouldBe 2
            }
        }
    }
}
