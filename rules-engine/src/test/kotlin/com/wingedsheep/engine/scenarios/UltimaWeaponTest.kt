package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.UltimaWeapon
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ultima Weapon — {7} Legendary Artifact — Equipment
 *
 * "Whenever equipped creature attacks, destroy target creature an opponent controls.
 *  Equipped creature gets +7/+7.
 *  Equip {7}"
 */
class UltimaWeaponTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(UltimaWeapon)
        return driver
    }

    test("equip gives the equipped creature +7/+7") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser") // 3/3
        val weapon = driver.putPermanentOnBattlefield(me, "Ultima Weapon")
        val equipId = UltimaWeapon.activatedAbilities.first().id

        projector.getProjectedPower(driver.state, courser) shouldBe 3
        projector.getProjectedToughness(driver.state, courser) shouldBe 3

        driver.giveColorlessMana(me, 7)
        driver.submit(
            ActivateAbility(me, weapon, equipId, targets = listOf(ChosenTarget.Permanent(courser)))
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.state.getEntity(weapon)?.get<AttachedToComponent>()?.targetId shouldBe courser
        projector.getProjectedPower(driver.state, courser) shouldBe 10
        projector.getProjectedToughness(driver.state, courser) shouldBe 10
    }

    test("equipped creature attacking destroys a target opponent creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val attacker = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        driver.removeSummoningSickness(attacker)
        val weapon = driver.putPermanentOnBattlefield(me, "Ultima Weapon")
        val equipId = UltimaWeapon.activatedAbilities.first().id

        // Equip it onto the attacker.
        driver.giveColorlessMana(me, 7)
        driver.submit(
            ActivateAbility(me, weapon, equipId, targets = listOf(ChosenTarget.Permanent(attacker)))
        ).isSuccess shouldBe true
        driver.bothPass()

        val victim = driver.putCreatureOnBattlefield(opp, "Glory Seeker")
        driver.findPermanent(opp, "Glory Seeker") shouldBe victim

        // Attack — the equipped-creature-attacks trigger destroys a target opponent creature.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(attacker), opp)
        driver.submitTargetSelection(me, listOf(victim))
        driver.bothPass()

        driver.findPermanent(opp, "Glory Seeker") shouldBe null
        driver.getGraveyard(opp).contains(victim) shouldBe true
    }
})
