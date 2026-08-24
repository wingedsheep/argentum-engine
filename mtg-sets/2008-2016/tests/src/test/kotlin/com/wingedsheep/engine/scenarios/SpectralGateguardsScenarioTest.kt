package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Spectral Gateguards — {4}{W} 2/5 Spirit Soldier with soulbond (CR 702.95) and
 * "As long as this creature is paired with another creature, both creatures have vigilance."
 *
 * The white sibling of Lightning Mauler's haste grant, so the interesting axis is that the payoff
 * really is scoped to the pair: both halves get vigilance, and an unpaired Gateguards gives none.
 */
class SpectralGateguardsScenarioTest : ScenarioTestBase() {

    init {
        context("Spectral Gateguards") {

            test("pairing on its own ETB gives vigilance to both halves") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Spectral Gateguards")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Grizzly Bears has no vigilance of its own") {
                    game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe false
                }

                val cast = game.castSpell(1, "Spectral Gateguards")
                withClue("the cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("soulbond's ETB half asks which unpaired creature to pair with") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectCards(listOf(bears))

                val guards = game.findPermanent("Spectral Gateguards")!!
                withClue("both creatures in the pair have vigilance (CR 702.95b)") {
                    game.state.projectedState.hasKeyword(guards, Keyword.VIGILANCE) shouldBe true
                    game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
                }
            }

            test("declining the pairing grants vigilance to nobody, the Gateguards included") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Spectral Gateguards")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .build()

                game.castSpell(1, "Spectral Gateguards")
                game.resolveStack()
                game.hasPendingDecision() shouldBe true
                game.skipSelection()

                val guards = game.findPermanent("Spectral Gateguards")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("the soulbondPair scope is empty while unpaired, so the static reaches nobody") {
                    game.state.projectedState.hasKeyword(guards, Keyword.VIGILANCE) shouldBe false
                    game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe false
                }
            }

            test("a creature entering later is pairable by soulbond's second half") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Spectral Gateguards")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .build()

                val guards = game.findPermanent("Spectral Gateguards")!!
                game.state.projectedState.hasKeyword(guards, Keyword.VIGILANCE) shouldBe false

                val cast = game.castSpell(1, "Grizzly Bears")
                withClue("the cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                game.hasPendingDecision() shouldBe true
                game.answerYesNo(true)

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("the pair formed and vigilance reaches both") {
                    game.state.projectedState.hasKeyword(guards, Keyword.VIGILANCE) shouldBe true
                    game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
                }
            }
        }
    }
}
