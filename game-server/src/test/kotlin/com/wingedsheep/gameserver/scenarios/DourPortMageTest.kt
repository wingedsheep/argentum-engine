package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class DourPortMageTest : ScenarioTestBase() {

    init {
        test("Dour Port-Mage draws a card when its ability bounces a creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Dour Port-Mage")
                .withCardOnBattlefield(1, "Glory Seeker")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            val handSizeBefore = game.handSize(1)

            // Activate Dour Port-Mage's ability targeting Glory Seeker
            val portMageId = game.findPermanent("Dour Port-Mage")!!
            val glorySeekerId = game.findPermanent("Glory Seeker")!!
            val cardDef = cardRegistry.getCard("Dour Port-Mage")!!
            val ability = cardDef.script.activatedAbilities.first()

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = portMageId,
                    abilityId = ability.id,
                    targets = listOf(ChosenTarget.Permanent(glorySeekerId))
                )
            )

            // Resolve the activated ability (bounce Glory Seeker)
            game.resolveStack()

            // The triggered ability fires (creature left without dying) → draw a card
            game.resolveStack()

            game.isOnBattlefield("Glory Seeker") shouldBe false
            game.isInHand(1, "Glory Seeker") shouldBe true

            // +1 Glory Seeker returned to hand, +1 drawn card
            game.handSize(1) shouldBe handSizeBefore + 2
        }

        test("Dour Port-Mage does not trigger when a creature dies") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Dour Port-Mage")
                .withCardOnBattlefield(1, "Glory Seeker")
                .withCardInHand(1, "Fell")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            val handSizeBefore = game.handSize(1)

            // Cast Fell to destroy Glory Seeker (it dies → goes to graveyard)
            val glorySeekerId = game.findPermanent("Glory Seeker")!!
            game.castSpell(1, "Fell", glorySeekerId)
            game.resolveStack()

            // Glory Seeker died (went to graveyard), not "left without dying"
            game.isInGraveyard(1, "Glory Seeker") shouldBe true

            // Should NOT have drawn a card (Fell left hand = -1)
            game.handSize(1) shouldBe handSizeBefore - 1
        }
    }
}
