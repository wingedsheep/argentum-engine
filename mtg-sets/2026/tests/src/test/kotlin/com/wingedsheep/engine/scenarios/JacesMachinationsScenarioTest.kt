package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.LoyaltyActivationPermissionsComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Jace's Machinations (FRA #32) — {2}{U} Instant:
 *   Until end of turn, you may activate loyalty abilities of Jace planeswalkers you control on any
 *   player's turn any time you could cast an instant.
 *   Empower Jace 8.
 *
 * Pins the instant-speed permission: it covers Jace planeswalkers you control (a Jace card and the
 * Jace token empower made), not other planeswalkers; each planeswalker's once-per-turn limit
 * (CR 606.3) still holds; and the permission ends with the turn.
 */
class JacesMachinationsScenarioTest : ScenarioTestBase() {
    init {
        val sculptorPlusOne = cardRegistry.getCard("Jace, Reality Sculptor")!!.script.activatedAbilities
            .single { (it.cost as? AbilityCost.Loyalty)?.change == 1 }.id
        val ajaniPlusOne = cardRegistry.getCard("Ajani Goldmane")!!.script.activatedAbilities
            .single { (it.cost as? AbilityCost.Loyalty)?.change == 1 }.id

        fun TestGame.loyaltyOf(id: EntityId): Int =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

        /** Opponent's precombat main phase, with you holding priority. */
        fun opponentsTurn() = scenario().withPlayers()
            .withCardInHand(1, "Jace's Machinations")
            .withLandsOnBattlefield(1, "Island", 3)
            .withCardOnBattlefield(1, "Jace, Reality Sculptor")
            .withCardOnBattlefield(1, "Ajani Goldmane")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(2)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        /** Resolve the stack, then have the active opponent pass so you hold priority again. */
        fun TestGame.resolveAndTakePriority() {
            resolveStack()
            if (state.priorityPlayerId == player2Id) passPriority()
            state.priorityPlayerId shouldBe player1Id
        }

        test("without it, loyalty abilities can't be activated on an opponent's turn") {
            val game = opponentsTurn().build()
            val jace = game.findPermanent("Jace, Reality Sculptor")!!
            game.execute(ActivateAbility(game.player1Id, jace, sculptorPlusOne)).error shouldNotBe null
        }

        test("on an opponent's turn: empower Jace 8, then activate Jaces at instant speed, once each") {
            val game = opponentsTurn().build()
            val jace = game.findPermanent("Jace, Reality Sculptor")!!
            val ajani = game.findPermanent("Ajani Goldmane")!!

            game.castSpell(1, "Jace's Machinations").error shouldBe null
            game.resolveAndTakePriority()

            val token = game.findPermanents("Jace").single()
            game.loyaltyOf(token) shouldBe 8

            withClue("Jace, Reality Sculptor is a Jace planeswalker you control") {
                game.execute(ActivateAbility(game.player1Id, jace, sculptorPlusOne)).error shouldBe null
            }
            game.resolveAndTakePriority()
            game.loyaltyOf(jace) shouldBe 6
            withClue("+1 empowered the Jace token by the three Islands") {
                game.loyaltyOf(token) shouldBe 11
            }

            withClue("the Jace token empower made is covered too") {
                val drawThree = game.state.getEntity(token)!!
                    .get<com.wingedsheep.engine.state.components.identity.CardComponent>()!!
                val tokenDraw = cardRegistry.getCard(drawThree.cardDefinitionId)!!.script.activatedAbilities
                    .single { (it.cost as? AbilityCost.Loyalty)?.change == -3 }.id
                val hand = game.handSize(1)
                game.execute(ActivateAbility(game.player1Id, token, tokenDraw)).error shouldBe null
                game.resolveAndTakePriority()
                game.handSize(1) shouldBe hand + 1
            }

            withClue("a non-Jace planeswalker is still sorcery-speed") {
                game.execute(ActivateAbility(game.player1Id, ajani, ajaniPlusOne)).error shouldNotBe null
            }
            withClue("CR 606.3's once-per-turn limit still applies") {
                game.execute(ActivateAbility(game.player1Id, jace, sculptorPlusOne)).error shouldNotBe null
            }
        }

        test("the permission ends with the turn") {
            val game = opponentsTurn().build()
            game.castSpell(1, "Jace's Machinations").error shouldBe null
            game.resolveAndTakePriority()
            game.state.getEntity(game.player1Id)!!.has<LoyaltyActivationPermissionsComponent>() shouldBe true

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.state.getEntity(game.player1Id)!!.has<LoyaltyActivationPermissionsComponent>() shouldBe false
        }
    }
}
