package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.VexingBauble
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Vexing Bauble (MH3 #212) — {1} Artifact.
 *
 * "Whenever a player casts a spell, if no mana was spent to cast it, counter that spell.
 *  {1}, {T}, Sacrifice this artifact: Draw a card."
 */
class VexingBaubleScenarioTest : FunSpec({

    val sacDrawAbilityId = VexingBauble.activatedAbilities.first().id

    fun setup(): Pair<GameTestDriver, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to p1
    }

    test("counters its controller's own spell when no mana was spent (a player, not just opponents)") {
        val (driver, p1) = setup()
        driver.putPermanentOnBattlefield(p1, "Vexing Bauble")

        val mox = driver.putCardInHand(p1, "Mox Ruby") // {0}: nothing paid
        driver.castSpell(p1, mox)
        driver.bothPass() // trigger resolves, counters the Mox

        driver.state.getGraveyard(p1).contains(mox) shouldBe true
        driver.findPermanent(p1, "Mox Ruby") shouldBe null
    }

    test("does not counter a spell that mana was spent on") {
        val (driver, p1) = setup()
        val p2 = driver.getOpponent(p1)
        driver.putPermanentOnBattlefield(p2, "Vexing Bauble")

        val bears = driver.putCardInHand(p1, "Grizzly Bears")
        driver.giveMana(p1, Color.GREEN, 2)
        driver.castSpell(p1, bears)
        driver.bothPass()

        driver.findPermanent(p1, "Grizzly Bears").shouldNotBeNull()
    }

    test("{1}, {T}, Sacrifice: draw a card") {
        val (driver, p1) = setup()
        val bauble = driver.putPermanentOnBattlefield(p1, "Vexing Bauble")
        driver.giveMana(p1, Color.GREEN, 1)
        val handBefore = driver.getHand(p1).size

        driver.submit(
            ActivateAbility(playerId = p1, sourceId = bauble, abilityId = sacDrawAbilityId)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getHand(p1).size shouldBe handBefore + 1
        driver.state.getBattlefield().contains(bauble) shouldBe false
        driver.state.getGraveyard(p1).contains(bauble) shouldBe true
    }
})
