package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mom.cards.CompleatedHuntmaster
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Compleated Huntmaster (MOM #96) — "{1}, {T}, Sacrifice another creature or artifact: Incubate 3."
 */
class CompleatedHuntmasterScenarioTest : ScenarioTestBase() {

    private val ability = CompleatedHuntmaster.activatedAbilities[0].id

    init {
        context("Compleated Huntmaster") {
            test("sacrificing another creature incubates 3") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Compleated Huntmaster", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val huntmaster = game.findPermanent("Compleated Huntmaster")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = huntmaster,
                        abilityId = ability,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.findPermanent("Grizzly Bears") shouldBe null
                game.findPermanent("Compleated Huntmaster") shouldNotBe null
                val incubator = game.findPermanent("Incubator")!!
                game.state.getEntity(incubator)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
            }
        }
    }
}
