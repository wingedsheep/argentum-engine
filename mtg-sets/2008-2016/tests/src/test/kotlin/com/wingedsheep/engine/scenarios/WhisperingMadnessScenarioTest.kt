package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Whispering Madness (GTC #207) — {2}{U}{B} Sorcery.
 *
 * "Each player discards their hand, then draws cards equal to the greatest number of cards a
 * player discarded this way. Cipher"
 *
 * The wheel runs first, then the caster may encode the card on a creature they control (the
 * ruling: you draw before choosing). When that creature deals combat damage to a player, its
 * controller may cast a free copy — a second wheel — while the card stays encoded in exile.
 */
class WhisperingMadnessScenarioTest : ScenarioTestBase() {

    private fun ScenarioBuilder.withLibraries(n: Int): ScenarioBuilder {
        var b = this
        repeat(n) { b = b.withCardInLibrary(1, "Island").withCardInLibrary(2, "Island") }
        return b
    }

    private fun madnessGame() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Whispering Madness")
        .withCardInHand(1, "Grizzly Bears")
        .withCardInHand(1, "Grizzly Bears")
        .withCardInHand(2, "Island")
        .withCardInHand(2, "Island")
        .withCardInHand(2, "Island")
        .withCardInHand(2, "Island")
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withLandsOnBattlefield(1, "Island", 2)
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withLibraries(12)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("wheels every hand to the largest discard, then encodes on a creature instead of the graveyard") {
            val game = madnessGame()
            game.castSpell(1, "Whispering Madness").error shouldBe null
            game.resolveStack()

            withClue("Alice discarded 2 and Bob 4, so each draws 4") {
                game.handSize(1) shouldBe 4
                game.handSize(2) shouldBe 4
            }
            game.selectCards(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null

            game.isInExile(1, "Whispering Madness") shouldBe true
            game.findCardsInGraveyard(1, "Whispering Madness").size shouldBe 0
        }

        test("combat damage from the encoded creature casts a free copy, wheeling again") {
            val game = madnessGame()
            game.castSpell(1, "Whispering Madness")
            game.resolveStack()
            game.selectCards(listOf(game.findPermanent("Grizzly Bears")!!))
            val aliceGraveyard = game.graveyardSize(1)
            val bobGraveyard = game.graveyardSize(2)

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            withClue("the copy discarded both four-card hands and drew four each") {
                game.graveyardSize(1) shouldBe aliceGraveyard + 4
                game.graveyardSize(2) shouldBe bobGraveyard + 4
                game.handSize(1) shouldBe 4
                game.handSize(2) shouldBe 4
            }
            withClue("the original stays encoded in exile; the copy left nothing behind") {
                game.isInExile(1, "Whispering Madness") shouldBe true
                game.findCardsInGraveyard(1, "Whispering Madness").size shouldBe 0
            }
        }
    }
}
