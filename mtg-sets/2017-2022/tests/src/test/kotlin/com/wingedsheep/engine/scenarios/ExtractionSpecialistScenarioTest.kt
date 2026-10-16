package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Extraction Specialist (SNC #12).
 *
 * Its enters trigger reanimates a creature card with mana value 2 or less, and that creature can't
 * attack or block for as long as you control the Specialist (`Duration.WhileYouControlSource`). The
 * restriction must land on the creature the pipeline actually returned, and must lift the moment the
 * Specialist leaves the battlefield (ruling, 2022-04-29).
 */
class ExtractionSpecialistScenarioTest : ScenarioTestBase() {

    init {
        test("returns a mana value 2 or less creature that can't attack or block until the Specialist leaves") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Extraction Specialist")
                .withCardInHand(1, "Unsummon")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardInGraveyard(1, "Grizzly Bears") // MV 2
                .withCardInGraveyard(1, "Centaur Courser") // MV 3
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val courser = game.findCardsInGraveyard(1, "Centaur Courser").single()

            game.castSpell(1, "Extraction Specialist").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            val legal = decision.legalTargets[0].orEmpty()
            legal shouldContain bears
            legal shouldNotContain courser

            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Centaur Courser") shouldBe true
            val returned = game.findPermanent("Grizzly Bears")!!
            val specialist = game.findPermanent("Extraction Specialist")
            specialist shouldNotBe null

            withClue("the returned creature can't attack or block while you control the Specialist") {
                game.state.projectedState.cantAttack(returned) shouldBe true
                game.state.projectedState.cantBlock(returned) shouldBe true
            }
            withClue("the Specialist itself is unrestricted") {
                game.state.projectedState.cantAttack(specialist!!) shouldBe false
                game.state.projectedState.cantBlock(specialist) shouldBe false
            }

            game.castSpell(1, "Unsummon", specialist!!).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Extraction Specialist") shouldBe true

            withClue("once the Specialist leaves, the returned creature attacks and blocks as normal") {
                game.state.projectedState.cantAttack(returned) shouldBe false
                game.state.projectedState.cantBlock(returned) shouldBe false
            }
        }
    }
}
