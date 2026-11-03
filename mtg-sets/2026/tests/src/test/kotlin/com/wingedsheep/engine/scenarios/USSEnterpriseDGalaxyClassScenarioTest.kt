package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * U.S.S. Enterprise-D, Galaxy-Class ({3} Legendary Artifact — Spacecraft, 4/5):
 * "Whenever one or more charge counters are put on U.S.S. Enterprise-D for the first time each turn,
 * exile the top card of your library. You may play that card this turn. Station. 7+ | Flying, vigilance"
 *
 * Proves the first-time-each-turn gate on charge counters (stationing twice in one turn triggers
 * once) and that the impulse-exiled card is playable that turn.
 */
class USSEnterpriseDGalaxyClassScenarioTest : ScenarioTestBase() {

    private val enterprise = "U.S.S. Enterprise-D, Galaxy-Class"

    private fun stationAbilityId() =
        cardRegistry.getCard(enterprise)!!.activatedAbilities
            .first { (it.cost as? AbilityCost.Atom)?.atom is CostAtom.TapPermanents }.id

    init {
        context("U.S.S. Enterprise-D, Galaxy-Class") {
            test("stationing twice in one turn triggers once, and the exiled card is playable") {
                val game = scenario()
                    .withPlayers("Picard", "Opponent")
                    .withCardOnBattlefield(1, enterprise, summoningSickness = false)
                    .withCardOnBattlefield(1, "Spined Wurm", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ship = game.findPermanent(enterprise)!!
                val wurm = game.findPermanent("Spined Wurm")!! // power 5
                val bears = game.findPermanent("Grizzly Bears")!! // power 2

                fun station(creature: EntityId) {
                    val result = game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = ship,
                            abilityId = stationAbilityId(),
                            costPayment = AdditionalCostPayment(tappedPermanents = listOf(creature))
                        )
                    )
                    withClue("station activation should succeed: ${result.error}") { result.error shouldBe null }
                    game.resolveStack()
                }

                val libraryBefore = game.librarySize(1)

                station(wurm)
                withClue("first charge counters this turn exile the top card") {
                    game.librarySize(1) shouldBe libraryBefore - 1
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                }

                station(bears)
                withClue("both stations landed: 5 + 2 charge counters") {
                    game.state.getEntity(ship)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) shouldBe 7
                }
                withClue("second batch of charge counters this turn does not trigger again") {
                    game.librarySize(1) shouldBe libraryBefore - 1
                }
                withClue("at 7+ it is an artifact creature") {
                    game.state.projectedState.isCreature(ship) shouldBe true
                }

                withClue("you may play the exiled card this turn") {
                    game.castSpellFromExile(1, "Grizzly Bears").error shouldBe null
                }
            }
        }
    }
}
