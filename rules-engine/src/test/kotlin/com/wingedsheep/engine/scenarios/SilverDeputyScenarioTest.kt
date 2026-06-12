package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.SilverDeputy
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Silver Deputy (OTJ #248) — {2} Artifact Creature — Mercenary, 1/2.
 *
 * "When this creature enters, you may search your library for a basic land card or a Desert
 *  card, reveal it, then shuffle and put it on top.
 *  {T}: Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery."
 *
 * Verifies the optional ETB search puts the found land on top of the library, and the tap
 * ability pumps a creature you control.
 */
class SilverDeputyScenarioTest : FunSpec({

    val pumpAbilityId = SilverDeputy.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("ETB may-search puts a basic land on top of the library") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val forest = driver.putCardOnTopOfLibrary(player, "Forest")
        // A non-land buried below so the searched land's placement on top is observable.
        driver.putCardOnTopOfLibrary(player, "Grizzly Bears")

        // Cast from hand so the enters-the-battlefield trigger fires.
        val deputyCard = driver.putCardInHand(player, "Silver Deputy")
        driver.giveMana(player, Color.GREEN, 2)
        driver.castSpell(player, deputyCard).isSuccess shouldBe true
        driver.bothPass() // resolve the creature spell -> it enters, ETB trigger goes on stack
        driver.bothPass() // resolve the ETB trigger

        // MayEffect prompts yes/no first; accept.
        driver.submitYesNo(player, true)

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(player, listOf(forest))
        driver.isPaused shouldBe false

        // The chosen land ends up on top of the library.
        val library = driver.state.getZone(ZoneKey(player, Zone.LIBRARY))
        library.first() shouldBe forest
    }

    test("tap ability gives a creature you control +1/+0 until end of turn") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        // Place directly on the battlefield (no ETB trigger) so we stay in the main phase.
        val deputy = driver.putCreatureOnBattlefield(player, "Silver Deputy")
        driver.removeSummoningSickness(deputy)

        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.state.projectedState.getPower(bear) shouldBe 2

        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = deputy,
                abilityId = pumpAbilityId,
                targets = listOf(ChosenTarget.Permanent(bear))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.isTapped(deputy) shouldBe true
        driver.state.projectedState.getPower(bear) shouldBe 3
        driver.state.projectedState.getToughness(bear) shouldBe 2
    }
})
