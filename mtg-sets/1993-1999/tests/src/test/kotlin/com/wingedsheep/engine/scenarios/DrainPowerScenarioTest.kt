package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Drain Power (LEA #56) — "Target player activates a mana ability of each land they control. Then
 * that player loses all unspent mana and you add the mana lost this way."
 */
class DrainPowerScenarioTest : ScenarioTestBase() {
    init {
        fun TestGame.pool(playerId: EntityId): ManaPoolComponent =
            state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

        fun TestGame.isTapped(name: String): Boolean =
            findAllPermanents(name).all { state.getEntity(it)!!.has<TappedComponent>() }

        test("the target player taps each untapped land, choosing City of Brass's colour, and the caster gets the mana") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Drain Power")
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(2, "Forest", 2)
                .withCardOnBattlefield(2, "City of Brass")
                .withCardOnBattlefield(2, "Mountain", tapped = true)
                .withActivePlayer(1).build()

            game.castSpellTargetingPlayer(1, "Drain Power", 2).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseColorDecision>()
            withClue("the target player makes the choices of their own activation") {
                decision.playerId shouldBe game.player2Id
            }
            game.submitDecision(ColorChosenResponse(decision.id, Color.RED)).error shouldBe null

            game.isTapped("Forest") shouldBe true
            game.isTapped("City of Brass") shouldBe true
            game.pool(game.player1Id).green shouldBe 2
            game.pool(game.player1Id).red shouldBe 1
            game.pool(game.player1Id).total shouldBe 3
            game.pool(game.player2Id).total shouldBe 0

            game.resolveStack()
            withClue("City of Brass became tapped, so it damages its controller") {
                game.getLifeTotal(2) shouldBe 19
            }
        }

        test("mana a land's tap trigger adds is lost and moved as well") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Drain Power")
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(2, "Forest", 2)
                .withCardOnBattlefield(2, "Heartbeat of Spring")
                .withActivePlayer(1).build()

            game.castSpellTargetingPlayer(1, "Drain Power", 2).error shouldBe null
            game.resolveStack()

            game.getPendingDecision() shouldBe null
            withClue("each Forest made {G}{G} under Heartbeat of Spring, all of it drained") {
                game.pool(game.player1Id).green shouldBe 4
                game.pool(game.player2Id).total shouldBe 0
            }
        }

        test("mana the target makes in response is drained too, and a land tapped in response isn't activated again") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Drain Power")
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(2, "Forest", 1)
                .withLandsOnBattlefield(2, "Swamp", 1)
                .withActivePlayer(1).build()

            game.castSpellTargetingPlayer(1, "Drain Power", 2).error shouldBe null
            game.passPriority()
            val forest = game.findPermanent("Forest")!!
            game.execute(ActivateAbility(game.player2Id, forest, AbilityId.intrinsicMana(Color.GREEN.symbol))).error shouldBe null
            game.pool(game.player2Id).green shouldBe 1
            game.resolveStack()

            game.pool(game.player1Id).green shouldBe 1
            game.pool(game.player1Id).black shouldBe 1
            game.pool(game.player2Id).total shouldBe 0
        }
    }
}
