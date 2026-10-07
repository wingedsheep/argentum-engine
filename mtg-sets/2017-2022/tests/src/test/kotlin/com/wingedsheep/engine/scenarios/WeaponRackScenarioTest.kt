package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.eld.cards.WeaponRack
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

class WeaponRackScenarioTest : ScenarioTestBase() {
    private fun TestGame.counters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.activate(rack: EntityId, creature: EntityId) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = rack,
            abilityId = WeaponRack.activatedAbilities[0].id,
            targets = listOf(ChosenTarget.Permanent(creature))
        )
    )

    init {
        test("enters with three counters and can move one onto an opponent's creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Weapon Rack")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Weapon Rack").error shouldBe null
            game.resolveStack()
            val rack = game.findPermanent("Weapon Rack")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.counters(rack) shouldBe 3
            game.activate(rack, bears).error shouldBe null
            game.resolveStack()
            game.counters(rack) shouldBe 2
            game.counters(bears) shouldBe 1
        }

        test("an empty rack can activate but does not create a counter") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Weapon Rack")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val rack = game.findPermanent("Weapon Rack")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(rack) { it.with(CountersComponent()) }
            game.activate(rack, bears).error shouldBe null
            game.resolveStack()
            game.counters(rack) shouldBe 0
            game.counters(bears) shouldBe 0
        }

        test("cannot activate during combat") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Weapon Rack")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            val rack = game.findPermanent("Weapon Rack")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.activate(rack, bears).error shouldBe "This ability can only be activated as a sorcery"
        }
    }
}
