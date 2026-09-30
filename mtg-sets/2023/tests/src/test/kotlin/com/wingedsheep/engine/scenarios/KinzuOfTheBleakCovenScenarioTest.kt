package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Kinzu of the Bleak Coven (ONE #406) — {4}{B} 5/4 Legendary Creature — Phyrexian Vampire, flying.
 *
 * "Whenever another nontoken creature you control dies, you may pay 2 life and exile it. If you do,
 *  create a token that's a copy of that creature, except it's 1/1 and has toxic 1."
 *
 * Proof card for token copies keeping numeric keywords: the copied creature's printed toxic 2 lives
 * on a component, not on the copied CardComponent, and the added toxic 1 stacks with it
 * (CR 702.164b) into a total of toxic 3.
 */
class KinzuOfTheBleakCovenScenarioTest : ScenarioTestBase() {

    private fun TestGame.killStalker() {
        val stalker = findPermanent("Branchblight Stalker")!!
        castSpell(1, "Shock", stalker).error shouldBe null
        resolveStack()
    }

    init {
        test("paying 2 life exiles the dead creature and mints a 1/1 copy whose toxic stacks to 3") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Kinzu of the Bleak Coven")
                .withCardOnBattlefield(1, "Branchblight Stalker")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.killStalker()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 18
            withClue("the dead card was exiled") {
                game.isInExile(1, "Branchblight Stalker") shouldBe true
                game.findCardsInGraveyard(1, "Branchblight Stalker").shouldBeEmpty()
            }
            val token = game.findPermanent("Branchblight Stalker")!!
            val projected = game.state.projectedState
            withClue("a 1/1 token copy with toxic 2 (copied) + toxic 1 (added)") {
                game.state.getEntity(token)!!.has<TokenComponent>() shouldBe true
                projected.getPower(token) shouldBe 1
                projected.getToughness(token) shouldBe 1
                // Combat damage reads the one projected TOXIC_<n> total (CombatDamageManager).
                projected.getKeywords(token) shouldContain "TOXIC_3"
            }
        }

        test("declining leaves the card in the graveyard and makes no token") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Kinzu of the Bleak Coven")
                .withCardOnBattlefield(1, "Branchblight Stalker")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.killStalker()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 20
            game.findCardsInGraveyard(1, "Branchblight Stalker").size shouldBe 1
            game.findPermanent("Branchblight Stalker") shouldBe null
        }
    }
}
