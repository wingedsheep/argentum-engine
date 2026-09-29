package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mom.cards.StormclawRager
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Stormclaw Rager (MOM #254) — "{1}, Sacrifice another creature or artifact: Put a +1/+1 counter on
 * this creature and draw a card. Activate only as a sorcery."
 */
class StormclawRagerScenarioTest : ScenarioTestBase() {

    private val ability = StormclawRager.activatedAbilities[0].id

    init {
        context("Stormclaw Rager") {
            test("sacrificing another creature puts a counter on it and draws a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Stormclaw Rager")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rager = game.findPermanent("Stormclaw Rager")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.handSize(1)
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rager,
                        abilityId = ability,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.findPermanent("Grizzly Bears") shouldBe null
                game.findPermanent("Stormclaw Rager") shouldNotBe null
                game.state.getEntity(rager)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                game.handSize(1) shouldBe handBefore + 1
            }

            test("cannot sacrifice itself to its own ability") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Stormclaw Rager")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rager = game.findPermanent("Stormclaw Rager")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rager,
                        abilityId = ability,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(rager))
                    )
                ).error shouldNotBe null
            }

            test("cannot be activated at instant speed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Stormclaw Rager")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()

                val rager = game.findPermanent("Stormclaw Rager")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rager,
                        abilityId = ability,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears))
                    )
                ).error shouldNotBe null
            }
        }
    }
}
