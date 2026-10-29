package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Devoted Druid — "Put a -1/-1 counter on this creature: Untap this creature."
 *
 * The counter is paid at activation, so the second activation kills the 0/2 before its untap
 * resolves; and a Druid that can't have counters put on it can't pay the cost at all (CR 614.17b,
 * the card's 2018-12-07 Solemnity ruling) — here Blossombind supplies the "can't have counters".
 */
class DevotedDruidScenarioTest : ScenarioTestBase() {

    private val untapAbilityId by lazy {
        cardRegistry.requireCard("Devoted Druid").activatedAbilities[1].id
    }

    private fun TestGame.minusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0

    private fun TestGame.untapOffered(id: EntityId): Boolean =
        getLegalActions(1).any {
            val action = it.action
            action is ActivateAbility && action.sourceId == id && action.abilityId == untapAbilityId
        }

    init {
        test("the -1/-1 counter is paid at activation and the untap resolves") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Devoted Druid", tapped = true)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val druid = game.findPermanent("Devoted Druid")!!

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = druid, abilityId = untapAbilityId)
            ).error shouldBe null
            game.minusCounters(druid) shouldBe 1
            game.state.getEntity(druid)!!.has<TappedComponent>() shouldBe true

            game.resolveStack()
            game.state.getEntity(druid)!!.has<TappedComponent>() shouldBe false
        }

        test("the second counter kills it before the untap can resolve") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Devoted Druid", tapped = true)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val druid = game.findPermanent("Devoted Druid")!!
            game.state = game.state.updateEntity(druid) {
                it.with(CountersComponent().withAdded(CounterType.MINUS_ONE_MINUS_ONE, 1))
            }

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = druid, abilityId = untapAbilityId)
            ).error shouldBe null

            game.isOnBattlefield("Devoted Druid") shouldBe false
            game.isInGraveyard(1, "Devoted Druid") shouldBe true
        }

        test("a Druid that can't have counters put on it can't activate the untap") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Devoted Druid")
                .withCardAttachedTo(1, "Blossombind", "Devoted Druid")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val druid = game.findPermanent("Devoted Druid")!!

            game.untapOffered(druid) shouldBe false
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = druid, abilityId = untapAbilityId)
            ).error shouldNotBe null
            game.minusCounters(druid) shouldBe 0
        }

        test("without the prohibition the untap is offered") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Devoted Druid")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.untapOffered(game.findPermanent("Devoted Druid")!!) shouldBe true
        }
    }
}
