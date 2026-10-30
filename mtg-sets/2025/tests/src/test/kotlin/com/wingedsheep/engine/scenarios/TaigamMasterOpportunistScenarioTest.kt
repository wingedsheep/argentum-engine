package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SuspendedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Taigam, Master Opportunist (TDM) — {1}{U} Legendary Human Monk.
 *
 * "Flurry — Whenever you cast your second spell each turn, copy it, then exile the spell you
 * cast with four time counters on it. If it doesn't have suspend, it gains suspend."
 *
 * Exercises the reusable Suspend chain ([com.wingedsheep.sdk.dsl.Effects.Suspend]): the copy
 * resolves normally while the original spell is exiled with four time counters and the
 * suspended marker. (The owner's-upkeep countdown that recasts it for free is covered by
 * `SuspendMechanicTest`.)
 */
class TaigamMasterOpportunistScenarioTest : ScenarioTestBase() {

    init {
        context("Taigam, Master Opportunist Flurry") {

            test("the second spell is copied and the original is exiled with four time counters and suspend") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Taigam, Master Opportunist")
                    .withCardsInHand(1, "Grizzly Bears", 2)
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // First spell of the turn — resolves into a real Grizzly Bears.
                game.castSpell(1, "Grizzly Bears")
                game.resolveStack()

                // Second spell triggers Flurry. Resolving the stack runs the trigger (copy the
                // spell + exile the original with suspend), then the copy resolves into a token.
                game.castSpell(1, "Grizzly Bears")
                game.resolveStack()

                val exiled = game.state.getExile(game.player1Id).filter { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                withClue("Exactly one Grizzly Bears (the original second spell) is exiled") {
                    exiled.size shouldBe 1
                }
                val suspended = exiled.first()
                withClue("The exiled spell carries the suspended marker") {
                    game.state.getEntity(suspended)?.has<SuspendedComponent>() shouldBe true
                }
                withClue("The exiled spell has four time counters on it") {
                    game.state.getEntity(suspended)?.get<CountersComponent>()
                        ?.getCount(CounterType.TIME) shouldBe 4
                }
                withClue("Suspend pre-arms haste (CR 702.62a) as a dormant effect on the exiled card") {
                    val hasteArmed = game.state.floatingEffects.any { fx ->
                        suspended in fx.effect.affectedEntities &&
                            (fx.effect.modification as? SerializableModification.GrantKeyword)?.keyword == Keyword.HASTE.name
                    }
                    hasteArmed shouldBe true
                }

                // The first Bears (real) plus the copy (a token) are on the battlefield.
                val bears = game.state.getBattlefield().filter { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                withClue("Two Grizzly Bears on the battlefield: the first spell and the copy") {
                    bears.size shouldBe 2
                }
                withClue("Rule 707.10 — the copy of a creature spell is a token") {
                    bears.count { game.state.getEntity(it)?.has<TokenComponent>() == true } shouldBe 1
                }
            }
        }
    }
}
