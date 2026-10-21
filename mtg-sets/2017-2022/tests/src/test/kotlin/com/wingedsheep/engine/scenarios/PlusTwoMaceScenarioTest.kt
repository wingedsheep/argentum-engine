package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.PlusTwoMace
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * +2 Mace (AFR #1) — {1}{W} Artifact — Equipment.
 *
 * "Equipped creature gets +2/+2.
 *  Equip {3}"
 */
class PlusTwoMaceScenarioTest : FunSpec({

    val projector = StateProjector()

    test("Equip {3} attaches +2 Mace to a creature you control, which gets +2/+2") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PlusTwoMace)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears") // 2/2
        val equipment = driver.putPermanentOnBattlefield(me, "+2 Mace")

        val equipAbilityId = PlusTwoMace.activatedAbilities.single { it.isEquipAbility }.id
        driver.giveColorlessMana(me, 3)
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = equipment,
                abilityId = equipAbilityId,
                targets = listOf(ChosenTarget.Permanent(bear))
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.state.getEntity(equipment)?.get<AttachedToComponent>()?.targetId shouldBe bear
        projector.getProjectedPower(driver.state, bear) shouldBe 2 + 2
        projector.getProjectedToughness(driver.state, bear) shouldBe 2 + 2

    }
})
