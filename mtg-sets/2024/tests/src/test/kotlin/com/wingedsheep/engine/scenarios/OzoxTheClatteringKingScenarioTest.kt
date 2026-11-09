package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ozox, the Clattering King (J25 #44) — "Ozox can't block. When Ozox dies, create Jumblebones, a
 * legendary 2/1 black Skeleton creature token with 'Jumblebones can't block' and 'When Jumblebones
 * leaves the battlefield, return target card named Ozox, the Clattering King from your graveyard to
 * your hand.'"
 *
 * Pins the token's own leaves-the-battlefield trigger: it fires on a non-dying leave (bounce) as
 * well as on death, after the token has ceased to exist, and targets Ozox in the graveyard.
 */
class OzoxTheClatteringKingScenarioTest : ScenarioTestBase() {

    init {
        fun ScenarioTestBase.TestGame.returnOzox() {
            val ozoxCard = findCardsInGraveyard(1, "Ozox, the Clattering King").single()
            if (getPendingDecision() is ChooseTargetsDecision) {
                selectTargets(listOf(ozoxCard))
            }
            resolveStack()
        }

        fun ScenarioTestBase.TestGame.killOzox() {
            castSpell(2, "Lightning Bolt", findPermanent("Ozox, the Clattering King")!!).error shouldBe null
            resolveStack()
        }

        test("dies into Jumblebones, a legendary 2/1 black Skeleton") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Ozox, the Clattering King")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.killOzox()

            game.isInGraveyard(1, "Ozox, the Clattering King") shouldBe true
            val token = game.findPermanent("Jumblebones")
            withClue("Ozox's death made Jumblebones") { token shouldNotBe null }
            val projected = game.state.projectedState
            withClue("Jumblebones is a legendary 2/1 black Skeleton") {
                projected.isLegendary(token!!) shouldBe true
                projected.hasColor(token, Color.BLACK) shouldBe true
                projected.hasSubtype(token, "Skeleton") shouldBe true
                projected.getPower(token) shouldBe 2
                projected.getToughness(token) shouldBe 1
            }
        }

        test("Jumblebones being bounced returns Ozox from the graveyard to hand") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Ozox, the Clattering King")
                .withCardInHand(2, "Lightning Bolt")
                .withCardInHand(2, "Unsummon")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withLandsOnBattlefield(2, "Island", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.killOzox()
            game.castSpell(2, "Unsummon", game.findPermanent("Jumblebones")!!).error shouldBe null
            game.resolveStack()
            game.returnOzox()

            withClue("the bounced token ceased to exist") { game.findPermanent("Jumblebones") shouldBe null }
            withClue("Ozox went back to its owner's hand") {
                game.isInHand(1, "Ozox, the Clattering King") shouldBe true
                game.isInGraveyard(1, "Ozox, the Clattering King") shouldBe false
            }
        }

        test("Jumblebones dying also returns Ozox to hand") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Ozox, the Clattering King")
                .withCardsInHand(2, "Lightning Bolt", 2)
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.killOzox()
            game.castSpell(2, "Lightning Bolt", game.findPermanent("Jumblebones")!!).error shouldBe null
            game.resolveStack()
            game.returnOzox()

            game.findPermanent("Jumblebones") shouldBe null
            game.isInHand(1, "Ozox, the Clattering King") shouldBe true
        }
    }
}
