package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ody.cards.CephalidColiseum
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Cephalid Coliseum — {T}: Add {U}, 1 damage to you. Threshold — {U}, {T}, Sacrifice: target
 * player draws three cards, then discards three cards; only with seven or more cards in your
 * graveyard.
 */
class CephalidColiseumScenarioTest : FunSpec({

    val manaAbilityId = CephalidColiseum.activatedAbilities[0].id
    val thresholdAbilityId = CephalidColiseum.activatedAbilities[1].id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(CephalidColiseum))
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("mana ability adds blue and deals 1 damage to you") {
        val d = driver()
        val me = d.activePlayer!!
        val coliseum = d.putLandOnBattlefield(me, "Cephalid Coliseum")

        d.submit(ActivateAbility(playerId = me, sourceId = coliseum, abilityId = manaAbilityId))
            .outcome shouldBe Outcome.Done

        d.isTapped(coliseum) shouldBe true
        d.getLifeTotal(me) shouldBe 19
    }

    test("cannot activate threshold ability with six cards in graveyard") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val coliseum = d.putLandOnBattlefield(me, "Cephalid Coliseum")
        repeat(6) { d.putCardInGraveyard(me, "Island") }
        d.giveMana(me, Color.BLUE, 1)

        d.submitExpectFailure(
            ActivateAbility(
                playerId = me,
                sourceId = coliseum,
                abilityId = thresholdAbilityId,
                targets = listOf(ChosenTarget.Player(opp))
            )
        )
        d.isTapped(coliseum) shouldBe false
    }

    test("with threshold, target opponent draws three then discards three of their choice") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val coliseum = d.putLandOnBattlefield(me, "Cephalid Coliseum")
        repeat(7) { d.putCardInGraveyard(me, "Island") }
        d.giveMana(me, Color.BLUE, 1)

        val oppHandBefore = d.getHandSize(opp)
        val oppGraveBefore = d.getGraveyard(opp).size

        d.submit(
            ActivateAbility(
                playerId = me,
                sourceId = coliseum,
                abilityId = thresholdAbilityId,
                targets = listOf(ChosenTarget.Player(opp))
            )
        ).outcome shouldBe Outcome.Done

        // Sacrificed as a cost.
        d.getGraveyardCardNames(me) shouldContain "Cephalid Coliseum"
        d.getLifeTotal(me) shouldBe 20

        d.bothPass()

        val decision = d.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe opp
        d.getHandSize(opp) shouldBe oppHandBefore + 3

        d.submitDecision(opp, CardsSelectedResponse(decision.id, decision.options.take(3)))

        d.getHandSize(opp) shouldBe oppHandBefore
        d.getGraveyard(opp).size shouldBe oppGraveBefore + 3
    }
})
