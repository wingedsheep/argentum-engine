package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j25.cards.GildedScuttler
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Gilded Scuttler (J25 #7) — "This creature can't be blocked. When this creature enters, tap target
 * creature an opponent controls and put a stun counter on it."
 */
class GildedScuttlerScenarioTest : FunSpec({

    fun GameTestDriver.stun(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    test("the enters trigger taps an opponent's creature and the stun counter eats its next untap") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + GildedScuttler)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mine = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")

        val card = d.putCardInHand(d.player1, "Gilded Scuttler")
        d.giveMana(d.player1, Color.BLUE, 3)
        d.castSpell(d.player1, card).outcome shouldBe Outcome.Done
        d.bothPass() // Scuttler resolves; enters trigger announces its target

        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        withClue("only creatures an opponent controls are legal") {
            decision.legalTargets.getValue(0) shouldContainExactly listOf(theirs)
        }
        d.submitTargetSelection(d.player1, listOf(theirs)).error shouldBe null
        d.bothPass()

        d.isTapped(theirs) shouldBe true
        d.stun(theirs) shouldBe 1
        d.isTapped(mine) shouldBe false
        d.state.projectedState.hasKeyword(card, AbilityFlag.CANT_BE_BLOCKED) shouldBe true

        d.passPriorityUntil(Step.UPKEEP, d.player2)
        withClue("the untap step removes the stun counter instead of untapping") {
            d.isTapped(theirs) shouldBe true
            d.stun(theirs) shouldBe 0
        }
    }
})
