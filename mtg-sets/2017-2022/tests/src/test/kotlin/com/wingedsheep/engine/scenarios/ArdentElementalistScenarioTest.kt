package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Ardent Elementalist (MID #128) — "When this creature enters, return target instant or sorcery
 * card from your graveyard to your hand."
 */
class ArdentElementalistScenarioTest : ScenarioTestBase() {

    init {
        test("entering returns an instant or sorcery card from your own graveyard to your hand") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Ardent Elementalist")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInGraveyard(1, "Shock")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(2, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val shock = game.findCardsInGraveyard(1, "Shock").single()

            game.castSpell(1, "Ardent Elementalist").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldNotBeNull() as ChooseTargetsDecision
            withClue("only instants/sorceries in your own graveyard: not the Bears, not the opponent's Bolt") {
                decision.legalTargets[0].shouldNotBeNull() shouldContainExactlyInAnyOrder listOf(shock)
            }
            game.selectTargets(listOf(shock)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Shock") shouldBe true
            game.isInGraveyard(1, "Shock") shouldBe false
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Lightning Bolt") shouldBe true
        }
    }
}
