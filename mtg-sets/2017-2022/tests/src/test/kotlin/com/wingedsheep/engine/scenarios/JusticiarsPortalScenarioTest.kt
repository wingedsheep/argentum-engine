package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class JusticiarsPortalScenarioTest : ScenarioTestBase() {
    init {
        test("blinks a creature you control; it returns untapped with first strike") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withCardInHand(1, "Justiciar's Portal")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe false

            game.castSpell(1, "Justiciar's Portal", bears).error shouldBe null
            game.resolveStack()

            val returned = game.findPermanent("Grizzly Bears")
            returned shouldNotBe null
            val entity = game.state.getEntity(returned!!)!!
            withClue("the returned creature is a new object: untapped, summoning sick") {
                entity.has<TappedComponent>() shouldBe false
                entity.has<SummoningSicknessComponent>() shouldBe true
            }
            withClue("the returned creature gains first strike") {
                game.state.projectedState.hasKeyword(returned, Keyword.FIRST_STRIKE) shouldBe true
            }
            game.isInGraveyard(1, "Justiciar's Portal") shouldBe true
        }

        test("first strike lasts only until end of turn") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Justiciar's Portal")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Justiciar's Portal", bears).error shouldBe null
            game.resolveStack()
            val returned = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.hasKeyword(returned, Keyword.FIRST_STRIKE) shouldBe true

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("first strike expires at cleanup") {
                game.state.projectedState.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.FIRST_STRIKE) shouldBe false
            }
        }

        test("cannot target a creature an opponent controls") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Justiciar's Portal")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val theirs = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Justiciar's Portal", theirs).error shouldNotBe null
        }
    }
}
