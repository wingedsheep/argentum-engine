package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.AirtightAlibi
import com.wingedsheep.mtg.sets.definitions.mkm.cards.ConvenientTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Airtight Alibi (MKM #149) — {2}{G} Enchantment — Aura.
 *
 * "Flash
 *  Enchant creature
 *  When this Aura enters, untap enchanted creature. It gains hexproof until end of turn. If it's
 *  suspected, it's no longer suspected.
 *  Enchanted creature gets +2/+2 and can't become suspected."
 *
 * Two independent claims, and the second is the one worth a test. The enters trigger takes an
 * *existing* suspect off; the static stops a *new* one attaching. Neither implies the other, and
 * only the static needed new engine vocabulary.
 *
 * The load-bearing assertion is that "can't become suspected" suppresses **all three** halves of
 * suspect — the designation, the menace and the "can't block". Suspect used to be a composite of
 * three effects, where a prohibition could only be checked in the designation's executor; that
 * would have left the enchanted creature un-suspected but still carrying menace and unable to
 * block, i.e. strictly worse off than an unenchanted creature. Collapsing suspect into one effect
 * with one executor is what makes that state unrepresentable, so the menace/can't-block assertions
 * below are the regression guard for the collapse, not incidental detail.
 *
 * Convenient Target (also MKM, "when this Aura enters, suspect enchanted creature") is the
 * suspecting effect throughout — an Aura rather than an instant so no control change or extra
 * duration muddies what is being measured.
 */
class AirtightAlibiScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(AirtightAlibi)
        driver.registerCard(ConvenientTarget)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast [auraName] onto [host], let its enters trigger resolve, and return the Aura's id. */
    fun enchant(driver: GameTestDriver, player: EntityId, host: EntityId, auraName: String): EntityId {
        val card = driver.putCardInHand(player, auraName)
        driver.giveMana(player, Color.GREEN, 1)
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 2)
        driver.castSpellWithTargets(player, card, listOf(ChosenTarget.Permanent(host))).error shouldBe null
        driver.bothPass()
        repeat(2) { if (!driver.isPaused && driver.stackSize > 0) driver.bothPass() }
        return driver.findPermanent(player, auraName)!!
    }

    test("the enters trigger untaps, grants hexproof, and clears an existing suspect") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val courser = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        // Suspect it first, then tap it — the state Airtight Alibi is printed to answer.
        enchant(driver, player, courser, "Convenient Target")
        driver.tapPermanent(courser)
        withClue("baseline: suspected, with suspect's menace and can't-block, and tapped") {
            val before = projector.project(driver.state)
            before.isSuspected(courser) shouldBe true
            before.hasKeyword(courser, Keyword.MENACE) shouldBe true
            before.cantBlock(courser) shouldBe true
            driver.isTapped(courser) shouldBe true
        }

        enchant(driver, player, courser, "Airtight Alibi")

        val projected = projector.project(driver.state)
        withClue("no longer suspected — and the menace and can't-block go with it (CR 701.60c)") {
            projected.isSuspected(courser) shouldBe false
            projected.hasKeyword(courser, Keyword.MENACE) shouldBe false
            projected.cantBlock(courser) shouldBe false
        }
        withClue("untapped and hexproof until end of turn") {
            driver.isTapped(courser) shouldBe false
            projected.hasKeyword(courser, Keyword.HEXPROOF) shouldBe true
        }
        withClue("3/3 base, +1/+1 from Convenient Target, +2/+2 from Airtight Alibi") {
            projected.getPower(courser) shouldBe 6
            projected.getToughness(courser) shouldBe 6
        }
    }

    test("the enchanted creature can't become suspected — designation, menace and can't-block all blocked") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val courser = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        enchant(driver, player, courser, "Airtight Alibi")
        withClue("baseline: not suspected, and the prohibition is live") {
            val before = projector.project(driver.state)
            before.isSuspected(courser) shouldBe false
            before.canBecomeSuspected(courser) shouldBe false
        }

        // Convenient Target's enters trigger tries to suspect the creature it enchants.
        enchant(driver, player, courser, "Convenient Target")

        val projected = projector.project(driver.state)
        withClue("the suspect was a whole no-op — not just the designation half") {
            projected.isSuspected(courser) shouldBe false
            projected.hasKeyword(courser, Keyword.MENACE) shouldBe false
            projected.cantBlock(courser) shouldBe false
        }
        withClue("the rest of Convenient Target still happened: its +1/+1 is unaffected") {
            projected.getPower(courser) shouldBe 6
            projected.getToughness(courser) shouldBe 6
        }
    }

    test("the prohibition dies with the Aura, so the creature can be suspected again") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val courser = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        val alibi = enchant(driver, player, courser, "Airtight Alibi")
        driver.moveToGraveyard(alibi)
        withClue("with the Aura gone the +2/+2 and the prohibition both lift") {
            val after = projector.project(driver.state)
            after.getPower(courser) shouldBe 3
            after.canBecomeSuspected(courser) shouldBe true
        }

        enchant(driver, player, courser, "Convenient Target")

        val projected = projector.project(driver.state)
        withClue("suspect lands normally once nothing is prohibiting it") {
            projected.isSuspected(courser) shouldBe true
            projected.hasKeyword(courser, Keyword.MENACE) shouldBe true
            projected.cantBlock(courser) shouldBe true
        }
    }

    test("an already-suspected creature stays suspected when the Aura arrives after being un-suspected") {
        // CR 701.60d and the prohibition are different questions: the enters trigger clears the
        // designation once, and the static then keeps it clear. Re-suspecting after the Aura is
        // already attached must fail — otherwise the card only works on the turn it lands.
        val driver = createDriver()
        val player = driver.activePlayer!!
        val courser = driver.putCreatureOnBattlefield(player, "Centaur Courser")

        enchant(driver, player, courser, "Convenient Target")
        enchant(driver, player, courser, "Airtight Alibi")
        projector.project(driver.state).isSuspected(courser) shouldBe false

        // A second suspecting Aura, after the alibi is already in place.
        enchant(driver, player, courser, "Convenient Target")

        val projected = projector.project(driver.state)
        withClue("still cleared — the static outlives the one-shot that cleared it") {
            projected.isSuspected(courser) shouldBe false
            projected.hasKeyword(courser, Keyword.MENACE) shouldBe false
            projected.cantBlock(courser) shouldBe false
        }
    }
})
