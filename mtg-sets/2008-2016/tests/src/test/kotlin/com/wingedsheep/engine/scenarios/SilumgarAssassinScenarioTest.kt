package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.SilumgarAssassin
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Silumgar Assassin — "Creatures with power greater than this creature's power can't block it.
 * Megamorph {2}{B}. When this creature is turned face up, destroy target creature with power 3 or
 * less an opponent controls."
 *
 * Pins the power comparison as blockers are declared, that the evasion is off while the Assassin
 * is a face-down 2/2 (a face-down permanent has no abilities), and the megamorph flip: +1/+1
 * counter, destroy a power-3 opponent creature, and the 3/2 Assassin now blockable by power 3.
 */
class SilumgarAssassinScenarioTest : FunSpec({

    val bear = card("Assassin Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2; toughness = 2
    }
    val ogre = card("Assassin Test Ogre") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Ogre"
        power = 3; toughness = 3
    }
    val giant = card("Assassin Test Giant") {
        manaCost = "{3}{R}"
        typeLine = "Creature — Giant"
        power = 4; toughness = 4
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(SilumgarAssassin, bear, ogre, giant))
        it.initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.assassin(faceDown: Boolean): EntityId {
        val id = putCreatureOnBattlefield(player1, SilumgarAssassin.name)
        if (faceDown) {
            replaceState(
                state.updateEntity(id) { c ->
                    c.with(FaceDownComponent)
                        .with(FaceDownModeComponent(FaceDownMode.MORPH))
                        .with(FaceDownTurnUp.dataFor(SilumgarAssassin, SilumgarAssassin.name, FaceDownMode.MORPH)!!)
                }
            )
        }
        removeSummoningSickness(id)
        return id
    }

    fun GameTestDriver.attackWith(attacker: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, listOf(attacker), player2).error shouldBe null
        passPriorityUntil(Step.DECLARE_BLOCKERS)
    }

    test("a creature with greater power can't block it; equal power can") {
        val d = driver()
        val assassin = d.assassin(faceDown = false)
        val ogre1 = d.putCreatureOnBattlefield(d.player2, ogre.name)
        val bear1 = d.putCreatureOnBattlefield(d.player2, bear.name)
        d.attackWith(assassin)

        withClue("power 3 is greater than the Assassin's 2") {
            d.declareBlockers(d.player2, mapOf(ogre1 to listOf(assassin))).error shouldNotBe null
        }
        d.declareBlockers(d.player2, mapOf(bear1 to listOf(assassin))).error shouldBe null
    }

    test("the evasion is off while it is a face-down 2/2") {
        val d = driver()
        val assassin = d.assassin(faceDown = true)
        val ogre1 = d.putCreatureOnBattlefield(d.player2, ogre.name)
        d.attackWith(assassin)

        d.declareBlockers(d.player2, mapOf(ogre1 to listOf(assassin))).error shouldBe null
    }

    test("turned face up for megamorph: +1/+1 counter, destroys a power-3 creature, now blockable by power 3") {
        val d = driver()
        val assassin = d.assassin(faceDown = true)
        val ogre1 = d.putCreatureOnBattlefield(d.player2, ogre.name)
        val ogre2 = d.putCreatureOnBattlefield(d.player2, ogre.name)
        val giant1 = d.putCreatureOnBattlefield(d.player2, giant.name)

        d.giveMana(d.player1, Color.BLACK, 1)
        d.giveColorlessMana(d.player1, 2)
        d.submit(TurnFaceUp(d.player1, assassin, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.state.getEntity(assassin)!!.has<FaceDownComponent>() shouldBe false

        val decision = d.pendingDecision as ChooseTargetsDecision
        withClue("a power-4 creature isn't a legal target") {
            decision.legalTargets.values.flatten() shouldNotContain giant1
        }
        decision.legalTargets.values.flatten() shouldContain ogre1
        d.submitTargetSelection(d.player1, listOf(ogre1)).error shouldBe null
        d.bothPass()

        d.state.getBattlefield() shouldNotContain ogre1
        d.state.getBattlefield() shouldContain giant1
        d.state.projectedState.getPower(assassin) shouldBe 3
        d.state.projectedState.getToughness(assassin) shouldBe 2

        d.attackWith(assassin)
        withClue("power 4 is still greater than 3") {
            d.declareBlockers(d.player2, mapOf(giant1 to listOf(assassin))).error shouldNotBe null
        }
        d.declareBlockers(d.player2, mapOf(ogre2 to listOf(assassin))).error shouldBe null
    }
})
