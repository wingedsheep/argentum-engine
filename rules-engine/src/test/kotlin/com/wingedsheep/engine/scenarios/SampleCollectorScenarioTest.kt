package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Sample Collector (MKM) — "Whenever this creature attacks, you may collect evidence 3. When you do,
 * put a +1/+1 counter on target creature you control."
 *
 * The *effect* shape: collecting is not a cost here, and nothing on the card is linked. What these
 * tests pin is the reflexive trigger (CR 603.12) — the target is chosen *after* the collection
 * resolves, not when the attack trigger goes on the stack — and the CR 701.59b decline path, where
 * a graveyard that can't reach 3 means the player is never asked.
 */
class SampleCollectorScenarioTest : ScenarioTestBase() {

    private fun counters(game: TestGame, name: String): Int =
        game.state.getEntity(game.findPermanent(name)!!)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("attacking and collecting evidence 3 puts a counter on a creature you control") {
            val game = scenario()
                .withPlayers("Attacker", "Defender")
                .withCardOnBattlefield(1, "Sample Collector")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.declareAttackers(mapOf("Sample Collector" to 2)).error shouldBe null
            game.resolveStack()

            // "You may collect evidence 3" — say yes.
            game.answerYesNo(true)
            game.resolveStack()
            // The reflexive "when you do" then picks its target.
            game.selectTargets(listOf(bears))
            game.resolveStack()

            game.isInExile(1, "Centaur Courser") shouldBe true
            counters(game, "Grizzly Bears") shouldBe 1
        }

        test("declining the collection exiles nothing and puts no counter") {
            val game = scenario()
                .withPlayers("Attacker", "Defender")
                .withCardOnBattlefield(1, "Sample Collector")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Sample Collector" to 2)).error shouldBe null
            game.resolveStack()

            game.answerYesNo(false)
            game.resolveStack()

            game.isInGraveyard(1, "Centaur Courser") shouldBe true
            counters(game, "Grizzly Bears") shouldBe 0
        }

        test("CR 701.59b — a graveyard under the threshold is never even asked") {
            val game = scenario()
                .withPlayers("Attacker", "Defender")
                .withCardOnBattlefield(1, "Sample Collector")
                .withCardOnBattlefield(1, "Grizzly Bears")
                // Lightning Bolt is mana value 1 — well short of 3.
                .withCardInGraveyard(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Sample Collector" to 2)).error shouldBe null
            game.resolveStack()

            // No yes/no prompt at all — the option is absent, not offered and refused.
            game.state.pendingDecision shouldBe null
            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            counters(game, "Grizzly Bears") shouldBe 0
        }
    }
}
