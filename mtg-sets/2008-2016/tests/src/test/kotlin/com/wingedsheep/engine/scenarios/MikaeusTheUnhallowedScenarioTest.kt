package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Mikaeus, the Unhallowed — a Human that deals damage to you is destroyed; other non-Human
 * creatures you control get +1/+1 and have undying.
 */
class MikaeusTheUnhallowedScenarioTest : ScenarioTestBase() {
    init {
        context("Mikaeus, the Unhallowed") {
            test("a Human that deals combat damage to you is destroyed") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mikaeus, the Unhallowed", tapped = true)
                    .withCardOnBattlefield(2, "Elite Vanguard")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(3) { builder = builder.withCardInLibrary(1, "Swamp") }
                repeat(3) { builder = builder.withCardInLibrary(2, "Plains") }
                val game = builder.build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Elite Vanguard" to 1)).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 18
                withClue("Elite Vanguard is a Human and dealt damage to Mikaeus's controller") {
                    game.findPermanent("Elite Vanguard") shouldBe null
                    game.isInGraveyard(2, "Elite Vanguard") shouldBe true
                }
            }

            test("other non-Human creatures get +1/+1 and return once through undying") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mikaeus, the Unhallowed")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Elite Vanguard")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Grizzly Bears gets +1/+1; the Human Elite Vanguard doesn't") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 3
                    val vanguard = game.findPermanent("Elite Vanguard")!!
                    game.state.projectedState.getPower(vanguard) shouldBe 2
                }

                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()

                val returned = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                withClue("Undying returns Grizzly Bears with a +1/+1 counter") {
                    game.state.getEntity(returned)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                    game.state.projectedState.getPower(returned) shouldBe 4
                }

                game.castSpell(1, "Lightning Bolt", returned).error shouldBe null
                game.resolveStack()
                withClue("Bolt deals 3 to a 4/4, so it survives") {
                    game.findPermanent("Grizzly Bears").shouldNotBeNull()
                }
                game.castSpell(1, "Shock", returned).error shouldBe null
                game.resolveStack()
                withClue("With a +1/+1 counter on it, undying doesn't return it again") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
