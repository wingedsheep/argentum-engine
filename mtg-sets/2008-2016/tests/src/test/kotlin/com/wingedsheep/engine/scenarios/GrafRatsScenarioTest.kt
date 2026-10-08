package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.MeldedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Graf Rats (EMN) — "At the beginning of combat on your turn, if you both own and control this
 * creature and a creature named Midnight Scavengers, exile them, then meld them into Chittering
 * Host." (CR 701.42)
 */
class GrafRatsScenarioTest : ScenarioTestBase() {

    init {
        test("at beginning of combat with Midnight Scavengers, the pair melds into Chittering Host") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Graf Rats")
                .withCardOnBattlefield(1, "Midnight Scavengers")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val rats = game.findPermanent("Graf Rats")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()

            val host = game.findPermanent("Chittering Host").shouldNotBeNull()
            withClue("Graf Rats' entity is the melded permanent") { host shouldBe rats }
            game.findPermanent("Graf Rats") shouldBe null
            game.findPermanent("Midnight Scavengers") shouldBe null
            game.isInExile(1, "Graf Rats") shouldBe false
            game.isInExile(1, "Midnight Scavengers") shouldBe false
            game.state.getEntity(host)!!.has<MeldedComponent>() shouldBe true
            game.state.projectedState.getPower(host) shouldBe 5
            withClue("Chittering Host entered: its ETB gave other creatures +1/+0") {
                game.state.projectedState.getPower(bears) shouldBe 3
            }
        }

        test("without Midnight Scavengers, nothing happens at beginning of combat") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Graf Rats")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()

            game.findPermanent("Graf Rats").shouldNotBeNull()
            game.findPermanent("Chittering Host") shouldBe null
            game.isInExile(1, "Graf Rats") shouldBe false
        }
    }
}
