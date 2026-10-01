package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.AtmosphereSurgeon
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Atmosphere Surgeon (ONE #41) — {1}{U} 2/1 Creature — Phyrexian Wizard.
 *
 *   Whenever you cast a noncreature spell, put an oil counter on this creature.
 *   Remove an oil counter from this creature: Target creature gains flying until end of turn.
 *   Activate only as a sorcery.
 */
class AtmosphereSurgeonScenarioTest : ScenarioTestBase() {

    private val abilityId = AtmosphereSurgeon.activatedAbilities.first().id

    private fun oil(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    private fun seedOil(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.OIL, amount))
        }
    }

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Atmosphere Surgeon")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInHand(1, "Divination")
        .withCardInHand(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Island", 3)
        .withLandsOnBattlefield(1, "Forest", 2)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun activate(game: TestGame, surgeon: EntityId, target: EntityId) =
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = surgeon,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(target)),
            )
        )

    init {
        test("casting a noncreature spell puts an oil counter on it") {
            val game = board()
            val surgeon = game.findPermanent("Atmosphere Surgeon")!!

            game.castSpell(1, "Divination").error shouldBe null
            game.resolveStack()

            oil(game, surgeon) shouldBe 1
        }

        test("casting a creature spell does not trigger it") {
            val game = board()
            val surgeon = game.findPermanent("Atmosphere Surgeon")!!

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.state.stack.size shouldBe 1
            game.resolveStack()

            oil(game, surgeon) shouldBe 0
        }

        test("removing an oil counter gives target creature flying until end of turn") {
            val game = board()
            val surgeon = game.findPermanent("Atmosphere Surgeon")!!
            val opposingBears = game.findPermanent("Grizzly Bears")!!
            seedOil(game, surgeon, 1)

            activate(game, surgeon, opposingBears).error shouldBe null
            oil(game, surgeon) shouldBe 0
            game.resolveStack()

            game.state.projectedState.hasKeyword(opposingBears, Keyword.FLYING) shouldBe true
            game.state.projectedState.hasKeyword(surgeon, Keyword.FLYING) shouldBe false

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.projectedState.hasKeyword(opposingBears, Keyword.FLYING) shouldBe false
        }

        test("can't be activated without an oil counter") {
            val game = board()
            val surgeon = game.findPermanent("Atmosphere Surgeon")!!
            val opposingBears = game.findPermanent("Grizzly Bears")!!

            activate(game, surgeon, opposingBears).error shouldNotBe null
        }

        test("can only be activated as a sorcery") {
            val game = board()
            val surgeon = game.findPermanent("Atmosphere Surgeon")!!
            val opposingBears = game.findPermanent("Grizzly Bears")!!
            seedOil(game, surgeon, 1)

            game.castSpell(1, "Divination").error shouldBe null
            // The stack holds Divination and the oil trigger — not sorcery timing.
            activate(game, surgeon, opposingBears).error shouldNotBe null
            oil(game, surgeon) shouldBe 1
        }
    }
}
