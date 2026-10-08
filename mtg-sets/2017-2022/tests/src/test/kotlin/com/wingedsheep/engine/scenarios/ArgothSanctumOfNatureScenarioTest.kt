package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Argoth, Sanctum of Nature (BRO #256) — Land.
 *
 *   This land enters tapped unless you control a legendary green creature.
 *
 * Pins both halves of the "unless": a legendary green creature (Titania, Voice of Gaea) lets it
 * enter untapped; a legendary creature that isn't green does not. The Bear + mill ability is
 * exercised by [TitaniaVoiceOfGaeaScenarioTest].
 */
class ArgothSanctumOfNatureScenarioTest : ScenarioTestBase() {

    private fun playArgoth(creature: String?): Boolean {
        var b = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Argoth, Sanctum of Nature")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (creature != null) b = b.withCardOnBattlefield(1, creature)
        val game = b.build()
        val card = game.findCardsInHand(1, "Argoth, Sanctum of Nature").single()
        game.execute(PlayLand(game.player1Id, card)).error shouldBe null
        game.resolveStack()
        val argoth = game.findPermanent("Argoth, Sanctum of Nature")!!
        return game.state.getEntity(argoth)!!.has<TappedComponent>()
    }

    init {
        test("enters tapped with no legendary green creature") {
            playArgoth(null) shouldBe true
        }

        test("enters untapped while you control a legendary green creature") {
            playArgoth("Titania, Voice of Gaea") shouldBe false
        }

        test("a legendary creature that isn't green doesn't count") {
            playArgoth("Barrin, Master Wizard") shouldBe true
        }
    }
}
