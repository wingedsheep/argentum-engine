package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.EmrakulTheWorldAnew
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class EmrakulTheWorldAnewScenarioTest : FunSpec({
    // A permanent that wasn't cast this turn, so its ability may target Emrakul.
    val bouncer = card("Test Bounce Relic") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Mana("{1}")
            val t = target(TargetFilter.Creature)
            effect = Effects.ReturnToHand(t)
        }
    }
    val emrakul = EmrakulTheWorldAnew.name

    fun GameTestDriver.controllerOf(id: EntityId) = state.projectedState.getController(id)
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(EmrakulTheWorldAnew, bouncer))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("the cast trigger takes all of the target player's creatures before Emrakul resolves, for good") {
        val d = driver()
        val bearA = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val bearB = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val mine = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, emrakul)
        d.giveColorlessMana(d.player1, 12)
        d.castSpell(d.player1, spell).error shouldBe null
        (d.state.pendingDecision as ChooseTargetsDecision).let {
            d.submitTargetSelection(d.player1, listOf(d.player2)).error shouldBe null
        }
        d.bothPass()
        d.getStackSpellNames() shouldBe listOf(emrakul)
        d.controllerOf(bearA) shouldBe d.player1
        d.controllerOf(bearB) shouldBe d.player1
        d.controllerOf(mine) shouldBe d.player1
        d.bothPass()
        d.findPermanent(d.player1, emrakul) shouldNotBe null
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe d.player2
        d.controllerOf(bearA) shouldBe d.player1
    }

    test("when Emrakul leaves the battlefield its controller sacrifices all of their creatures") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, emrakul)
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val relic = d.putPermanentOnBattlefield(d.player1, bouncer.name)
        d.giveColorlessMana(d.player1, 1)
        d.submit(
            ActivateAbility(d.player1, relic, bouncer.script.activatedAbilities.single().id, listOf(ChosenTarget.Permanent(source)))
        ).error shouldBe null
        d.bothPass()
        d.bothPass()
        d.findPermanent(d.player1, emrakul) shouldBe null
        d.findPermanent(d.player1, "Grizzly Bears") shouldBe null
        d.getGraveyardCardNames(d.player1).contains("Grizzly Bears") shouldBe true
        d.findPermanent(d.player2, "Grizzly Bears") shouldNotBe null
    }

    test("protection from spells: an opponent's Lightning Bolt can't target it") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player2, emrakul)
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, bolt, listOf(source)).error shouldNotBe null
    }
})
