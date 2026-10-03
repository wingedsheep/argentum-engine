package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.HopeEnderCoatl
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hope-Ender Coatl (MH3) — {2}{U} Flash, Flying, Devoid 2/2 with
 * "When you cast this spell, counter target spell an opponent controls unless they pay {1}."
 *
 * Player 1 casts Grizzly Bears; player 2 flashes the Coatl in response. The cast trigger resolves
 * before the Coatl and asks the Bears' controller to pay {1}.
 */
class HopeEnderCoatlScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + HopeEnderCoatl)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Player 1 casts Grizzly Bears, player 2 flashes in the Coatl aiming its trigger at the Bears. */
    fun GameTestDriver.flashCoatlOverBears(casterForests: Int): Pair<EntityId, EntityId> {
        val caster = player1
        val coatlController = getOpponent(caster)
        repeat(casterForests) { putLandOnBattlefield(caster, "Forest") }

        val bears = putCardInHand(caster, "Grizzly Bears")
        giveMana(caster, Color.GREEN, 2)
        castSpell(caster, bears).error shouldBe null
        passPriority(caster)

        val coatl = putCardInHand(coatlController, "Hope-Ender Coatl")
        giveMana(coatlController, Color.BLUE, 3)
        castSpell(coatlController, coatl).error shouldBe null

        if (pendingDecision is ChooseTargetsDecision) {
            submitTargetSelection(coatlController, listOf(bears))
        }
        withClue("the cast trigger sits above the Coatl, which sits above the Bears") {
            getStackSpellNames().last() shouldBe "Grizzly Bears"
            state.stack.size shouldBe 3
        }
        bothPass()
        return bears to coatl
    }

    test("counters the opponent's spell when they can't pay, then the Coatl resolves") {
        val d = driver()
        val (bears, _) = d.flashCoatlOverBears(casterForests = 0)

        withClue("no way to pay {1}, so the Bears are countered outright") {
            d.getGraveyard(d.player1) shouldContain bears
        }
        d.getStackSpellNames() shouldContainExactly listOf("Hope-Ender Coatl")
        d.bothPass()
        d.findPermanent(d.player2, "Hope-Ender Coatl").shouldNotBeNull()
    }

    test("counters the spell when its controller declines to pay") {
        val d = driver()
        val (bears, _) = d.flashCoatlOverBears(casterForests = 1)

        val decision = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        withClue("the targeted spell's controller is the one asked to pay") {
            decision.playerId shouldBe d.player1
        }
        d.submitYesNo(d.player1, false)

        d.getGraveyard(d.player1) shouldContain bears
    }

    test("the spell survives when its controller pays {1}") {
        val d = driver()
        d.flashCoatlOverBears(casterForests = 1)

        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true)
        d.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        d.submitManaAutoPayOrDecline(d.player1, autoPay = true)

        d.getStackSpellNames() shouldContainExactly listOf("Hope-Ender Coatl", "Grizzly Bears")
        d.bothPass()
        d.bothPass()
        d.findPermanent(d.player2, "Hope-Ender Coatl").shouldNotBeNull()
        d.findPermanent(d.player1, "Grizzly Bears").shouldNotBeNull()
    }

    test("can't target a spell its own controller cast — with no opponent spell the trigger does nothing") {
        val d = driver()
        val coatlController = d.player1
        val coatl = d.putCardInHand(coatlController, "Hope-Ender Coatl")
        d.giveMana(coatlController, Color.BLUE, 3)
        d.castSpell(coatlController, coatl).error shouldBe null

        withClue("the Coatl itself is not a legal target, so no target prompt and no trigger") {
            (d.pendingDecision is ChooseTargetsDecision) shouldBe false
            d.getStackSpellNames() shouldContainExactly listOf("Hope-Ender Coatl")
            d.state.stack.size shouldBe 1
        }
        d.bothPass()
        d.findPermanent(coatlController, "Hope-Ender Coatl").shouldNotBeNull()
        d.pendingDecision.shouldBeNull()
    }
})
