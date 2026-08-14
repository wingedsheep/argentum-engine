package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Crimestopper Sprite (MKM) — "As an additional cost to cast this spell, you may collect evidence 6.
 * Flying. When this creature enters, tap target creature. If evidence was collected, put a stun
 * counter on it."
 *
 * The *rider* half of the CR 701.59c linkage, and the counterpoint to Vitu-Ghazi Inspector: the
 * trigger here is unconditional, so it fires and targets whether or not evidence was collected, and
 * only the stun counter is gated. That distinction is what these tests pin — a linkage implemented
 * only as an intervening-if would wrongly skip the tap.
 */
class CrimestopperSpriteScenarioTest : ScenarioTestBase() {

    private fun stunCounters(game: TestGame, name: String): Int =
        game.state.getEntity(game.findPermanent(name)!!)
            ?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    private fun isTapped(game: TestGame, name: String): Boolean =
        game.state.getEntity(game.findPermanent(name)!!)?.has<TappedComponent>() == true

    init {
        test("collecting evidence taps the creature and stuns it") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Crimestopper Sprite")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.castSpellCollectingEvidence(
                1, "Crimestopper Sprite", "Centaur Courser", "Centaur Courser"
            ).error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(bears))
            game.resolveStack()

            isTapped(game, "Grizzly Bears") shouldBe true
            stunCounters(game, "Grizzly Bears") shouldBe 1
        }

        test("without evidence the tap still happens — only the stun counter is gated") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Crimestopper Sprite")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.castSpell(1, "Crimestopper Sprite").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(bears))
            game.resolveStack()

            isTapped(game, "Grizzly Bears") shouldBe true
            stunCounters(game, "Grizzly Bears") shouldBe 0
            game.isInGraveyard(1, "Centaur Courser") shouldBe true
        }
    }
}
