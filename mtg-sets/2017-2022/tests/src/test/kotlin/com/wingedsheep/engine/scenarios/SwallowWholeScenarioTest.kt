package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Swallow Whole (IKO #35) — {W} Sorcery.
 *
 * "As an additional cost to cast this spell, tap an untapped creature you control.
 *  Exile target tapped creature. Put a +1/+1 counter on the creature tapped to pay this spell's
 *  additional cost."
 *
 * The first spell to read `EffectTarget.TappedAsCost`: the creature tapped for the additional cost
 * is carried on the spell to resolution, so the counter lands on the payer, not on any other
 * creature the caster controls.
 */
class SwallowWholeScenarioTest : ScenarioTestBase() {

    private fun TestGame.counters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.cast(tapped: List<EntityId>, target: EntityId) = execute(
        CastSpell(
            player1Id,
            state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Swallow Whole" },
            listOf(ChosenTarget.Permanent(target)),
            additionalCostPayment = AdditionalCostPayment(tappedPermanents = tapped)
        )
    )

    init {
        test("exiles the tapped target and puts the counter on the creature tapped to pay") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Swallow Whole")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardOnBattlefield(1, "Savannah Lions")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val lions = game.findPermanent("Savannah Lions")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            val cast = game.cast(listOf(bears), giant)
            withClue("cast tapping Grizzly Bears: ${cast.error}") { cast.error shouldBe null }
            game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
            game.resolveStack()

            game.isInExile(2, "Hill Giant") shouldBe true
            withClue("the payer gets the counter") { game.counters(bears) shouldBe 1 }
            withClue("the caster's other creature doesn't") { game.counters(lions) shouldBe 0 }
        }

        test("can't be cast without an untapped creature to tap") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Swallow Whole")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.cast(listOf(bears), game.findPermanent("Hill Giant")!!).error shouldNotBe null
            game.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
