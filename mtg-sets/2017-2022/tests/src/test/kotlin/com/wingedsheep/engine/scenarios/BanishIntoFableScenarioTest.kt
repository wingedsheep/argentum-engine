package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Banish into Fable — the cast trigger copies once per controlled artifact / enchantment check,
 * only when the spell was cast from hand; the copies may be retargeted.
 */
class BanishIntoFableScenarioTest : ScenarioTestBase() {

    private fun base() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Plains", 3)
        .withLandsOnBattlefield(1, "Island", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Banish into Fable") {
            test("with no artifact or enchantment it is a single bounce plus one Knight") {
                val game = base().withCardInHand(1, "Banish into Fable").build()
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Banish into Fable", giant).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.isInHand(2, "Hill Giant") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.findPermanents("Knight Token").size shouldBe 1
            }

            test("an artifact and an enchantment copy it twice, and copies may be retargeted") {
                val game = base()
                    .withCardInHand(1, "Banish into Fable")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardOnBattlefield(1, "Glorious Anthem")
                    .build()
                val giant = game.findPermanent("Hill Giant")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Banish into Fable", giant).error shouldBe null
                game.resolveStack()

                withClue("first copy (artifact) asks for new targets — move it to the Bears") {
                    game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                    game.selectTargets(listOf(bears)).error shouldBe null
                }
                withClue("second copy (enchantment) asks too — keep the Hill Giant") {
                    game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                    game.skipTargets().error shouldBe null
                }
                game.resolveStack()

                game.isInHand(2, "Grizzly Bears") shouldBe true
                game.isInHand(2, "Hill Giant") shouldBe true
                withClue("two copies resolved; the original's target was gone so it made no Knight") {
                    game.findPermanents("Knight Token").size shouldBe 2
                }
            }

            test("cast from the graveyard it doesn't trigger, even with an artifact") {
                val game = base()
                    .withCardInGraveyard(1, "Banish into Fable")
                    .withCardOnBattlefield(1, "Lier, Disciple of the Drowned")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .build()
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpellFromGraveyard(1, "Banish into Fable", giant).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.isInHand(2, "Hill Giant") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.findPermanents("Knight Token").size shouldBe 1
            }
        }
    }
}
