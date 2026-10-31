package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mythos of Illuna (IKO #58) — {2}{U}{U} Sorcery.
 *
 * "Create a token that's a copy of target permanent. If {R}{G} was spent to cast this spell,
 *  instead create a token that's a copy of that permanent, except the token has "When this token
 *  enters, if it's a creature, it fights up to one target creature you don't control.""
 *
 * The bonus hinges on the colours actually paid: two Islands + Mountain + Forest must spend {R}{G}
 * on the generic part; four Islands spend neither.
 */
class MythosOfIllunaScenarioTest : ScenarioTestBase() {

    private fun scenarioWith(vararg lands: Pair<String, Int>) = scenario()
        .withPlayers()
        .withCardInHand(1, "Mythos of Illuna")
        .withCardOnBattlefield(2, "Centaur Courser")    // 3/3 — the copy target
        .withCardOnBattlefield(2, "Grizzly Bears")      // 2/2 — the fight target
        .withCardOnBattlefield(2, "Plains")             // a noncreature copy target
        .apply { lands.forEach { (land, n) -> withLandsOnBattlefield(1, land, n) } }
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("with {R}{G} spent, the copy enters and fights a creature you don't control") {
            val game = scenarioWith("Island" to 2, "Mountain" to 1, "Forest" to 1)
            val courser = game.findPermanent("Centaur Courser")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Mythos of Illuna", courser).error shouldBe null
            game.resolveStack()

            withClue("the token copy is on the battlefield and its enters-trigger asks for a target") {
                game.findPermanents("Centaur Courser") shouldHaveSize 2
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            }
            game.selectTargets(listOf(bears))
            game.resolveStack()

            withClue("the 3/3 token copy fought the 2/2 Bears and survived") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.findPermanents("Centaur Courser") shouldHaveSize 2
            }
        }

        test("without {R}{G} spent, the copy has no fight ability") {
            val game = scenarioWith("Island" to 4)
            val courser = game.findPermanent("Centaur Courser")!!

            game.castSpell(1, "Mythos of Illuna", courser).error shouldBe null
            game.resolveStack()

            game.findPermanents("Centaur Courser") shouldHaveSize 2
            game.hasPendingDecision() shouldBe false
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("a noncreature copy never triggers the fight even with {R}{G} spent") {
            val game = scenarioWith("Island" to 2, "Mountain" to 1, "Forest" to 1)
            val plains = game.findPermanent("Plains")!!

            game.castSpell(1, "Mythos of Illuna", plains).error shouldBe null
            game.resolveStack()

            game.findPermanents("Plains") shouldHaveSize 2
            game.hasPendingDecision() shouldBe false
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
