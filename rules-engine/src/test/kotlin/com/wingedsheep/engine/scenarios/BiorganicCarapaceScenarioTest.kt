package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.BiorganicCarapace
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Biorganic Carapace — {2}{W}{U} Artifact — Equipment
 *
 * "When this Equipment enters, attach it to target creature you control.
 *  Equipped creature gets +2/+2 and has \"Whenever this creature deals combat damage to a player,
 *  draw a card for each modified creature you control.\"
 *  Equip {2}"
 */
class BiorganicCarapaceScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(BiorganicCarapace)
        return driver
    }

    fun GameTestDriver.addPlusCounter(entityId: EntityId, count: Int) {
        replaceState(state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(CounterType.PLUS_ONE_PLUS_ONE, count))
        })
    }

    test("ETB attaches to a target creature you control, granting +2/+2") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser") // 3/3

        projector.getProjectedPower(driver.state, courser) shouldBe 3
        projector.getProjectedToughness(driver.state, courser) shouldBe 3

        val carapace = driver.putCardInHand(me, "Biorganic Carapace")
        driver.giveColorlessMana(me, 2)
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveMana(me, Color.BLUE, 1)
        driver.castSpell(me, carapace)
        driver.bothPass() // resolve the artifact spell -> it enters -> ETB trigger on stack
        driver.bothPass() // resolve ETB trigger -> pauses for target selection

        driver.submitTargetSelection(me, listOf(courser))
        driver.bothPass()

        // Attached to the chosen creature, granting +2/+2.
        val carapaceId = driver.findPermanent(me, "Biorganic Carapace")!!
        driver.state.getEntity(carapaceId)?.get<AttachedToComponent>()?.targetId shouldBe courser
        projector.getProjectedPower(driver.state, courser) shouldBe 5
        projector.getProjectedToughness(driver.state, courser) shouldBe 5
    }

    test("combat damage to a player draws one card per modified creature you control") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Equipped attacker (modified by the Equipment attached to it).
        val attacker = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        driver.removeSummoningSickness(attacker)
        // A second modified creature (modified by a +1/+1 counter).
        val counterCreature = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        driver.addPlusCounter(counterCreature, 1)
        // A third, UNMODIFIED creature — must NOT be counted.
        driver.putCreatureOnBattlefield(me, "Centaur Courser")

        // Attach the Carapace to the attacker.
        val carapace = driver.putCardInHand(me, "Biorganic Carapace")
        driver.giveColorlessMana(me, 2)
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveMana(me, Color.BLUE, 1)
        driver.castSpell(me, carapace)
        driver.bothPass() // resolve spell -> ETB trigger on stack
        driver.bothPass() // resolve ETB trigger -> pauses for target selection
        driver.submitTargetSelection(me, listOf(attacker))
        driver.bothPass()

        val carapaceId = driver.findPermanent(me, "Biorganic Carapace")!!
        driver.state.getEntity(carapaceId)?.get<AttachedToComponent>()?.targetId shouldBe attacker

        val handBefore = driver.getHandSize(me)

        // Attack the opponent; the equipped creature connects for combat damage.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(attacker), defendingPlayer = opp).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(opp).error shouldBe null
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        if (driver.state.pendingDecision != null) {
            driver.confirmCombatDamage()
        }
        // Resolve the granted "deals combat damage to a player" trigger.
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard < 20) {
            driver.bothPass()
            guard++
        }

        // Two modified creatures you control (equipped attacker + counter creature) -> draw 2.
        driver.getHandSize(me) shouldBe handBefore + 2
    }
})
