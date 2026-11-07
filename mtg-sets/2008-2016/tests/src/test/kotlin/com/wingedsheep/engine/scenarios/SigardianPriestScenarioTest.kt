package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.emn.cards.SigardianPriest
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sigardian Priest (EMN #42) — {1}{W} 1/2 Creature — Human Cleric.
 *
 *   {1}, {T}: Tap target non-Human creature.
 */
class SigardianPriestScenarioTest : ScenarioTestBase() {

    init {
        val abilityId = SigardianPriest.activatedAbilities.first().id

        fun board(): TestGame = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Sigardian Priest")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Steadfast Cathar")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("taps a target non-Human creature") {
            val game = board()
            val priest = game.findPermanent("Sigardian Priest")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = priest,
                    abilityId = abilityId,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                )
            )
            withClue("${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(priest)?.has<TappedComponent>() shouldBe true
        }

        test("a Human creature can't be targeted") {
            val game = board()
            val priest = game.findPermanent("Sigardian Priest")!!
            val cathar = game.findPermanent("Steadfast Cathar")!!

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = priest,
                    abilityId = abilityId,
                    targets = listOf(ChosenTarget.Permanent(cathar)),
                )
            )
            result.error shouldNotBe null
            game.state.getEntity(cathar)?.has<TappedComponent>() shouldBe false
        }
    }
}
