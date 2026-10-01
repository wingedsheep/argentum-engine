package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.ProdigalSorcerer
import com.wingedsheep.mtg.sets.definitions.one.cards.VeneratedRotpriest
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Venerated Rotpriest (ONE #192) — {G} 1/2 Creature — Phyrexian Druid.
 *
 * "Toxic 1 / Whenever a creature you control becomes the target of a spell, target opponent gets
 *  a poison counter."
 *
 * Pins: a spell targeting one of your creatures poisons the opponent (whoever cast the spell),
 * and a spell targeting an opponent's creature does not trigger it.
 */
class VeneratedRotpriestScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(VeneratedRotpriest)
        driver.registerCard(ProdigalSorcerer)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.poison(player: EntityId): Int =
        state.getEntity(player)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    /** Answer the trigger's "target opponent" prompt if the engine asks rather than auto-picks. */
    fun GameTestDriver.chooseOpponentIfAsked(controller: EntityId, opponent: EntityId) {
        if (pendingDecision != null) {
            submitTargetSelection(controller, listOf(opponent)).error shouldBe null
        }
    }

    test("your own spell targeting a creature you control poisons the opponent") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2
        driver.putCreatureOnBattlefield(me, "Venerated Rotpriest")
        val bears = driver.putCreatureOnBattlefield(me, "Centaur Courser")

        val growth = driver.putCardInHand(me, "Giant Growth")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, growth, listOf(bears)).error shouldBe null
        driver.chooseOpponentIfAsked(me, opp)

        driver.bothPass() // resolve the Rotpriest trigger
        driver.poison(opp) shouldBe 1
        driver.poison(me) shouldBe 0
    }

    test("the Rotpriest itself counts as a creature you control") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2
        val priest = driver.putCreatureOnBattlefield(me, "Venerated Rotpriest")

        val growth = driver.putCardInHand(me, "Giant Growth")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, growth, listOf(priest)).error shouldBe null
        driver.chooseOpponentIfAsked(me, opp)

        driver.bothPass()
        driver.poison(opp) shouldBe 1
    }

    test("an opponent's spell targeting your creature still poisons that opponent") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2
        driver.putCreatureOnBattlefield(opp, "Venerated Rotpriest")
        val theirCreature = driver.putCreatureOnBattlefield(opp, "Centaur Courser")

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(theirCreature)).error shouldBe null
        driver.chooseOpponentIfAsked(opp, me)

        driver.bothPass()
        driver.poison(me) shouldBe 1
        driver.poison(opp) shouldBe 0
    }

    test("targeting an opponent's creature does not trigger it") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2
        driver.putCreatureOnBattlefield(me, "Venerated Rotpriest")
        val theirCreature = driver.putCreatureOnBattlefield(opp, "Centaur Courser")

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(theirCreature)).error shouldBe null
        driver.pendingDecision shouldBe null

        driver.bothPass()
        driver.poison(opp) shouldBe 0
        driver.poison(me) shouldBe 0
    }

    test("an activated ability targeting your creature does not trigger it") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2
        driver.putCreatureOnBattlefield(me, "Venerated Rotpriest")
        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val sorcerer = driver.putCreatureOnBattlefield(me, "Prodigal Sorcerer")
        driver.removeSummoningSickness(sorcerer)

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = sorcerer,
                abilityId = ProdigalSorcerer.activatedAbilities.first().id,
                targets = listOf(ChosenTarget.Permanent(courser)),
            )
        )
        driver.pendingDecision shouldBe null

        driver.bothPass()
        driver.poison(opp) shouldBe 0
    }
})
