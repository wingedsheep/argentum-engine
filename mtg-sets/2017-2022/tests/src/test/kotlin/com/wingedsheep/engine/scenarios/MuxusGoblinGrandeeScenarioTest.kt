package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.jmp.cards.MuxusGoblinGrandee
import com.wingedsheep.mtg.sets.definitions.p02.cards.GoblinPiker
import com.wingedsheep.mtg.sets.definitions.por.cards.RagingGoblin
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.engine.state.ZoneKey
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/** Scenario tests for Muxus, Goblin Grandee. */
class MuxusGoblinGrandeeScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(MuxusGoblinGrandee, GoblinPiker, RagingGoblin))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20, skipMulligans = true)
    }

    test("ETB puts every Goblin creature card with mana value 5 or less onto the battlefield, rest to the bottom") {
        val d = setup()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Top six: two cheap Goblins, a mana value 6 Goblin, a non-Goblin creature, two lands.
        val forest = d.putCardOnTopOfLibrary(you, "Forest")
        val bears = d.putCardOnTopOfLibrary(you, "Grizzly Bears")
        val bigGoblin = d.putCardOnTopOfLibrary(you, "Muxus, Goblin Grandee")
        val piker = d.putCardOnTopOfLibrary(you, "Goblin Piker")
        val forest2 = d.putCardOnTopOfLibrary(you, "Forest")
        val raging = d.putCardOnTopOfLibrary(you, "Raging Goblin")

        d.giveMana(you, Color.RED, 6)
        val muxus = d.putCardInHand(you, "Muxus, Goblin Grandee")
        d.castSpell(you, muxus).error shouldBe null
        d.bothPass() // resolve the creature spell
        d.pendingDecision shouldBe null
        d.bothPass() // resolve the ETB trigger
        while (d.pendingDecision == null && d.stackSize > 0) d.bothPass()

        d.getPermanents(you) shouldContainAll listOf(muxus, piker, raging)
        val library = d.state.getZone(ZoneKey(you, Zone.LIBRARY))
        val bottomFour = library.takeLast(4)
        bottomFour.toSet() shouldBe setOf(forest, bears, bigGoblin, forest2)
    }

    test("attack trigger gives +1/+1 for each other Goblin you control") {
        val d = setup()
        val you = d.activePlayer!!
        val opp = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val muxus = d.putCreatureOnBattlefield(you, "Muxus, Goblin Grandee")
        d.putCreatureOnBattlefield(you, "Goblin Piker")
        d.putCreatureOnBattlefield(you, "Raging Goblin")
        d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.removeSummoningSickness(muxus)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(you, listOf(muxus), opp).error shouldBe null
        while (d.stackSize > 0) d.bothPass()

        d.state.projectedState.getPower(muxus) shouldBe 6
        d.state.projectedState.getToughness(muxus) shouldBe 6
    }
})
