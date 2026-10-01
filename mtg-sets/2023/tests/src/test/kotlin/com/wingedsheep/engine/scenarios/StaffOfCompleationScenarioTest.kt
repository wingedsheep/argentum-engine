package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Staff of Compleation (ONE #242) — {3} Artifact.
 *
 *   {T}, Pay 1 life: Destroy target permanent you own.
 *   {T}, Pay 2 life: Add one mana of any color.
 *   {T}, Pay 3 life: Proliferate.
 *   {T}, Pay 4 life: Draw a card.
 *   {5}: Untap this artifact.
 */
class StaffOfCompleationScenarioTest : ScenarioTestBase() {

    private fun abilityId(index: Int) =
        cardRegistry.requireCard("Staff of Compleation").activatedAbilities[index].id

    private fun board(lands: Int = 0) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Staff of Compleation")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withLandsOnBattlefield(1, "Forest", lands)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun isTapped(game: TestGame, id: EntityId) =
        game.state.getEntity(id)?.has<TappedComponent>() == true

    init {
        test("pay 2 life: adds one mana of the chosen color without using the stack") {
            val game = board()
            val staff = game.findPermanent("Staff of Compleation")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id, sourceId = staff, abilityId = abilityId(1),
                    manaColorChoice = Color.BLACK
                )
            ).error shouldBe null

            game.state.stack.size shouldBe 0
            game.getLifeTotal(1) shouldBe 18
            isTapped(game, staff) shouldBe true
            val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
            pool.black shouldBe 1
            pool.white + pool.blue + pool.red + pool.green shouldBe 0
        }

        test("pay 1 life: destroys a permanent you own") {
            val game = board()
            val staff = game.findPermanent("Staff of Compleation")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id, sourceId = staff, abilityId = abilityId(0),
                    targets = listOf(ChosenTarget.Permanent(bears))
                )
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 19
            isTapped(game, staff) shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }

        test("cannot target a permanent you don't own") {
            val game = board()
            val staff = game.findPermanent("Staff of Compleation")!!
            val giant = game.findPermanent("Hill Giant")!!

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id, sourceId = staff, abilityId = abilityId(0),
                    targets = listOf(ChosenTarget.Permanent(giant))
                )
            )
            withClue("an opponent's permanent is not a legal target") { result.error shouldNotBe null }
            game.findPermanent("Hill Giant") shouldBe giant
            game.getLifeTotal(1) shouldBe 20
        }

        test("pay 3 life: proliferate") {
            val game = board()
            val staff = game.findPermanent("Staff of Compleation")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(bears) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
            }

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = staff, abilityId = abilityId(2))
            ).error shouldBe null
            var guard = 0
            while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 20) {
                if (game.hasPendingDecision()) game.selectCards(listOf(bears)).error shouldBe null
                else game.resolveStack()
            }

            game.getLifeTotal(1) shouldBe 17
            game.state.getEntity(bears)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        }

        test("pay 4 life: draw a card, then {5}: untap and it can be used again") {
            val game = board(lands = 5)
            val staff = game.findPermanent("Staff of Compleation")!!
            val handBefore = game.handSize(1)

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = staff, abilityId = abilityId(3))
            ).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 16
            game.handSize(1) shouldBe handBefore + 1
            isTapped(game, staff) shouldBe true

            withClue("a tapped Staff can't pay its {T} cost again") {
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = staff, abilityId = abilityId(3))
                ).error shouldNotBe null
            }

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = staff, abilityId = abilityId(4))
            ).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()
            isTapped(game, staff) shouldBe false
        }
    }
}
