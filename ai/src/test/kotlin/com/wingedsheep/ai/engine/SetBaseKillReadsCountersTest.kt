package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * A base-P/T set until end of turn kills only what it leaves at 0 toughness *after* the counters
 * and pumps layered on top of the new base.
 *
 * User report (2026-11-04): the AI cast Multiply by Zero ("target creature has base power and
 * toughness 0/0 until end of turn") on creatures with +1/+1 counters, which it does not kill. The
 * [AiProfile.refuseWastedPayoffs] floor read the new base 0 as the creature's whole toughness,
 * called it a kill, and handed a cast that buys nothing back to the leaf — which prices the
 * shrink as if it outlived the turn.
 */
class SetBaseKillReadsCountersTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    /** The opponent's precombat main, a Grizzly Bears of theirs, Multiply by Zero in our hand. */
    private fun position(counters: Int = 0, damage: Int = 0): TestGame {
        val game = scenario()
            .withPlayers()
            .withTurnNumber(8)
            .withActivePlayer(2)
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withCardInHand(1, "Multiply by Zero")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .build()
            .advanceToPriority(1, Step.PRECOMBAT_MAIN)
        val bears = game.findPermanent("Grizzly Bears")!!
        game.state = game.state.updateEntity(bears) { c ->
            var next = c
            if (counters > 0) {
                next = next.with((c.get<CountersComponent>() ?: CountersComponent())
                    .withCounters(CounterType.PLUS_ONE_PLUS_ONE, counters))
            }
            if (damage > 0) next = next.with(DamageComponent(damage))
            next
        }
        return game
    }

    private fun TestGame.multiplyByZeroInHand() = state.getZone(player1Id, Zone.HAND).first {
        state.getEntity(it)?.get<CardComponent>()?.name == "Multiply by Zero"
    }

    private fun TestGame.verdict(): TimingVerdict =
        HoldPolicy(intents, refuseWastedPayoffs = true).verdictFor(
            state, player1Id, "Multiply by Zero",
            cast = CastSpell(
                player1Id, multiplyByZeroInHand(),
                targets = listOf(ChosenTarget.Permanent(findPermanent("Grizzly Bears")!!)),
            ),
        )

    init {
        test("0/0 on a creature with a +1/+1 counter leaves it a 1/1, so it kills nothing") {
            val game = position(counters = 1)

            game.verdict() shouldBe TimingVerdict.NoWindow
            val action = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).chooseAction(game.state)
            (action as? CastSpell)?.cardId shouldNotBe game.multiplyByZeroInHand()
        }

        test("0/0 on a creature with no counters is removal, and stays the leaf's call") {
            position().verdict() shouldNotBe TimingVerdict.NoWindow
        }

        test("marked damage still counts against what the counters leave") {
            position(counters = 1, damage = 1).verdict() shouldNotBe TimingVerdict.NoWindow
        }
    }
}
