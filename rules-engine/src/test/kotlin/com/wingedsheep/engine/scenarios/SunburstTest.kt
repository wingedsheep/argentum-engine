package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Sunburst (CR 702.44). "If this object is entering the battlefield from the stack as a creature,
 * it enters with a +1/+1 counter on it for each color of mana spent to cast it. Otherwise, it
 * enters with a charge counter on it for each color of mana spent to cast it."
 *
 * Pinned here: a noncreature gets charge counters and a creature gets +1/+1 counters (702.44a);
 * colors are counted, not mana, and colorless is not a color (CR 105.1); a sunburst permanent put
 * onto the battlefield other than from the stack gets no counters (702.44a, "from the stack").
 * The "as a creature" read off the object as it would exist on the battlefield (an artifact March
 * of the Machines animates) is pinned in EngineeredExplosivesScenarioTest, where March lives.
 */
class SunburstTest : FunSpec({

    val sunburstArtifact = card("Test Sunburst Relic") {
        manaCost = "{4}"
        typeLine = "Artifact"
        oracleText = "Sunburst"
        keywords(Keyword.SUNBURST)
    }

    val sunburstCreature = card("Test Sunburst Construct") {
        manaCost = "{4}"
        typeLine = "Artifact Creature — Construct"
        oracleText = "Sunburst"
        power = 1
        toughness = 1
        keywords(Keyword.SUNBURST)
    }

    val reanimateArtifact = card("Test Artifact Recall") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Return target artifact card from your graveyard to the battlefield."
        spell {
            val artifact = target(TargetFilter.ArtifactInYourGraveyard)
            effect = Effects.PutOntoBattlefieldFromGraveyard(artifact)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(sunburstArtifact, sunburstCreature, reanimateArtifact))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun counters(driver: GameTestDriver, id: EntityId, type: CounterType): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    test("a noncreature sunburst permanent enters with a charge counter per color spent") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        val relic = driver.putCardInHand(p, "Test Sunburst Relic")
        driver.giveMana(p, Color.WHITE, 2)
        driver.giveMana(p, Color.BLUE, 1)
        driver.giveMana(p, Color.RED, 1)

        driver.castSpell(p, relic).outcome shouldBe Outcome.Done
        driver.bothPass()

        // Four mana of three colors: three charge counters, never +1/+1 counters.
        counters(driver, relic, CounterType.CHARGE) shouldBe 3
        counters(driver, relic, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
    }

    test("a sunburst creature enters with a +1/+1 counter per color spent instead") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        val construct = driver.putCardInHand(p, "Test Sunburst Construct")
        driver.giveMana(p, Color.BLACK, 1)
        driver.giveMana(p, Color.GREEN, 1)
        driver.giveColorlessMana(p, 2)

        driver.castSpell(p, construct).outcome shouldBe Outcome.Done
        driver.bothPass()

        counters(driver, construct, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        counters(driver, construct, CounterType.CHARGE) shouldBe 0
    }

    test("colorless mana is not a color, so an all-colorless cast enters with no counters") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        val relic = driver.putCardInHand(p, "Test Sunburst Relic")
        driver.giveColorlessMana(p, 4)

        driver.castSpell(p, relic).outcome shouldBe Outcome.Done
        driver.bothPass()

        counters(driver, relic, CounterType.CHARGE) shouldBe 0
    }

    test("a sunburst permanent put onto the battlefield other than from the stack gets no counters") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        val relic = driver.putCardInGraveyard(p, "Test Sunburst Relic")
        val recall = driver.putCardInHand(p, "Test Artifact Recall")
        driver.giveMana(p, Color.RED, 1)

        driver.castSpellWithTargets(p, recall, listOf(ChosenTarget.Card(relic, p, Zone.GRAVEYARD))).error shouldBe null
        driver.bothPass()

        driver.state.getBattlefield().contains(relic) shouldBe true
        counters(driver, relic, CounterType.CHARGE) shouldBe 0
    }
})
