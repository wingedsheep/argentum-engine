package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Bruna, the Fading Light.
 *
 * Her reanimation ability triggers on cast, before Bruna resolves, and may target only an Angel
 * or Human creature card in her controller's graveyard.
 */
class BrunaTheFadingLightScenarioTest : ScenarioTestBase() {

    init {
        context("Bruna, the Fading Light") {

            test("cast trigger may return an Angel or Human creature card from your graveyard") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Bruna, the Fading Light")
                    .withCardInGraveyard(1, "Glory Seeker")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 7)
                    .build()

                val cast = game.castSpell(1, "Bruna, the Fading Light")
                withClue("casting Bruna should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }

                game.resolveStack()
                val targetDecision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                val human = game.findCardsInGraveyard(1, "Glory Seeker").single()
                val bear = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                withClue("the Human is legal and the non-Angel, non-Human creature is not") {
                    targetDecision.legalTargets[0].orEmpty() shouldContain human
                    targetDecision.legalTargets[0].orEmpty() shouldNotContain bear
                }

                game.submitDecision(
                    TargetsResponse(targetDecision.id, mapOf(0 to listOf(human))),
                ).error shouldBe null
                game.resolveStack()

                withClue("the cast trigger reanimates the chosen Human before Bruna resolves") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                    game.isOnBattlefield("Bruna, the Fading Light") shouldBe true
                }
            }
        }
    }
}
