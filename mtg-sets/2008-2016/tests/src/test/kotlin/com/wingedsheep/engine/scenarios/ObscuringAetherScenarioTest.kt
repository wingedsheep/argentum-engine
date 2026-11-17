package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.ObscuringAether
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Obscuring Aether (DTK #194) — "Face-down creature spells you cast cost {1} less to cast.
 * {1}{G}: Turn this enchantment face down. (It becomes a 2/2 creature.)"
 *
 * Pins what the script can't prove on its own: turning a *noncreature* permanent face down makes
 * it a 2/2 creature with no other types, it loses its cost reduction while face down, and — having
 * no morph cost — it is never offered a way to turn back face up.
 */
class ObscuringAetherScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(ObscuringAether))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castMorphFaceDownWith(mana: Int): Outcome {
        val me = player1
        val morph = putCardInHand(me, "Morph Test Creature")
        giveColorlessMana(me, mana)
        return submit(
            CastSpell(
                playerId = me,
                cardId = morph,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).outcome
    }

    test("face-down creature spells you cast cost {1} less") {
        val d = createDriver()
        d.putPermanentOnBattlefield(d.player1, "Obscuring Aether")

        d.castMorphFaceDownWith(2) shouldBe Outcome.Done
        d.stackSize shouldBe 1
    }

    test("the activated ability turns it into a face-down 2/2 creature that can't turn face up") {
        val d = createDriver()
        val me = d.player1
        val aether = d.putPermanentOnBattlefield(me, "Obscuring Aether")

        d.giveMana(me, Color.GREEN, 2)
        d.submitSuccess(ActivateAbility(me, aether, ObscuringAether.activatedAbilities.first().id))
        d.bothPass()

        val projected = d.state.projectedState
        withClue("face down: a 2/2 creature and nothing else") {
            projected.isFaceDown(aether) shouldBe true
            projected.isCreature(aether) shouldBe true
            projected.hasType(aether, "ENCHANTMENT") shouldBe false
            projected.getPower(aether) shouldBe 2
            projected.getToughness(aether) shouldBe 2
        }
        withClue("no morph cost, so no turn-face-up action is offered") {
            d.state.getEntity(aether)!!.has<MorphDataComponent>() shouldBe false
            d.legalActions(me).none { (it.action as? TurnFaceUp)?.sourceId == aether } shouldBe true
        }
        withClue("a face-down permanent has no abilities, so the reduction is gone") {
            d.castMorphFaceDownWith(2) shouldNotBe Outcome.Done
        }
    }
})
