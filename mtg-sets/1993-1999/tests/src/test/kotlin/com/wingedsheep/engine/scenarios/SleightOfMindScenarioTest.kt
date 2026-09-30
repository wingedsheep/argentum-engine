package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.ChooseReplacementDecision
import com.wingedsheep.engine.core.ReplacementChosenResponse
import com.wingedsheep.engine.core.TextChangedEvent
import com.wingedsheep.engine.state.components.identity.ProtectionComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.SleightOfMind
import com.wingedsheep.mtg.sets.definitions.lea.cards.WhiteKnight
import com.wingedsheep.mtg.sets.definitions.lea.cards.Unsummon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class SleightOfMindScenarioTest : FunSpec({
    val removeRed = card("Remove Red Witness") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Destroy target red creature."
        spell {
            val victim = target(TargetFilter.Creature.withColor(Color.RED))
            effect = Effects.Destroy(victim)
        }
    }
    val redWitness = card("Red Witness") {
        manaCost = "{R}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val blueWitness = card("Blue Witness") {
        manaCost = "{U}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val grantProtection = card("Grant Protection Witness") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val subject = target(TargetFilter.Creature)
            effect = Effects.GrantProtectionFromColor(Color.BLACK, subject)
        }
    }
    val tuckSpell = card("Tuck Spell Witness") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val subject = target(com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent())
            effect = Effects.PutOnTopOrBottomOfLibrary(subject)
        }
    }
    val dyingWitness = card("Dying Word Witness") {
        manaCost = "{0}"
        typeLine = "Creature — Bear"
        oracleText = "When this creature dies, destroy target red creature."
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.self.dies()
            val victim = target(TargetFilter.Creature.withColor(Color.RED))
            effect = Effects.Destroy(victim)
        }
    }
    val killWitness = card("Kill Witness") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val victim = target(TargetFilter.Creature)
            effect = Effects.Destroy(victim)
        }
    }
    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SleightOfMind, WhiteKnight, Unsummon, removeRed, redWitness, blueWitness, grantProtection, dyingWitness, killWitness, tuckSpell))
        driver.initMirrorMatch(Deck.of("Island" to 30, "Forest" to 30))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }
    fun GameTestDriver.castSleight(player: EntityId, target: EntityId, onStack: Boolean = false) {
        val sleight = putCardInHand(player, "Sleight of Mind")
        giveMana(player, Color.BLUE, 1)
        val result = if (onStack) castSpellWithTargets(player, sleight, listOf(ChosenTarget.Spell(target)))
            else castSpell(player, sleight, listOf(target))
        result.error shouldBe null
        bothPass().error shouldBe null
        (pendingDecision as ChooseReplacementDecision).fromOptions.shouldContainExactlyInAnyOrder("White", "Blue", "Black", "Red", "Green")
        (pendingDecision as ChooseReplacementDecision).toOptions.shouldContainExactlyInAnyOrder("White", "Blue", "Black", "Red", "Green")
    }
    fun GameTestDriver.replace(player: EntityId, from: String, to: String) {
        val decision = pendingDecision as ChooseReplacementDecision
        val result = submitDecision(player, ReplacementChosenResponse(decision.id, decision.fromOptions.indexOf(from), decision.toOptions.indexOf(to)))
        result.error shouldBe null
        val event = result.events.filterIsInstance<TextChangedEvent>().single()
        event.fromWord shouldBe from
        event.toWord shouldBe to
    }
    test("protection changes indefinitely while name color and mana cost stay unchanged") {
        val (driver, player) = newGame()
        val knight = driver.putCreatureOnBattlefield(player, "White Knight")
        driver.addComponent(knight, ProtectionComponent(colors = setOf(Color.BLACK)))
        driver.castSleight(player, knight)
        driver.replace(player, "Black", "Red")
        driver.state.projectedState.getKeywords(knight) shouldContain "PROTECTION_FROM_RED"
        driver.state.projectedState.getKeywords(knight) shouldNotContain "PROTECTION_FROM_BLACK"
        driver.state.projectedState.getColors(knight) shouldBe setOf("WHITE")
        driver.getCardName(knight) shouldBe "White Knight"
        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.projectedState.getKeywords(knight) shouldContain "PROTECTION_FROM_RED"
    }
    test("a word absent from the target is legal and identical words are rejected") {
        val (driver, player) = newGame()
        val forest = driver.putLandOnBattlefield(player, "Forest")
        driver.castSleight(player, forest)
        val decision = driver.pendingDecision as ChooseReplacementDecision
        driver.submitDecision(player, ReplacementChosenResponse(decision.id, decision.fromOptions.indexOf("Red"), decision.toOptions.indexOf("Red"))).error shouldBe "Replacement Red not allowed for Red"
        driver.pendingDecision shouldBe decision
        driver.replace(player, "Red", "Blue")
        driver.state.projectedState.getSubtypes(forest) shouldBe setOf("Forest")
        driver.getCardName(forest) shouldBe "Forest"
    }
    test("a color change on a spell revalidates its original target without retargeting") {
        val (driver, player) = newGame()
        val red = driver.putCreatureOnBattlefield(player, "Red Witness")
        val blue = driver.putCreatureOnBattlefield(player, "Blue Witness")
        val removal = driver.putCardInHand(player, "Remove Red Witness")
        driver.giveMana(player, Color.BLUE, 1)
        driver.castSpell(player, removal, listOf(red)).error shouldBe null
        driver.castSleight(player, removal, onStack = true)
        driver.replace(player, "Red", "Blue")
        driver.bothPass().error shouldBe null
        driver.state.getBattlefield() shouldContain red
        driver.state.getBattlefield() shouldContain blue
        driver.state.getGraveyard(player) shouldContain removal
        driver.state.getEntity(removal)!!.get<TextReplacementComponent>() shouldBe null
    }
    test("text replacement leaves subsequently granted protection unchanged") {
        val (driver, player) = newGame()
        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.castSleight(player, bear)
        driver.replace(player, "Black", "Red")
        val grant = driver.putCardInHand(player, "Grant Protection Witness")
        driver.castSpell(player, grant, listOf(bear)).error shouldBe null
        driver.bothPass().error shouldBe null
        driver.state.projectedState.getKeywords(bear) shouldContain "PROTECTION_FROM_BLACK"
        driver.state.projectedState.getKeywords(bear) shouldNotContain "PROTECTION_FROM_RED"
    }
    test("successive text changes read the already changed words") {
        val (driver, player) = newGame()
        val knight = driver.putCreatureOnBattlefield(player, "White Knight")
        driver.addComponent(knight, ProtectionComponent(colors = setOf(Color.BLACK)))
        driver.castSleight(player, knight)
        driver.replace(player, "Black", "Red")
        driver.castSleight(player, knight)
        (driver.pendingDecision as ChooseReplacementDecision).fromOptions.first() shouldBe "Red"
        driver.replace(player, "Red", "Green")
        driver.state.projectedState.getKeywords(knight) shouldContain "PROTECTION_FROM_GREEN"
        driver.state.projectedState.getKeywords(knight) shouldNotContain "PROTECTION_FROM_RED"
    }

    test("successive changes preserve separate original protection abilities") {
        val (driver, player) = newGame()
        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.addComponent(bear, ProtectionComponent(colors = linkedSetOf(Color.RED, Color.WHITE)))
        driver.castSleight(player, bear)
        driver.replace(player, "White", "Green")
        driver.castSleight(player, bear)
        driver.replace(player, "Red", "White")
        driver.state.projectedState.getKeywords(bear).shouldContainExactlyInAnyOrder("PROTECTION_FROM_WHITE", "PROTECTION_FROM_GREEN")
    }

    test("putting a changed spell into its library ends the text change before recasting") {
        val (driver, player) = newGame()
        val red = driver.putCreatureOnBattlefield(player, "Red Witness")
        val removal = driver.putCardInHand(player, "Remove Red Witness")
        driver.giveMana(player, Color.BLUE, 1)
        driver.castSpell(player, removal, listOf(red)).error shouldBe null
        driver.castSleight(player, removal, onStack = true)
        driver.replace(player, "Red", "Blue")
        val tuck = driver.putCardInHand(player, "Tuck Spell Witness")
        driver.castSpellWithTargets(player, tuck, listOf(ChosenTarget.Spell(removal))).error shouldBe null
        driver.bothPass().error shouldBe null
        val decision = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(player, OptionChosenResponse(decision.id, 0)).error shouldBe null
        driver.state.getLibrary(player).first() shouldBe removal
        driver.state.getEntity(removal)!!.get<TextReplacementComponent>() shouldBe null
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.state.getHand(player) shouldContain removal
        driver.giveMana(player, Color.BLUE, 1)
        driver.castSpell(player, removal, listOf(red)).error shouldBe null
        driver.bothPass().error shouldBe null
        driver.state.getGraveyard(player) shouldContain red
    }

    test("target leaving before resolution fizzles without presenting a word choice") {
        val (driver, player) = newGame()
        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val change = driver.putCardInHand(player, "Sleight of Mind")
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

    test("a dies trigger keeps its changed target words after the source loses the text component") {
        val (driver, player) = newGame()
        val dying = driver.putCreatureOnBattlefield(player, "Dying Word Witness")
        val red = driver.putCreatureOnBattlefield(player, "Red Witness")
        val blue = driver.putCreatureOnBattlefield(player, "Blue Witness")
        driver.castSleight(player, dying)
        driver.replace(player, "Red", "Blue")
        val kill = driver.putCardInHand(player, "Kill Witness")
        driver.castSpell(player, kill, listOf(dying)).error shouldBe null
        driver.bothPass().error shouldBe null
        driver.state.getEntity(dying)!!.get<TextReplacementComponent>() shouldBe null
        driver.submitTargetSelection(player, listOf(blue)).error shouldBe null
        driver.bothPass().error shouldBe null
        driver.state.getGraveyard(player) shouldContain blue
        driver.state.getBattlefield() shouldContain red
    }
})
