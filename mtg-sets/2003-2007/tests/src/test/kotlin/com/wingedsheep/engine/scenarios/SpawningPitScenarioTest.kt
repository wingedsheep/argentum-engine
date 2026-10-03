package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dst.cards.SpawningPit
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Spawning Pit (DST #141) — sacrifice creatures for charge counters, cash two in for a 2/2 Spawn. */
class SpawningPitScenarioTest : ScenarioTestBase() {
    init {
        val sacAbility = SpawningPit.activatedAbilities[0].id
        val tokenAbility = SpawningPit.activatedAbilities[1].id

        fun charge(game: TestGame): Int = game.state.getEntity(game.findPermanent("Spawning Pit")!!)!!
            .get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0

        test("two sacrificed creatures pay for a 2/2 colorless Spawn artifact creature token") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Spawning Pit")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Glory Seeker")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val pit = game.findPermanent("Spawning Pit")!!

            for (name in listOf("Grizzly Bears", "Glory Seeker")) {
                val fodder = game.findPermanent(name)!!
                val r = game.execute(
                    ActivateAbility(
                        game.player1Id, pit, sacAbility,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder))
                    )
                )
                withClue("${r.error}") { r.error shouldBe null }
                game.resolveStack()
                game.isInGraveyard(1, name) shouldBe true
            }
            charge(game) shouldBe 2

            val before = game.state.getZone(game.player1Id, Zone.BATTLEFIELD).toSet()
            val r = game.execute(ActivateAbility(game.player1Id, pit, tokenAbility))
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()

            charge(game) shouldBe 0
            val token = game.state.getZone(game.player1Id, Zone.BATTLEFIELD).filterNot { it in before }.single()
            val projected = game.state.projectedState
            projected.isCreature(token) shouldBe true
            projected.hasType(token, "ARTIFACT") shouldBe true
            projected.hasSubtype(token, "Spawn") shouldBe true
            projected.getPower(token) shouldBe 2
            projected.getToughness(token) shouldBe 2
            projected.getColors(token).isEmpty() shouldBe true
        }

        test("one charge counter is not enough to make a Spawn") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Spawning Pit")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val pit = game.findPermanent("Spawning Pit")!!
            game.execute(
                ActivateAbility(
                    game.player1Id, pit, sacAbility,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(game.findPermanent("Grizzly Bears")!!))
                )
            ).error shouldBe null
            game.resolveStack()
            charge(game) shouldBe 1

            game.execute(ActivateAbility(game.player1Id, pit, tokenAbility)).error shouldNotBe null
        }
    }
}
