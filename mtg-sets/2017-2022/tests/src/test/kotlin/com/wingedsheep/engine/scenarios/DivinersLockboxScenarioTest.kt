package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m20.cards.DivinersLockbox
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Diviner's Lockbox (M20) — "{1}, {T}: Choose a card name, then reveal the top card of your
 * library. If that card has the chosen name, sacrifice this artifact and draw three cards."
 *
 * The load-bearing claim is the conditional branch reading the name chosen earlier in the same
 * resolution: a match sacrifices the Lockbox and draws three (the revealed card among them), a miss
 * leaves everything in place.
 */
class DivinersLockboxScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + DivinersLockbox)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.activateAndName(lockbox: EntityId, cardName: String) {
        giveColorlessMana(player1, 1)
        submit(
            ActivateAbility(
                playerId = player1,
                sourceId = lockbox,
                abilityId = DivinersLockbox.script.activatedAbilities[0].id
            )
        ).outcome shouldBe Outcome.Done
        bothPass()
        val decision = state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(player1, OptionChosenResponse(decision.id, decision.options.indexOf(cardName)))
            .error shouldBe null
    }

    test("naming the top card sacrifices the Lockbox and draws three cards") {
        val d = driver()
        val lockbox = d.putPermanentOnBattlefield(d.player1, "Diviner's Lockbox")
        val top = d.putCardOnTopOfLibrary(d.player1, "Grizzly Bears")
        val handBefore = d.getHandSize(d.player1)

        d.activateAndName(lockbox, "Grizzly Bears")

        d.getHandSize(d.player1) shouldBe handBefore + 3
        d.getHand(d.player1) shouldContain top
        d.findPermanent(d.player1, "Diviner's Lockbox") shouldBe null
    }

    test("misnaming the top card leaves it on top and keeps the Lockbox") {
        val d = driver()
        val lockbox = d.putPermanentOnBattlefield(d.player1, "Diviner's Lockbox")
        val top = d.putCardOnTopOfLibrary(d.player1, "Grizzly Bears")
        val handBefore = d.getHandSize(d.player1)

        d.activateAndName(lockbox, "Hill Giant")

        d.getHandSize(d.player1) shouldBe handBefore
        d.getHand(d.player1) shouldNotContain top
        d.findPermanent(d.player1, "Diviner's Lockbox") shouldBe lockbox
    }
})
