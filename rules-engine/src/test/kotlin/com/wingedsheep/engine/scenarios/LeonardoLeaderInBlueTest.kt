package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tmt.cards.LeonardoLeaderInBlue
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Leonardo, Leader in Blue (TMT #16) — sneak-paid ETB anthem + activated first strike.
 */
class LeonardoLeaderInBlueTest : FunSpec({

    val firstStrikeAbilityId = LeonardoLeaderInBlue.activatedAbilities.first().id

    test("{1}{W} grants Leonardo first strike until end of turn") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LeonardoLeaderInBlue))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20), startingLife = 20)
        val player = driver.activePlayer!!

        val leo = driver.putCreatureOnBattlefield(player, "Leonardo, Leader in Blue")
        driver.removeSummoningSickness(leo)
        driver.state.projectedState.hasKeyword(leo, Keyword.FIRST_STRIKE) shouldBe false

        driver.giveMana(player, Color.WHITE, 2)
        driver.submit(
            ActivateAbility(playerId = player, sourceId = leo, abilityId = firstStrikeAbilityId)
        ).isSuccess shouldBe true
        driver.bothPass()
        driver.state.projectedState.hasKeyword(leo, Keyword.FIRST_STRIKE) shouldBe true
    }

    test("ETB anthem fires only when the sneak cost was paid") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LeonardoLeaderInBlue))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        // An attacker to return for the sneak cost, plus a back-rank creature to observe the anthem.
        val attacker = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val backRank = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)
        driver.removeSummoningSickness(backRank)
        val leo = driver.putCardInHand(player, "Leonardo, Leader in Blue")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(attacker), opponent)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, emptyMap())
        var guard = 0
        while (driver.state.priorityPlayerId != null && driver.state.priorityPlayerId != player &&
            driver.state.step == Step.DECLARE_BLOCKERS && guard++ < 4
        ) driver.passPriority(driver.state.priorityPlayerId!!)

        driver.giveMana(player, Color.WHITE, 5)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = leo,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SNEAK,
                additionalCostPayment = AdditionalCostPayment(bouncedPermanents = listOf(attacker)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        // Sneak cost paid → back-rank Grizzly Bears gets +2/+0 this turn (2 -> 4 power).
        driver.state.projectedState.getPower(backRank) shouldBe 4
    }
})
