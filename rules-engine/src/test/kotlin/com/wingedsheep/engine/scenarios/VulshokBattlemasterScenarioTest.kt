package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.VulshokBattlemaster
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Vulshok Battlemaster (MRD #110) — {4}{R} Creature — Human Warrior, 2/2.
 *
 * "Haste
 *  When this creature enters, attach all Equipment on the battlefield to it."
 *
 * The printed rulings are the test plan: unattached Equipment comes over, Equipment already
 * strapped to another creature is *pulled off* and re-attached, and **opponents'** Equipment moves
 * too without changing controller. That last one is the easiest to get wrong — a stray
 * `youControl()` on the group filter would silently pass every other test here.
 *
 * The Battlemaster is *cast* rather than placed, because the driver's direct-placement helpers
 * don't emit the `ZoneChangeEvent` an enters-the-battlefield trigger reads.
 */
class VulshokBattlemasterScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(VulshokBattlemaster))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.attachedTo(equipment: EntityId): EntityId? =
        state.getEntity(equipment)?.get<AttachedToComponent>()?.targetId

    /** Cast the Battlemaster and let the enters trigger fully resolve; returns its entity id. */
    fun GameTestDriver.castBattlemaster(player: EntityId): EntityId {
        val card = putCardInHand(player, "Vulshok Battlemaster")
        giveMana(player, Color.RED, 5)
        castSpell(player, card).isSuccess shouldBe true
        var guard = 0
        while ((stackSize > 0 || isPaused) && guard++ < 30) {
            if (isPaused) autoResolveDecision() else bothPass()
        }
        return findPermanent(player, "Vulshok Battlemaster")!!
    }

    test("has haste") {
        VulshokBattlemaster.keywords.contains(Keyword.HASTE) shouldBe true
    }

    test("unattached Equipment is attached on entry") {
        val driver = newDriver()
        val me = driver.activePlayer!!

        val bonesplitter = driver.putPermanentOnBattlefield(me, "Bonesplitter")
        driver.attachedTo(bonesplitter) shouldBe null

        val battlemaster = driver.castBattlemaster(me)

        driver.attachedTo(bonesplitter) shouldBe battlemaster
    }

    test("Equipment already attached to another creature is pulled off and re-attached") {
        val driver = newDriver()
        val me = driver.activePlayer!!

        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val bonesplitter = driver.putPermanentOnBattlefield(me, "Bonesplitter")
        driver.addComponent(bonesplitter, AttachedToComponent(lions))
        driver.attachedTo(bonesplitter) shouldBe lions

        val battlemaster = driver.castBattlemaster(me)

        driver.attachedTo(bonesplitter) shouldBe battlemaster
    }

    test("an opponent's Equipment moves over too, and they keep control of it") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val theirBonesplitter = driver.putPermanentOnBattlefield(opponent, "Bonesplitter")
        driver.getController(theirBonesplitter) shouldBe opponent

        val battlemaster = driver.castBattlemaster(me)

        driver.attachedTo(theirBonesplitter) shouldBe battlemaster
        // "Control of the Equipment doesn't change."
        driver.getController(theirBonesplitter) shouldBe opponent
    }

    test("every Equipment on the battlefield comes over, not just one") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val mine = driver.putPermanentOnBattlefield(me, "Bonesplitter")
        val theirs = driver.putPermanentOnBattlefield(opponent, "Leonin Scimitar")

        val battlemaster = driver.castBattlemaster(me)

        driver.attachedTo(mine) shouldBe battlemaster
        driver.attachedTo(theirs) shouldBe battlemaster
    }
})
