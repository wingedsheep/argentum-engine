package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.MonstrousComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SinuousVerminScenarioTest : ScenarioTestBase() {
    private val abilityId = cardRegistry.getCard("Sinuous Vermin")!!.activatedAbilities.single().id

    private fun TestGame.activate() = execute(
        ActivateAbility(playerId = player1Id, sourceId = findPermanent("Sinuous Vermin")!!, abilityId = abilityId)
    )

    init {
        test("monstrosity grants three counters and menace only once") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sinuous Vermin")
                .withLandsOnBattlefield(1, "Swamp", 10)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val vermin = game.findPermanent("Sinuous Vermin")!!
            game.state.projectedState.hasKeyword(vermin, Keyword.MENACE) shouldBe false

            game.activate().error shouldBe null
            game.resolveStack()
            game.state.getEntity(vermin)?.has<MonstrousComponent>() shouldBe true
            game.state.projectedState.getPower(vermin) shouldBe 5
            game.state.projectedState.getToughness(vermin) shouldBe 5
            game.state.projectedState.hasKeyword(vermin, Keyword.MENACE) shouldBe true

            game.activate().error shouldBe null
            game.resolveStack()
            game.state.getEntity(vermin)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
        }

        test("becoming monstrous before blockers requires at least two blockers") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sinuous Vermin")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.activate().error shouldBe null
            game.resolveStack()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Sinuous Vermin" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Sinuous Vermin"))).error shouldNotBe null
            game.declareBlockers(mapOf(
                "Grizzly Bears" to listOf("Sinuous Vermin"),
                "Centaur Courser" to listOf("Sinuous Vermin")
            )).error shouldBe null
        }

        test("gaining menace after a legal single block does not undo the block") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sinuous Vermin")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            game.declareAttackers(mapOf("Sinuous Vermin" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Sinuous Vermin"))).error shouldBe null

            game.activate().error shouldBe null
            game.resolveStack()
            game.state.projectedState.hasKeyword(game.findPermanent("Sinuous Vermin")!!, Keyword.MENACE) shouldBe true
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.getLifeTotal(2) shouldBe 20
            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isOnBattlefield("Sinuous Vermin") shouldBe true
        }
    }
}
