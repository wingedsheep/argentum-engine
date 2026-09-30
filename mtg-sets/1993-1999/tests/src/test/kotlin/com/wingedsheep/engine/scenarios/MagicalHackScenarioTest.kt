package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseReplacementDecision
import com.wingedsheep.engine.core.ReplacementChosenResponse
import com.wingedsheep.engine.core.TextChangedEvent
import com.wingedsheep.engine.mechanics.mana.IntrinsicManaAbilities
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.BogWraith
import com.wingedsheep.mtg.sets.definitions.lea.cards.MagicalHack
import com.wingedsheep.mtg.sets.definitions.lea.cards.Unsummon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class MagicalHackScenarioTest : FunSpec({
    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MagicalHack, BogWraith, Unsummon))
        driver.initMirrorMatch(Deck.of("Island" to 30, "Forest" to 30))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }
    fun GameTestDriver.castHack(player: EntityId, target: EntityId, onStack: Boolean = false) {
        val hack = putCardInHand(player, "Magical Hack")
        giveMana(player, Color.BLUE, 1)
        val result = if (onStack) castSpellWithTargets(player, hack, listOf(ChosenTarget.Spell(target)))
            else castSpell(player, hack, listOf(target))
        result.error shouldBe null
        bothPass().error shouldBe null
        (pendingDecision as ChooseReplacementDecision).fromOptions.shouldContainExactlyInAnyOrder("Plains", "Island", "Swamp", "Mountain", "Forest")
        (pendingDecision as ChooseReplacementDecision).toOptions.shouldContainExactlyInAnyOrder("Plains", "Island", "Swamp", "Mountain", "Forest")
    }
    fun GameTestDriver.replace(player: EntityId, from: String, to: String) {
        val decision = pendingDecision as ChooseReplacementDecision
        val result = submitDecision(player, ReplacementChosenResponse(decision.id, decision.fromOptions.indexOf(from), decision.toOptions.indexOf(to)))
        result.error shouldBe null
        val event = result.events.filterIsInstance<TextChangedEvent>().single()
        event.fromWord shouldBe from
        event.toWord shouldBe to
    }
    test("land type and intrinsic mana change indefinitely without renaming the land") {
        val (driver, player) = newGame()
        val forest = driver.putLandOnBattlefield(player, "Forest")
        driver.castHack(player, forest)
        driver.replace(player, "Forest", "Island")
        driver.state.projectedState.getSubtypes(forest) shouldBe setOf("Island")
        driver.getCardName(forest) shouldBe "Forest"
        IntrinsicManaAbilities.forEntity(driver.state, driver.state.projectedState, forest).map { (it.effect as AddManaEffect).color } shouldBe listOf(Color.BLUE)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.projectedState.getSubtypes(forest) shouldBe setOf("Island")
    }
    test("printed swampwalk changes to plainswalk and the picker highlights Swamp") {
        val (driver, player) = newGame()
        val wraith = driver.putCreatureOnBattlefield(player, "Bog Wraith")
        driver.castHack(player, wraith)
        val decision = driver.pendingDecision as ChooseReplacementDecision
        decision.fromOptions.first() shouldBe "Swamp"
        driver.replace(player, "Swamp", "Plains")
        driver.state.projectedState.getKeywords(wraith) shouldContain "PLAINSWALK"
        driver.state.projectedState.getKeywords(wraith) shouldNotContain "SWAMPWALK"
    }
    test("a target without land words is legal and the same word is rejected") {
        val (driver, player) = newGame()
        val target = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.castHack(player, target)
        val decision = driver.pendingDecision as ChooseReplacementDecision
        val swamp = decision.fromOptions.indexOf("Swamp")
        driver.submitDecision(player, ReplacementChosenResponse(decision.id, swamp, decision.toOptions.indexOf("Swamp"))).error shouldBe "Replacement Swamp not allowed for Swamp"
        driver.pendingDecision shouldBe decision
        driver.replace(player, "Swamp", "Plains")
        driver.state.projectedState.getSubtypes(target) shouldBe setOf("Bear")
    }
    test("a permanent spell retains its changed landwalk after resolving") {
        val (driver, player) = newGame()
        val wraith = driver.putCardInHand(player, "Bog Wraith")
        driver.giveMana(player, Color.BLACK, 4)
        driver.castSpell(player, wraith).error shouldBe null
        driver.castHack(player, wraith, onStack = true)
        driver.replace(player, "Swamp", "Plains")
        driver.bothPass().error shouldBe null
        driver.state.getBattlefield() shouldContain wraith
        driver.state.projectedState.getKeywords(wraith) shouldContain "PLAINSWALK"
    }
    test("leaving and recasting ends the old object's indefinite change") {
        val (driver, player) = newGame()
        val wraith = driver.putCreatureOnBattlefield(player, "Bog Wraith")
        driver.castHack(player, wraith)
        driver.replace(player, "Swamp", "Plains")
        val bounce = driver.putCardInHand(player, "Unsummon")
        driver.giveMana(player, Color.BLUE, 1)
        driver.castSpell(player, bounce, listOf(wraith)).error shouldBe null
        driver.bothPass().error shouldBe null
        driver.state.getEntity(wraith)!!.get<TextReplacementComponent>() shouldBe null
        driver.giveMana(player, Color.BLACK, 4)
        driver.castSpell(player, wraith).error shouldBe null
        driver.bothPass().error shouldBe null
        driver.state.projectedState.getKeywords(wraith) shouldContain "SWAMPWALK"
        driver.state.projectedState.getKeywords(wraith) shouldNotContain "PLAINSWALK"
    }

    test("target leaving before resolution fizzles without presenting a word choice") {
        val (driver, player) = newGame()
        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val change = driver.putCardInHand(player, "Magical Hack")
        driver.giveMana(player, Color.BLUE, 2)
        driver.castSpell(player, change, listOf(bear)).error shouldBe null
        val bounce = driver.putCardInHand(player, "Unsummon")
        driver.castSpell(player, bounce, listOf(bear)).error shouldBe null
        driver.bothPass().error shouldBe null
        driver.bothPass().error shouldBe null
        driver.pendingDecision shouldBe null
        driver.state.getGraveyard(player) shouldContain change
        driver.state.getEntity(bear)!!.get<TextReplacementComponent>() shouldBe null
    }
})
