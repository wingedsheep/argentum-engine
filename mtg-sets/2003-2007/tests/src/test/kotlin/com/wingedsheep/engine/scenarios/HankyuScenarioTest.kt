package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.matchers.shouldBe

/**
 * Hankyu — equipped creature has "{T}: Put an aim counter on Hankyu" and "{T}, Remove all aim
 * counters from Hankyu: This creature deals damage to any target equal to the number of aim
 * counters removed this way."
 *
 * The counters sit on the Equipment, the damage comes from the creature, and the amount is what
 * the cost removed — the counters are already gone when the ability resolves.
 */
class HankyuScenarioTest : ScenarioTestBase() {

    private fun TestGame.aimCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.AIM) ?: 0

    private fun TestGame.addAim(id: EntityId, n: Int) {
        state = state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.AIM, n))
        }
    }

    private val grants by lazy {
        cardRegistry.requireCard("Hankyu").script.staticAbilities
            .filterIsInstance<GrantActivatedAbility>().map { it.ability.id }
    }
    private val aimAbility get() = grants[0]
    private val fireAbility get() = grants[1]

    private fun equippedGame(): TestGame = scenario()
        .withPlayers()
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardAttachedTo(1, "Hankyu", "Grizzly Bears")
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("the aim ability taps the creature and puts the counter on Hankyu") {
            val game = equippedGame()
            val bears = game.findPermanent("Grizzly Bears")!!
            val hankyu = game.findPermanent("Hankyu")!!

            game.execute(ActivateAbility(game.player1Id, bears, aimAbility)).error shouldBe null
            game.resolveStack()

            game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
            game.state.getEntity(hankyu)!!.has<TappedComponent>() shouldBe false
            game.aimCounters(hankyu) shouldBe 1
            game.aimCounters(bears) shouldBe 0
        }

        test("firing removes every aim counter and deals that much damage") {
            val game = equippedGame()
            val bears = game.findPermanent("Grizzly Bears")!!
            val hankyu = game.findPermanent("Hankyu")!!
            game.addAim(hankyu, 3)

            game.execute(
                ActivateAbility(
                    game.player1Id, bears, fireAbility,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                )
            ).error shouldBe null
            // The cost took the counters off before the ability was even on the stack.
            game.aimCounters(hankyu) shouldBe 0
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
        }

        test("the damage is the number removed, not the number on Hankyu at resolution") {
            val game = equippedGame()
            val bears = game.findPermanent("Grizzly Bears")!!
            val hankyu = game.findPermanent("Hankyu")!!
            game.addAim(hankyu, 2)

            game.execute(
                ActivateAbility(
                    game.player1Id, bears, fireAbility,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                )
            ).error shouldBe null
            // New aim counters arriving while the ability is on the stack don't change the count.
            game.addAim(hankyu, 5)
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.aimCounters(hankyu) shouldBe 5
        }

        test("with no aim counters it can still be activated and deals no damage") {
            val game = equippedGame()
            val bears = game.findPermanent("Grizzly Bears")!!

            val fireOffers = game.getLegalActions(1).filter { (it.action as? ActivateAbility)?.abilityId == fireAbility }
            fireOffers.size shouldBe 1
            fireOffers.single().description.startsWith("{T}, Remove all aim counters from the granting permanent:") shouldBe true

            game.execute(
                ActivateAbility(
                    game.player1Id, bears, fireAbility,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 20
        }

        test("the damage is dealt by the equipped creature, so its lifelink applies") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Child of Night")
                .withCardOnBattlefield(2, "Glory Seeker")
                .withCardAttachedTo(1, "Hankyu", "Child of Night")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val child = game.findPermanent("Child of Night")!!
            val hankyu = game.findPermanent("Hankyu")!!
            val seeker = game.findPermanent("Glory Seeker")!!
            game.addAim(hankyu, 2)

            game.execute(
                ActivateAbility(
                    game.player1Id, child, fireAbility,
                    targets = listOf(ChosenTarget.Permanent(seeker)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.findPermanent("Glory Seeker") shouldBe null
            game.getLifeTotal(1) shouldBe 22
        }
    }
}
