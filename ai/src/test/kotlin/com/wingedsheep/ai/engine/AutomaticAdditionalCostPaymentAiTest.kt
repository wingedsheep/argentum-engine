package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AutomaticAdditionalCostPaymentAiTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40))
    }

    test("AI materializes each enumerated spell additional-cost selection") {
        val driver = driver()
        val player = driver.activePlayer!!
        val spell = driver.putCardInHand(player, "Grizzly Bears")
        val candidates = List(8) { driver.putCardInHand(player, "Forest") }
        val ai = AIPlayer.create(driver.cardRegistry, player)

        fun choose(info: AdditionalCostData): CastSpell {
            val legal = LegalAction(
                action = CastSpell(player, spell),
                actionType = "CastSpell",
                description = "test",
                additionalCostInfo = info,
            )
            return ai.chooseFrom(driver.state, listOf(legal)).action as CastSpell
        }

        choose(AdditionalCostData("behold", "Behold", validBeholdTargets = candidates, beholdCount = 2))
            .additionalCostPayment?.beheldCards shouldBe candidates.take(2)
        choose(AdditionalCostData("tap", "TapPermanents", validTapTargets = candidates, tapCount = 2))
            .additionalCostPayment?.tappedPermanents shouldBe candidates.take(2)
        choose(AdditionalCostData("discard", "DiscardCard", validDiscardTargets = candidates, discardCount = 2))
            .additionalCostPayment?.discardedCards shouldBe candidates.take(2)
        choose(AdditionalCostData("sacrifice", "SacrificePermanent", validSacrificeTargets = candidates, sacrificeCount = 2))
            .additionalCostPayment?.sacrificedPermanents shouldBe candidates.take(2)
        choose(AdditionalCostData("bounce", "BouncePermanent", validBounceTargets = candidates, bounceCount = 2))
            .additionalCostPayment?.bouncedPermanents shouldBe candidates.take(2)
        choose(AdditionalCostData("exile", "ExileFromGraveyard", validExileTargets = candidates, exileMinCount = 2))
            .additionalCostPayment?.exiledCards shouldBe candidates.take(2)
    }

    test("AI materializes enumerated costs for activated abilities") {
        val driver = driver()
        val player = driver.activePlayer!!
        val source = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val fodder = driver.putCreatureOnBattlefield(player, "Hill Giant")
        val legal = LegalAction(
            action = ActivateAbility(player, source, AbilityId("test")),
            actionType = "ActivateAbility",
            description = "test",
            additionalCostInfo = AdditionalCostData(
                description = "tap a permanent",
                costType = "TapPermanents",
                validTapTargets = listOf(fodder),
                tapCount = 1,
            ),
        )

        val chosen = AIPlayer.create(driver.cardRegistry, player)
            .chooseFrom(driver.state, listOf(legal)).action as ActivateAbility
        chosen.costPayment?.tappedPermanents shouldBe listOf(fodder)
    }
})
