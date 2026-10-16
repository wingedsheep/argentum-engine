package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.combat.GoadedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.RedDeathShipwrecker
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Red Death, Shipwrecker (PIP #116) — {U}{R} Legendary Creature — Crab Mutant 1/3.
 *
 *   "Alluring Eyes — {T}: Goad target creature an opponent controls. That player draws a card.
 *    You add {R}."
 *
 * The ability adds mana but targets, so it is not a mana ability: it goes on the stack and the
 * {R} arrives only on resolution (ruling). On resolution the creature is goaded by Red Death's
 * controller, the creature's controller draws, and the activator gets {R}.
 */
class RedDeathShipwreckerScenarioTest : FunSpec({

    val abilityId = RedDeathShipwrecker.activatedAbilities.first().id

    fun GameTestDriver.pool(player: EntityId): ManaPoolComponent =
        state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    test("uses the stack, then goads the creature, its controller draws, and you add {R}") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RedDeathShipwrecker))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val redDeath = driver.putCreatureOnBattlefield(me, "Red Death, Shipwrecker")
        driver.removeSummoningSickness(redDeath)
        val oppBear = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val oppHandBefore = driver.getHandSize(opp)
        val myHandBefore = driver.getHandSize(me)

        driver.submitSuccess(
            ActivateAbility(me, redDeath, abilityId, listOf(ChosenTarget.Permanent(oppBear)))
        )

        // Not a mana ability: it waits on the stack and no mana has been added yet.
        driver.state.stack.size shouldBe 1
        driver.pool(me).red shouldBe 0

        driver.bothPass()

        driver.state.stack.size shouldBe 0
        driver.state.getEntity(oppBear)?.get<GoadedComponent>()?.goaderIds shouldBe setOf(me)
        driver.getHandSize(opp) shouldBe oppHandBefore + 1
        driver.getHandSize(me) shouldBe myHandBefore
        driver.pool(me).red shouldBe 1
    }
})
