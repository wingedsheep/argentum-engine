package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.WizardsStaff
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Wizard's Staff (HOB #59) — {1}{U} Artifact — Equipment.
 *
 *   Equipped creature has prowess.
 *   If an ability of equipped creature triggers, that ability triggers an additional time.
 *   Equip Wizard {1}
 *   Equip {3}
 *
 * The doubling is [AdditionalSourceTriggers] scoped by `attachedToBySource()` — the *host*
 * direction of the primitive, where Cloud, Midgar Mercenary uses the attachment direction. The
 * attack test proves the scoping actually fires: Ravenous Skirge's "whenever this creature attacks,
 * it gets +2/+0" resolves twice, so a 1/1 attacks as a 5/1 rather than a 3/1.
 */
class WizardsStaffScenarioTest : FunSpec({

    val equipWizardId = WizardsStaff.activatedAbilities[0].id
    val equipGenericId = WizardsStaff.activatedAbilities[1].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(WizardsStaff)
        return driver
    }

    fun startAtMain(driver: GameTestDriver): EntityId {
        driver.initMirrorMatch(deck = Deck.of("Island" to 40, "Grizzly Bears" to 20), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return player
    }

    test("Equip {3} grants the equipped creature prowess") {
        val driver = createDriver()
        val player = startAtMain(driver)

        val staff = driver.putPermanentOnBattlefield(player, "Wizard's Staff")
        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.giveMana(player, Color.BLUE, 3)

        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = staff,
                abilityId = equipGenericId,
                targets = listOf(ChosenTarget.Permanent(bear))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.state.projectedState.hasKeyword(bear, Keyword.PROWESS) shouldBe true
    }

    test("Equip Wizard {1} only attaches to a Wizard") {
        val driver = createDriver()
        val player = startAtMain(driver)

        val staff = driver.putPermanentOnBattlefield(player, "Wizard's Staff")
        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.giveMana(player, Color.BLUE, 1)

        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = staff,
                abilityId = equipWizardId,
                targets = listOf(ChosenTarget.Permanent(bear))
            )
        ).isSuccess shouldBe false

        driver.state.projectedState.hasKeyword(bear, Keyword.PROWESS) shouldBe false
    }

    test("the equipped creature's attack trigger fires an additional time") {
        val driver = createDriver()
        val player = startAtMain(driver)
        val opponent = driver.getOpponent(player)

        val staff = driver.putPermanentOnBattlefield(player, "Wizard's Staff")
        val skirge = driver.putCreatureOnBattlefield(player, "Ravenous Skirge")
        driver.removeSummoningSickness(skirge)
        driver.giveMana(player, Color.BLUE, 3)

        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = staff,
                abilityId = equipGenericId,
                targets = listOf(ChosenTarget.Permanent(skirge))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(skirge), opponent).isSuccess shouldBe true

        // Resolve both copies of "whenever this creature attacks, it gets +2/+0".
        repeat(4) { if (driver.stackSize > 0) driver.bothPass() }

        // Base 1/1, doubled +2/+0 → 5/1. Without the Staff it would be 3/1.
        driver.state.projectedState.getPower(skirge) shouldBe 5
    }
})
