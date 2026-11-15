package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import io.kotest.matchers.shouldBe

/**
 * Soulflayer (FRF #84) — {4}{B}{B} 4/4 Demon with delve. "If a creature card with flying was exiled
 * with this creature's delve ability, this creature has flying. The same is true for first strike,
 * double strike, deathtouch, haste, hexproof, indestructible, lifelink, reach, trample, and vigilance."
 */
class SoulflayerScenarioTest : ScenarioTestBase() {
    init {
        fun base(lands: Int) = scenario().withPlayers("P1", "P2")
            .withCardInHand(1, "Soulflayer")
            .withLandsOnBattlefield(1, "Swamp", lands)
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun TestGame.graveyardCard(name: String): EntityId =
            state.getGraveyard(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

        fun TestGame.castSoulflayer(delved: List<EntityId>) = execute(
            CastSpell(
                playerId = player1Id,
                cardId = state.getHand(player1Id).first {
                    state.getEntity(it)?.get<CardComponent>()?.name == "Soulflayer"
                },
                targets = emptyList(),
                paymentStrategy = PaymentStrategy.AutoPay,
                alternativePayment = AlternativePaymentChoice(delvedCards = delved),
            )
        )

        test("gains the listed keywords of the creature cards it delved, and only those") {
            val game = base(lands = 3)
                .withCardInGraveyard(1, "Serra Angel")
                .withCardInGraveyard(1, "Vampire Nighthawk")
                .withCardInGraveyard(1, "Grizzly Bears")
                .build()
            val delved = listOf("Serra Angel", "Vampire Nighthawk", "Grizzly Bears").map { game.graveyardCard(it) }
            game.castSoulflayer(delved).error shouldBe null
            game.resolveStack()

            val soulflayer = game.findPermanent("Soulflayer")!!
            val projected = game.state.projectedState
            projected.hasKeyword(soulflayer, Keyword.FLYING) shouldBe true
            projected.hasKeyword(soulflayer, Keyword.VIGILANCE) shouldBe true
            projected.hasKeyword(soulflayer, Keyword.DEATHTOUCH) shouldBe true
            projected.hasKeyword(soulflayer, Keyword.LIFELINK) shouldBe true
            projected.hasKeyword(soulflayer, Keyword.TRAMPLE) shouldBe false
            projected.hasKeyword(soulflayer, Keyword.FIRST_STRIKE) shouldBe false
        }

        test("a creature card left in the graveyard lends nothing — only delved cards count") {
            val game = base(lands = 6).withCardInGraveyard(1, "Serra Angel").build()
            game.castSoulflayer(emptyList()).error shouldBe null
            game.resolveStack()

            val soulflayer = game.findPermanent("Soulflayer")!!
            game.state.projectedState.hasKeyword(soulflayer, Keyword.FLYING) shouldBe false
            game.isInGraveyard(1, "Serra Angel") shouldBe true
        }

        test("a delved noncreature card grants nothing") {
            val game = base(lands = 5)
                .withCardInGraveyard(1, "Lightning Bolt")
                .build()
            game.castSoulflayer(listOf(game.graveyardCard("Lightning Bolt"))).error shouldBe null
            game.resolveStack()

            val soulflayer = game.findPermanent("Soulflayer")!!
            game.state.projectedState.hasKeyword(soulflayer, Keyword.HASTE) shouldBe false
        }

        test("hexproof from a quality is gained as that variant (ruling)") {
            val game = base(lands = 5).withCardInGraveyard(1, "Knight of Grace").build()
            game.castSoulflayer(listOf(game.graveyardCard("Knight of Grace"))).error shouldBe null
            game.resolveStack()

            val soulflayer = game.findPermanent("Soulflayer")!!
            val projected = game.state.projectedState
            projected.hasKeyword(soulflayer, "HEXPROOF_FROM_BLACK") shouldBe true
            projected.hasKeyword(soulflayer, Keyword.FIRST_STRIKE) shouldBe true
            projected.hasKeyword(soulflayer, Keyword.HEXPROOF) shouldBe false
        }

        test("a Soulflayer that wasn't cast has no delve record") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Soulflayer")
                .withCardInGraveyard(1, "Serra Angel")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val soulflayer = game.findPermanent("Soulflayer")!!
            game.state.projectedState.hasKeyword(soulflayer, Keyword.FLYING) shouldBe false
        }

        test("flickered, it returns as a new object that lost the delved keywords (CR 400.7)") {
            val game = base(lands = 3)
                .withCardInGraveyard(1, "Serra Angel")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withCardInHand(1, "Cloudshift")
                .withLandsOnBattlefield(1, "Plains", 1)
                .build()
            val delved = listOf("Serra Angel", "Grizzly Bears", "Hill Giant").map { game.graveyardCard(it) }
            game.castSoulflayer(delved).error shouldBe null
            game.resolveStack()
            val soulflayer = game.findPermanent("Soulflayer")!!
            game.state.projectedState.hasKeyword(soulflayer, Keyword.FLYING) shouldBe true

            game.castSpell(1, "Cloudshift", soulflayer).error shouldBe null
            game.resolveStack()

            val returned = game.findPermanent("Soulflayer")!!
            game.state.projectedState.hasKeyword(returned, Keyword.FLYING) shouldBe false
            game.state.projectedState.hasKeyword(returned, Keyword.VIGILANCE) shouldBe false
        }
    }
}
