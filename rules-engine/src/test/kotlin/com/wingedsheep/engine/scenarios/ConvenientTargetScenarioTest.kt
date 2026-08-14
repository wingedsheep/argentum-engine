package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.ConvenientTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Convenient Target (MKM #119) — {R} Enchantment — Aura.
 *
 * "Enchant creature
 *  When this Aura enters, suspect enchanted creature.
 *  Enchanted creature gets +1/+1.
 *  {2}{R}: Return this card from your graveyard to your hand."
 *
 * The claim under test is that the two halves have **different lifetimes**. The +1/+1 is an Aura-bound
 * continuous effect and dies with the Aura; the suspect is a one-shot that permanently designates the
 * creature, and per the printed ruling survives the Aura leaving the battlefield. Modelling suspect as a
 * static ability would pass a naive "is it suspected?" check while silently un-suspecting the creature the
 * moment the Aura died — so the second test is the one that actually distinguishes the implementations.
 */
class ConvenientTargetScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ConvenientTarget)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast the Aura onto [host] and let the enters trigger resolve. Returns the Aura's entity id. */
    fun enchant(driver: GameTestDriver, player: EntityId, host: EntityId): EntityId {
        val card = driver.putCardInHand(player, "Convenient Target")
        driver.giveMana(player, Color.RED, 1)
        driver.castSpellWithTargets(player, card, listOf(ChosenTarget.Permanent(host))).error shouldBe null
        driver.bothPass()
        repeat(2) { if (!driver.isPaused && driver.stackSize > 0) driver.bothPass() }
        return driver.findPermanent(player, "Convenient Target")!!
    }

    test("it suspects and pumps the creature it enchants") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val courser = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        withClue("baseline: a plain 3/3 that is not suspected") {
            projector.project(driver.state).isSuspected(courser) shouldBe false
        }

        enchant(driver, player, courser)

        val projected = projector.project(driver.state)
        withClue("the enters trigger suspected the enchanted creature (CR 701.60a)") {
            projected.isSuspected(courser) shouldBe true
            projected.hasKeyword(courser, Keyword.MENACE) shouldBe true
            projected.cantBlock(courser) shouldBe true
        }
        withClue("and the static half is a straight +1/+1") {
            projected.getPower(courser) shouldBe 4
            projected.getToughness(courser) shouldBe 4
        }
    }

    test("losing the Aura drops the +1/+1 but not the suspected designation") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val courser = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        val aura = enchant(driver, player, courser)
        driver.moveToGraveyard(aura)

        val projected = projector.project(driver.state)
        withClue("the Aura's continuous effect is gone with it") {
            projected.getPower(courser) shouldBe 3
            projected.getToughness(courser) shouldBe 3
        }
        withClue("but suspect was a one-shot designation and outlives the Aura (printed ruling)") {
            projected.isSuspected(courser) shouldBe true
            projected.cantBlock(courser) shouldBe true
        }
    }

    test("{2}{R} buys it back out of the graveyard") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val courser = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        val aura = enchant(driver, player, courser)
        driver.moveToGraveyard(aura)
        driver.getGraveyardCardNames(player) shouldContain "Convenient Target"

        val abilityId = ConvenientTarget.script.activatedAbilities.single().id
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 2)
        driver.submit(
            ActivateAbility(playerId = player, sourceId = aura, abilityId = abilityId)
        ).error shouldBe null
        driver.bothPass()

        withClue("the Aura is back in hand, ready to suspect something else") {
            driver.findCardInHand(player, "Convenient Target") shouldBe aura
            driver.getGraveyardCardNames(player).contains("Convenient Target") shouldBe false
        }
    }
})
