package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Invasion of Theros // Ephara, Ever-Sheltering.
 *
 * Front: entering tutors an Aura, God, or Demigod card to hand. Back: lifelink and indestructible
 * while you control three or more *other* enchantments; draws a card whenever another enchantment
 * you control enters.
 */
class InvasionOfTherosScenarioTest : ScenarioTestBase() {

    init {
        test("front: search finds an Aura, God, or Demigod card — and nothing else") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Theros")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Pacifism")
                .withCardInLibrary(1, "Hercules, Prince of Power")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Theros").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            val pacifism = game.findCardsInLibrary(1, "Pacifism").single()
            val hercules = game.findCardsInLibrary(1, "Hercules, Prince of Power").single()
            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
            decision.options shouldContain pacifism
            decision.options shouldContain hercules
            decision.options shouldNotContain bears

            game.selectCards(listOf(hercules)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Hercules, Prince of Power") shouldBe true
            game.isInHand(1, "Pacifism") shouldBe false
        }

        test("back: Ephara gains lifelink and indestructible with three other enchantments, and draws") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Invasion of Theros")
                .withCardOnBattlefield(1, "Glorious Anthem")
                .withCardOnBattlefield(1, "Glorious Anthem")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Glorious Anthem")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.checkStateBasedActions()
            repeat(2) {
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Invasion of Theros")!!).error shouldBe null
                game.resolveStack()
            }
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            val ephara = game.findPermanent("Ephara, Ever-Sheltering")
            ephara shouldNotBe null
            withClue("two other enchantments aren't enough") {
                game.state.projectedState.hasKeyword(ephara!!, Keyword.LIFELINK) shouldBe false
                game.state.projectedState.hasKeyword(ephara, Keyword.INDESTRUCTIBLE) shouldBe false
            }

            val handBefore = game.handSize(1)
            game.castSpell(1, "Glorious Anthem").error shouldBe null
            game.resolveStack()

            withClue("another enchantment entering draws a card (hand: -1 cast, +1 draw)") {
                game.handSize(1) shouldBe handBefore
            }
            withClue("three other enchantments turn on lifelink and indestructible") {
                game.state.projectedState.hasKeyword(ephara!!, Keyword.LIFELINK) shouldBe true
                game.state.projectedState.hasKeyword(ephara, Keyword.INDESTRUCTIBLE) shouldBe true
            }
        }
    }
}
