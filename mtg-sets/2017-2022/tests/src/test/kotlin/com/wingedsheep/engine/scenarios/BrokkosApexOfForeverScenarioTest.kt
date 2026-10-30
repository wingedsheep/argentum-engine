package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Brokkos, Apex of Forever (IKO #179) — {2}{B}{G}{U} Legendary Creature 6/6.
 *
 *   Mutate {2}{U/B}{G}{G}
 *   Trample
 *   You may cast this card from your graveyard using its mutate ability.
 *
 * The graveyard permission is `MayCastSelfFromZones(GRAVEYARD, castUsing = Keyword.MUTATE)`: from
 * the graveyard the mutate cast is offered and legal, a normal cast is not.
 */
class BrokkosApexOfForeverScenarioTest : ScenarioTestBase() {

    private fun scenarioWithBrokkosInGraveyard() = scenario()
        .withPlayers("P1", "P2")
        .withCardInGraveyard(1, "Brokkos, Apex of Forever")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Forest", 3)
        .withLandsOnBattlefield(1, "Island", 1)
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Brokkos, Apex of Forever") {

            test("mutates from the graveyard onto a non-Human creature") {
                val game = scenarioWithBrokkosInGraveyard()
                val brokkos = game.findCardsInGraveyard(1, "Brokkos, Apex of Forever").single()
                val bears = game.findPermanent("Grizzly Bears")!!

                val offer = game.getLegalActions(1).single {
                    val cast = it.action as? CastSpell
                    cast?.cardId == brokkos && cast.alternativeCostType == AlternativeCostType.MUTATE
                }
                game.execute((offer.action as CastSpell).copy(targets = listOf(ChosenTarget.Permanent(bears))))
                    .error shouldBe null
                game.resolveStack()
                game.answerYesNo(true)   // over
                game.resolveStack()

                withClue("the Bears' permanent is now a 6/6 Brokkos with trample") {
                    game.isInGraveyard(1, "Brokkos, Apex of Forever") shouldBe false
                    game.findPermanent("Brokkos, Apex of Forever") shouldBe bears
                    val projected = game.state.projectedState
                    projected.getPower(bears) shouldBe 6
                    projected.getToughness(bears) shouldBe 6
                    projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                }
            }

            test("can't be cast from the graveyard for its mana cost") {
                val game = scenarioWithBrokkosInGraveyard()
                val brokkos = game.findCardsInGraveyard(1, "Brokkos, Apex of Forever").single()

                withClue("no non-mutate cast of Brokkos is offered from the graveyard") {
                    game.getLegalActions(1).none {
                        val cast = it.action as? CastSpell
                        cast?.cardId == brokkos && cast.alternativeCostType != AlternativeCostType.MUTATE
                    } shouldBe true
                }
                game.execute(CastSpell(game.player1Id, brokkos)).error shouldNotBe null
                game.isInGraveyard(1, "Brokkos, Apex of Forever") shouldBe true
            }
        }
    }
}
