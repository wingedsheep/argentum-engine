package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.CleavingReaper
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Cleaving Reaper — "Pay 3 life: Return this card from your graveyard to your hand. Activate only
 * if you had an Angel or Berserker enter the battlefield under your control this turn."
 *
 * The restriction is read from the turn's entry log while the card sits in the graveyard.
 */
class CleavingReaperScenarioTest : FunSpec({

    val abilityId = CleavingReaper.activatedAbilities[0].id

    fun setup(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(CleavingReaper)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 30, "Mountain" to 30), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun resolveStack(d: GameTestDriver) {
        var guard = 0
        while (d.stackSize > 0 && guard++ < 10) d.bothPass()
    }

    test("can't be activated when no Angel or Berserker entered this turn") {
        val d = setup(); val me = d.activePlayer!!
        val reaper = d.putCardInGraveyard(me, "Cleaving Reaper")

        d.submit(ActivateAbility(playerId = me, sourceId = reaper, abilityId = abilityId)).error shouldNotBe null
        (reaper in d.getGraveyard(me)) shouldBe true
        d.getLifeTotal(me) shouldBe 20
    }

    test("a non-Angel, non-Berserker creature entering doesn't enable it") {
        val d = setup(); val me = d.activePlayer!!
        val reaper = d.putCardInGraveyard(me, "Cleaving Reaper")
        val guide = d.putCardInHand(me, "Goblin Guide")
        d.giveMana(me, Color.RED, 1)
        d.castSpell(me, guide)
        resolveStack(d)

        d.submit(ActivateAbility(playerId = me, sourceId = reaper, abilityId = abilityId)).error shouldNotBe null
        (reaper in d.getGraveyard(me)) shouldBe true
    }

    test("after an Angel Berserker entered under your control, pay 3 life to return it to hand") {
        val d = setup(); val me = d.activePlayer!!
        val reaper = d.putCardInGraveyard(me, "Cleaving Reaper")
        val other = d.putCardInHand(me, "Cleaving Reaper")
        d.giveMana(me, Color.BLACK, 5)
        d.castSpell(me, other)
        resolveStack(d)

        d.submit(ActivateAbility(playerId = me, sourceId = reaper, abilityId = abilityId)).error shouldBe null
        d.getLifeTotal(me) shouldBe 17
        resolveStack(d)
        (reaper in d.getHand(me)) shouldBe true
    }
})
