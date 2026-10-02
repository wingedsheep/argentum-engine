package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.PhyrexianIronworks
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Phyrexian Ironworks (MH3) — "Whenever you attack, you get {E}. / {T}, Pay {E}{E}{E}: Create a
 * 3/3 colorless Phyrexian Golem artifact creature token. Activate only as a sorcery."
 */
class PhyrexianIronworksScenarioTest : ScenarioTestBase() {

    private val golemAbility = PhyrexianIronworks.activatedAbilities.single().id

    private fun TestGame.energy(): Int =
        state.getEntity(player1Id)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    private fun TestGame.seedEnergy(amount: Int) {
        state = state.updateEntity(player1Id) { container ->
            val current = container.get<CountersComponent>() ?: CountersComponent()
            container.with(current.withAdded(CounterType.ENERGY, amount))
        }
    }

    private fun build() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Phyrexian Ironworks")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Savannah Lions")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Phyrexian Ironworks") {

            test("attacking with two creatures gets you one energy, not one per attacker") {
                val game = build()
                game.energy() shouldBe 0
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2, "Savannah Lions" to 2)).error shouldBe null
                game.resolveStack()
                game.energy() shouldBe 1
            }

            test("tap and pay three energy creates a 3/3 colorless Phyrexian Golem artifact creature") {
                val game = build()
                game.seedEnergy(4)
                val ironworks = game.findPermanent("Phyrexian Ironworks")!!

                game.execute(ActivateAbility(game.player1Id, ironworks, golemAbility)).error shouldBe null
                game.resolveStack()

                withClue("three of the four energy were paid") { game.energy() shouldBe 1 }
                val golems = game.findPermanents("Phyrexian Golem Token")
                golems shouldHaveSize 1
                val golem = golems.single()
                val projected = game.state.projectedState
                projected.getPower(golem) shouldBe 3
                projected.getToughness(golem) shouldBe 3
                projected.isCreature(golem) shouldBe true
                projected.hasType(golem, "ARTIFACT") shouldBe true
                projected.hasSubtype(golem, "Phyrexian") shouldBe true
                projected.hasSubtype(golem, "Golem") shouldBe true
                projected.getColors(golem).isEmpty() shouldBe true
            }

            test("can't activate with only two energy") {
                val game = build()
                game.seedEnergy(2)
                val ironworks = game.findPermanent("Phyrexian Ironworks")!!
                game.execute(ActivateAbility(game.player1Id, ironworks, golemAbility)).error shouldNotBe null
                game.energy() shouldBe 2
                game.findPermanents("Phyrexian Golem Token") shouldHaveSize 0
            }

            test("activate only as a sorcery — not during combat") {
                val game = build()
                game.seedEnergy(3)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack()
                game.energy() shouldBe 4

                val ironworks = game.findPermanent("Phyrexian Ironworks")!!
                game.execute(ActivateAbility(game.player1Id, ironworks, golemAbility)).error shouldNotBe null
                game.energy() shouldBe 4
            }
        }
    }
}
