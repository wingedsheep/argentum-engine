package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.ProstheticInjector
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Prosthetic Injector (ONE #239) — {1} Equipment
 *   Equipped creature gets +0/+2 and has toxic 1.
 *   Equip {1}
 */
class ProstheticInjectorScenarioTest : ScenarioTestBase() {

    private fun poison(game: TestGame, playerId: EntityId): Int =
        game.state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    private fun attackWithBears(game: TestGame) {
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareNoBlockers()
        game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
    }

    init {
        test("equipped creature gets +0/+2 and its combat damage gives a poison counter") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Prosthetic Injector", "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Grizzly Bears")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bears) shouldBe 2
            game.state.projectedState.getToughness(bears) shouldBe 4

            attackWithBears(game)

            game.getLifeTotal(2) shouldBe 18
            poison(game, game.player2Id) shouldBe 1
        }

        test("unequipped creature has no bonus and deals no poison; equip grants both") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Prosthetic Injector")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Grizzly Bears")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val injector = game.findPermanent("Prosthetic Injector")!!
            game.state.projectedState.getToughness(bears) shouldBe 2

            val equipId = ProstheticInjector.activatedAbilities.single { it.isEquipAbility }.id
            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = injector,
                    abilityId = equipId,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                )
            )
            result.error shouldBe null
            game.resolveStack()
            game.state.getEntity(injector)?.get<AttachedToComponent>()?.targetId shouldBe bears
            game.state.projectedState.getToughness(bears) shouldBe 4

            attackWithBears(game)
            game.getLifeTotal(2) shouldBe 18
            poison(game, game.player2Id) shouldBe 1
        }

        test("an unequipped attacker deals no poison") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Prosthetic Injector")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Grizzly Bears")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            attackWithBears(game)
            game.getLifeTotal(2) shouldBe 18
            poison(game, game.player2Id) shouldBe 0
        }
    }
}
