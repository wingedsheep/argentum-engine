package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.shouldBe

/**
 * Necrogen Communion (ONE #99) — {1}{B} Aura
 *   Enchant creature you control
 *   Enchanted creature has toxic 2.
 *   When enchanted creature dies, return that card to the battlefield under your control.
 */
class NecrogenCommunionScenarioTest : ScenarioTestBase() {

    private val slay = card("Slay Test Spell") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Destroy target creature."
        spell {
            val c = target(TargetFilter.Creature)
            effect = Effects.Destroy(c)
        }
    }

    private fun poison(game: TestGame, playerId: EntityId): Int =
        game.state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    init {
        cardRegistry.register(listOf(slay))

        test("enchanted creature's combat damage gives two poison counters") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Necrogen Communion", "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Grizzly Bears")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.getLifeTotal(2) shouldBe 18
            poison(game, game.player2Id) shouldBe 2
        }

        test("when enchanted creature dies it returns untapped under the Aura controller's control") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, "Grizzly Bears") // owned by player 2
                .withCardAttachedTo(1, "Necrogen Communion", "Grizzly Bears")
                .withCardInHand(1, "Slay Test Spell")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            // Player 1 controls the opponent-owned creature it enchanted.
            game.state = game.state.updateEntity(bears) { it.with(ControllerComponent(game.player1Id)) }

            game.castSpell(1, "Slay Test Spell", targetId = bears).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe false
            val returned = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getController(returned) shouldBe game.player1Id
            game.state.getEntity(returned)!!.has<TappedComponent>() shouldBe false
            // The Aura itself went to the graveyard and stays there.
            game.isInGraveyard(1, "Necrogen Communion") shouldBe true
        }
    }
}
