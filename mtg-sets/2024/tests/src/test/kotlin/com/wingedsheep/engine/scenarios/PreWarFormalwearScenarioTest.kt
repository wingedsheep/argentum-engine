package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Pre-War Formalwear (PIP #21) — {2}{W} Artifact — Equipment
 *
 *   When this Equipment enters, return target creature card with mana value 3 or less from your
 *   graveyard to the battlefield and attach this Equipment to it.
 *   Equipped creature gets +2/+2 and has vigilance.
 *   Equip {3}
 *
 * The attach step must find the same card after its graveyard-to-battlefield move, and the target
 * restriction must exclude creature cards with mana value 4 or more.
 */
class PreWarFormalwearScenarioTest : ScenarioTestBase() {

    init {
        context("Pre-War Formalwear") {

            test("returns the targeted creature card and enters attached to it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Pre-War Formalwear")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Pre-War Formalwear").error shouldBe null
                game.resolveStack()
                withClue("the enters trigger asks for a target") { game.hasPendingDecision() shouldBe true }

                val bearsCard = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                game.selectTargets(listOf(bearsCard)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                val bears = game.findPermanent("Grizzly Bears")!!
                val formalwear = game.findPermanent("Pre-War Formalwear")!!
                withClue("the Equipment is attached to the returned creature") {
                    game.state.getEntity(formalwear)?.get<AttachedToComponent>()?.targetId shouldBe bears
                }
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 4
                game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
            }

            test("a creature card with mana value 4 is not a legal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Pre-War Formalwear")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInGraveyard(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Pre-War Formalwear").error shouldBe null
                game.resolveStack()

                withClue("no legal target, so the trigger is removed") {
                    game.hasPendingDecision() shouldBe false
                }
                game.isInGraveyard(1, "Hill Giant") shouldBe true
                val formalwear = game.findPermanent("Pre-War Formalwear")!!
                game.state.getEntity(formalwear)?.get<AttachedToComponent>() shouldBe null
            }
        }
    }
}
