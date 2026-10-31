package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Marauding Raptor (M20 #150) — {1}{R} Creature — Dinosaur 2/3.
 *
 *   Creature spells you cast cost {1} less to cast.
 *   Whenever another creature you control enters, this creature deals 2 damage to it. If a
 *   Dinosaur is dealt damage this way, this creature gets +2/+0 until end of turn.
 *
 * The rider must read the *damaged creature's* subtype — the Raptor is always a Dinosaur, so a
 * check against the source would pump on every creature. Each creature is cast with exactly one
 * land fewer than its printed cost, so a successful cast also proves the reduction.
 */
class MaraudingRaptorScenarioTest : ScenarioTestBase() {

    init {
        context("Marauding Raptor") {

            test("a non-Dinosaur costs {1} less and takes 2 damage, but the Raptor gets no bonus") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Marauding Raptor")
                    .withCardInHand(1, "Hill Giant") // {3}{R} 3/3
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val raptor = game.findPermanent("Marauding Raptor")!!
                game.castSpell(1, "Hill Giant").error shouldBe null
                game.resolveStack()

                val giant = game.findPermanent("Hill Giant")!!
                withClue("the trigger dealt 2 damage to the entering creature") {
                    game.state.getEntity(giant)?.get<DamageComponent>()?.amount shouldBe 2
                }
                withClue("Hill Giant is a Giant, not a Dinosaur") {
                    game.state.projectedState.getPower(raptor) shouldBe 2
                }
            }

            test("a Dinosaur dealt damage this way gives the Raptor +2/+0") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Marauding Raptor")
                    .withCardInHand(1, "Charging Monstrosaur") // {4}{R} 5/5 Dinosaur
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val raptor = game.findPermanent("Marauding Raptor")!!
                game.castSpell(1, "Charging Monstrosaur").error shouldBe null
                game.resolveStack()

                val monstrosaur = game.findPermanent("Charging Monstrosaur")!!
                game.state.getEntity(monstrosaur)?.get<DamageComponent>()?.amount shouldBe 2
                game.state.projectedState.getPower(raptor) shouldBe 4
                game.state.projectedState.getToughness(raptor) shouldBe 3
            }

            test("a Dinosaur the damage kills still counts") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Marauding Raptor")
                    .withCardInHand(1, "Ravenous Daggertooth") // {2}{G} 3/2 Dinosaur
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val raptor = game.findPermanent("Marauding Raptor")!!
                game.castSpell(1, "Ravenous Daggertooth").error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Ravenous Daggertooth") shouldBe true
                game.state.projectedState.getPower(raptor) shouldBe 4
            }
        }
    }
}
