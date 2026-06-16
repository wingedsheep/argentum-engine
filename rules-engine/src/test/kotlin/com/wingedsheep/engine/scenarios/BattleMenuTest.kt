package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.BattleMenu
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Battle Menu — {1}{W} Instant, choose one —
 *  • Attack — Create a 2/2 white Knight creature token.
 *  • Ability — Target creature gets +0/+4 until end of turn.
 *  • Magic — Destroy target creature with power 4 or greater.
 *  • Item — You gain 4 life.
 */
class BattleMenuTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(BattleMenu)
        return driver
    }

    fun GameTestDriver.castBattleMenuPickMode(me: com.wingedsheep.sdk.model.EntityId, modeKeyword: String): ChooseOptionDecision {
        val spell = putCardInHand(me, "Battle Menu")
        giveColorlessMana(me, 1)
        giveMana(me, Color.WHITE, 1)
        submit(CastSpell(playerId = me, cardId = spell))
        val mode = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        val idx = mode.options.indexOfFirst { it.contains(modeKeyword) }
        submitDecision(me, OptionChosenResponse(mode.id, idx))
        return mode
    }

    test("Attack mode — creates a 2/2 white Knight token") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val creaturesBefore = driver.getCreatures(me).size
        driver.castBattleMenuPickMode(me, "Attack")
        driver.bothPass()

        val newCreatures = driver.getCreatures(me).filter { driver.getCardName(it)?.contains("Knight") == true }
        newCreatures.size shouldBe 1
        val token = newCreatures.first()
        projector.getProjectedPower(driver.state, token) shouldBe 2
        projector.getProjectedToughness(driver.state, token) shouldBe 2
        driver.getCreatures(me).size shouldBe (creaturesBefore + 1)
    }

    test("Item mode — gain 4 life") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val lifeBefore = driver.getLifeTotal(me)
        driver.castBattleMenuPickMode(me, "Item")
        driver.bothPass()
        driver.getLifeTotal(me) shouldBe (lifeBefore + 4)
    }

    test("Ability mode — target creature gets +0/+4 until end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser") // 3/3
        projector.getProjectedToughness(driver.state, courser) shouldBe 3

        driver.castBattleMenuPickMode(me, "Ability")
        val targetDecision = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        targetDecision // sanity
        driver.submitTargetSelection(me, listOf(courser))
        driver.bothPass()

        projector.getProjectedPower(driver.state, courser) shouldBe 3
        projector.getProjectedToughness(driver.state, courser) shouldBe 7
    }

    test("Magic mode — destroys a target creature with power 4 or greater") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Hill Giant is a 3/3... need power 4+. Use Ironroot Treefolk (0/7)? No. Use a 4-power creature.
        val bigGuy = driver.putCreatureOnBattlefield(opp, "Craw Wurm") // 6/4
        driver.getCardName(bigGuy) // ensure resolvable

        driver.castBattleMenuPickMode(me, "Magic")
        val targetDecision = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        targetDecision
        driver.submitTargetSelection(me, listOf(bigGuy))
        driver.bothPass()

        driver.findPermanent(opp, "Craw Wurm") shouldBe null
        driver.getGraveyard(opp).contains(bigGuy) shouldBe true
    }
})
