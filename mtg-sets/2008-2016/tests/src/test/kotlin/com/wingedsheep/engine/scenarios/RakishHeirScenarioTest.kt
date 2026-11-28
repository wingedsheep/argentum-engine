package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Rakish Heir (ISD #158) — {2}{R} 2/2 Vampire.
 *
 *   Whenever a Vampire you control deals combat damage to a player, put a +1/+1 counter on it.
 */
class RakishHeirScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("the counter goes on the Vampire that dealt the damage, not on a non-Vampire") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Rakish Heir")
                .withCardOnBattlefield(1, "Vampire Interloper")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val heir = game.findPermanent("Rakish Heir")!!
            val interloper = game.findPermanent("Vampire Interloper")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Vampire Interloper" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(2) shouldBe 16
            withClue("the attacking Vampire gets the counter") { game.plusOnes(interloper) shouldBe 1 }
            withClue("Rakish Heir didn't deal damage") { game.plusOnes(heir) shouldBe 0 }
            withClue("Grizzly Bears isn't a Vampire") { game.plusOnes(bears) shouldBe 0 }
        }

        test("an opponent's Vampire dealing combat damage doesn't trigger it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Rakish Heir")
                .withCardOnBattlefield(2, "Vampire Interloper")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val interloper = game.findPermanent("Vampire Interloper")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Vampire Interloper" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(1) shouldBe 18
            game.plusOnes(interloper) shouldBe 0
        }
    }
}
