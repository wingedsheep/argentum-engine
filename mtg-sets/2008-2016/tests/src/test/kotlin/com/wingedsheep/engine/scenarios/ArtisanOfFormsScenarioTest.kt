package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Artisan of Forms (THS #40) — {1}{U} Creature — Human Wizard 1/1.
 *
 *   Heroic — Whenever you cast a spell that targets this creature, you may have this creature
 *   become a copy of target creature, except it has this ability.
 *
 * The rules point worth proving: once it is a copy, the heroic trigger it keeps is the stamped
 * copiable one, and that retained trigger still fires on the next spell that targets it.
 */
class ArtisanOfFormsScenarioTest : ScenarioTestBase() {

    private fun TestGame.nameOf(id: EntityId) = state.getEntity(id)!!.get<CardComponent>()!!.name

    private fun TestGame.growTargetingAndCopy(artisan: EntityId, copyTarget: EntityId, accept: Boolean) {
        castSpell(1, "Giant Growth", artisan).error shouldBe null
        selectTargets(listOf(copyTarget)).error shouldBe null
        resolveStack()
        answerYesNo(accept).error shouldBe null
        resolveStack()
    }

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Artisan of Forms")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInHand(1, "Giant Growth")
        .withCardInHand(1, "Giant Growth")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("heroic copies the target creature, and the retained heroic re-copies on the next targeting spell") {
            val game = board()
            val artisan = game.findPermanent("Artisan of Forms")!!
            val giant = game.findPermanent("Hill Giant")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.growTargetingAndCopy(artisan, giant, accept = true)
            game.nameOf(artisan) shouldBe "Hill Giant"
            game.state.projectedState.getPower(artisan) shouldBe 6 // 3/3 copy + Giant Growth

            game.growTargetingAndCopy(artisan, bears, accept = true)
            game.nameOf(artisan) shouldBe "Grizzly Bears"
            game.state.projectedState.getPower(artisan) shouldBe 8 // 2/2 copy + two Giant Growths
            game.state.projectedState.getToughness(artisan) shouldBe 8
        }

        test("declining the copy leaves it as Artisan of Forms") {
            val game = board()
            val artisan = game.findPermanent("Artisan of Forms")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.growTargetingAndCopy(artisan, giant, accept = false)
            game.nameOf(artisan) shouldBe "Artisan of Forms"
            game.state.projectedState.getPower(artisan) shouldBe 4
        }
    }
}
