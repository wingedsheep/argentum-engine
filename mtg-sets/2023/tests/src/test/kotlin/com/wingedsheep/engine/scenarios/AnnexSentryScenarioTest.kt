package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Annex Sentry (ONE #2) — {2}{W} 1/4 Phyrexian Cleric artifact creature, toxic 1.
 * "When this creature enters, exile target artifact or creature an opponent controls with
 *  mana value 3 or less until this creature leaves the battlefield."
 */
class AnnexSentryScenarioTest : ScenarioTestBase() {
    init {
        test("exiles an opponent's cheap creature and returns it when the Sentry leaves") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Annex Sentry")
                .withCardInHand(1, "Murder")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardOnBattlefield(2, "Glory Seeker")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Ornithopter")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val seeker = game.findPermanent("Glory Seeker")!!
            val ornithopter = game.findPermanent("Ornithopter")!!

            game.castSpell(1, "Annex Sentry").error shouldBe null
            game.resolveStack()

            val sentry = game.findPermanent("Annex Sentry")!!
            game.state.projectedState.hasKeyword(sentry, Keyword.TOXIC) shouldBe true

            val decision = game.state.pendingDecision
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            withClue("only opponent's artifacts/creatures with mana value 3 or less are legal") {
                decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(seeker, ornithopter)
            }

            game.selectTargets(listOf(seeker)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Glory Seeker") shouldBe false
            game.state.getExile(game.player2Id).map {
                game.state.getEntity(it)?.get<CardComponent>()?.name
            } shouldContain "Glory Seeker"

            game.castSpell(1, "Murder", sentry).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Annex Sentry") shouldBe false
            withClue("Glory Seeker returns to its owner when Annex Sentry leaves") {
                val returned = game.findPermanent("Glory Seeker")
                (returned != null) shouldBe true
                game.state.projectedState.getController(returned!!) shouldBe game.player2Id
            }
        }
    }
}
