package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.UyoSilentProphet
import com.wingedsheep.mtg.sets.definitions.snc.cards.ErrantStreetArtist
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Errant, Street Artist (SNC) — "{1}{U}, {T}: Copy target spell you control that wasn't cast. You
 * may choose new targets for the copy."
 *
 * A copy put onto the stack isn't cast (CR 707.10), so Errant can copy a copy and never the spell
 * that was cast. Uyo, Silent Prophet makes the uncast copy of a Lightning Bolt.
 */
class ErrantStreetArtistScenarioTest : FunSpec({

    val errantAbility = ErrantStreetArtist.activatedAbilities.single().id
    val uyoAbility = UyoSilentProphet.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ErrantStreetArtist + UyoSilentProphet)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0, startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.activateErrant(errant: EntityId, target: EntityId) = ActivateAbility(
        playerId = player1,
        sourceId = errant,
        abilityId = errantAbility,
        targets = listOf(ChosenTarget.Spell(target)),
        paymentStrategy = PaymentStrategy.FromPool,
    )

    fun GameTestDriver.passUntilTargetChoice() {
        var guard = 0
        while (state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
            bothPass()
            guard++
        }
        (state.pendingDecision is ChooseTargetsDecision) shouldBe true
    }

    test("copies an uncast copy of a spell, and the new copy can be aimed at a new target") {
        val d = driver()
        val p1 = d.player1
        val p2 = d.getOpponent(p1)
        val errant = d.putCreatureOnBattlefield(p1, "Errant, Street Artist")
        val uyo = d.putCreatureOnBattlefield(p1, "Uyo, Silent Prophet")
        val lands = listOf(d.putLandOnBattlefield(p1, "Island"), d.putLandOnBattlefield(p1, "Island"))

        val bolt = d.putCardInHand(p1, "Lightning Bolt")
        d.giveMana(p1, Color.RED, 1)
        d.castSpellWithTargets(p1, bolt, listOf(ChosenTarget.Player(p2)))
        val boltOnStack = d.getTopOfStack()!!

        // Uyo copies the Bolt; the copy keeps the opponent as its target.
        d.giveColorlessMana(p1, 2)
        d.submitSuccess(
            ActivateAbility(
                playerId = p1,
                sourceId = uyo,
                abilityId = uyoAbility,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                costPayment = AdditionalCostPayment(bouncedPermanents = lands),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        d.passUntilTargetChoice()
        d.submitTargetSelection(p1, listOf(p2))
        val uncastCopy = d.getTopOfStack()!!
        withClue("Uyo's copy is a new object on top of the original Bolt") {
            (uncastCopy != boltOnStack) shouldBe true
            d.stackSize shouldBe 2
        }

        // Errant copies Uyo's copy, which wasn't cast, and aims the new copy at its own controller.
        d.giveColorlessMana(p1, 1)
        d.giveMana(p1, Color.BLUE, 1)
        d.submitSuccess(d.activateErrant(errant, uncastCopy))
        d.passUntilTargetChoice()
        d.submitTargetSelection(p1, listOf(p1))

        var guard = 0
        while (d.stackSize > 0 && guard < 20) {
            d.bothPass()
            guard++
        }
        withClue("the Bolt and Uyo's copy hit the opponent; Errant's copy hit its new target") {
            d.getLifeTotal(p2) shouldBe 14
            d.getLifeTotal(p1) shouldBe 17
        }
    }

    test("cannot target a spell that was cast") {
        val d = driver()
        val p1 = d.player1
        val p2 = d.getOpponent(p1)
        val errant = d.putCreatureOnBattlefield(p1, "Errant, Street Artist")

        val bolt = d.putCardInHand(p1, "Lightning Bolt")
        d.giveMana(p1, Color.RED, 1)
        d.castSpellWithTargets(p1, bolt, listOf(ChosenTarget.Player(p2)))
        val boltOnStack = d.getTopOfStack()!!

        d.giveColorlessMana(p1, 1)
        d.giveMana(p1, Color.BLUE, 1)
        d.submitExpectFailure(d.activateErrant(errant, boltOnStack))
        withClue("only the cast Bolt is on the stack") { d.stackSize shouldBe 1 }
    }
})
