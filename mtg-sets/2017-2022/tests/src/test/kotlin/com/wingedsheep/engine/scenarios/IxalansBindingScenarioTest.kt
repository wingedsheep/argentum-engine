package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ixalan's Binding (XLN #17): the enters trigger exiles an opponent's nonland permanent until the
 * Binding leaves (a duration move that links the card to the Binding's exile pile), opponents can't
 * cast spells sharing the exiled card's name, and the card comes back the moment the Binding leaves.
 */
class IxalansBindingScenarioTest : ScenarioTestBase() {

    init {
        test("exiles the permanent, locks its name for opponents, and returns it when the Binding leaves") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Ixalan's Binding")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(2, "Ambush Viper")
                .withCardInHand(2, "Ambush Viper")
                .withCardInHand(2, "Disenchant")
                .withLandsOnBattlefield(2, "Forest", 2)
                .withLandsOnBattlefield(2, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val viper = game.findPermanent("Ambush Viper").shouldNotBeNull()
            game.castSpell(1, "Ixalan's Binding").error shouldBe null
            game.resolveStack() // the Binding resolves; its enters trigger asks for a target
            if (game.state.pendingDecision != null) game.selectTargets(listOf(viper)).error shouldBe null
            game.resolveStack()

            withClue("the Viper is exiled") {
                game.findPermanent("Ambush Viper") shouldBe null
                game.isInExile(2, "Ambush Viper") shouldBe true
            }

            game.passPriority() // the active player passes; the opponent gets priority
            withClue("an opponent can't cast a spell with the exiled card's name") {
                game.castSpell(2, "Ambush Viper").error shouldNotBe null
            }

            val binding = game.findPermanent("Ixalan's Binding").shouldNotBeNull()
            game.castSpell(2, "Disenchant", binding).error shouldBe null
            game.resolveStack()

            withClue("the exiled card returns to the battlefield as soon as the Binding leaves") {
                game.findPermanent("Ixalan's Binding") shouldBe null
                game.isInExile(2, "Ambush Viper") shouldBe false
                game.findPermanent("Ambush Viper") shouldNotBe null
            }

            if (game.state.priorityPlayerId == game.player1Id) game.passPriority()
            withClue("with the Binding gone, the name is castable again") {
                game.castSpell(2, "Ambush Viper").error shouldBe null
            }
        }
    }
}
