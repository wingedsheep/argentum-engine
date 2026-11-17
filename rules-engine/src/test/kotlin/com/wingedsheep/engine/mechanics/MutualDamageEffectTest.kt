package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.CreatureFoughtEvent
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreatureStat
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `MutualDamageEffect` — "each of those creatures deals damage equal to its toughness to the other"
 * (Grim Contest). Card-level coverage: `GrimContestScenarioTest`.
 *
 *  - Each creature deals damage equal to the named stat (toughness, or power), not the other one.
 *  - Both amounts are read before either is dealt: one batch, so a wither source's -1/-1 counters
 *    can't shrink the other creature's toughness before it deals its damage.
 *  - The creatures are the damage sources, so their lifelink applies.
 *  - Not a fight: no `CreatureFoughtEvent`, no "whenever a creature fights" trigger.
 *  - If either creature is gone as it resolves, neither deals or is dealt damage (Grim Contest's ruling).
 */
class MutualDamageEffectTest : FunSpec({

    fun creature(name: String, power: Int, toughness: Int, vararg kws: Keyword) = card(name) {
        manaCost = "{1}"
        typeLine = "Creature — Beast"
        this.power = power
        this.toughness = toughness
        if (kws.isNotEmpty()) keywords(*kws)
    }

    val Turtle = creature("Mutual Test Turtle", 1, 4)
    val Giant = creature("Mutual Test Giant", 3, 3)
    val Withering = creature("Mutual Test Withering", 1, 3, Keyword.WITHER)
    val Bear = creature("Mutual Test Bear", 2, 2)
    val Healer = creature("Mutual Test Healer", 0, 5, Keyword.LIFELINK)

    /** "Whenever a creature you control fights, you gain 1 life." */
    val Watcher = card("Mutual Test Watcher") {
        manaCost = "{G}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl()).fights()
            effect = Effects.GainLife(1)
        }
    }

    fun contest(name: String, stat: CreatureStat) = card(name) {
        manaCost = "{G}"
        typeLine = "Instant"
        spell {
            val mine = target(TargetFilter.CreatureYouControl)
            val theirs = target(TargetFilter(GameObjectFilter.Creature.opponentControls()))
            effect = Effects.MutualDamage(mine, theirs, stat)
        }
    }

    /** "Target creature gains hexproof until end of turn." */
    val Veil = card("Mutual Test Veil") {
        manaCost = "{G}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.GrantKeyword(Keyword.HEXPROOF, creature)
        }
    }

    val ToughnessContest = contest("Mutual Test Toughness Contest", CreatureStat.TOUGHNESS)
    val PowerContest = contest("Mutual Test Power Contest", CreatureStat.POWER)

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(Turtle, Giant, Withering, Bear, Healer, Watcher, Veil, ToughnessContest, PowerContest)
        )
        driver.initMirrorMatch(deck = Deck.of("Forest" to 60), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun drain(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && (driver.pendingDecision != null || driver.state.stack.isNotEmpty())) {
            if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
        }
    }

    fun cast(
        driver: GameTestDriver, player: EntityId, spellName: String, mine: EntityId, theirs: EntityId,
        beforeResolve: () -> Unit = {}
    ) {
        val spell = driver.putCardInHand(player, spellName)
        driver.giveMana(player, Color.GREEN, 1)
        driver.castSpell(player, spell, listOf(mine, theirs)).outcome shouldBe Outcome.Done
        beforeResolve()
        drain(driver)
    }

    fun damageOn(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    test("each creature deals damage equal to its toughness, not its power") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val turtle = driver.putCreatureOnBattlefield(me, "Mutual Test Turtle")
        val giant = driver.putCreatureOnBattlefield(opp, "Mutual Test Giant")

        cast(driver, me, "Mutual Test Toughness Contest", turtle, giant)

        withClue("the 1/4 dealt 4 to the 3/3") { (giant in driver.getGraveyard(opp)) shouldBe true }
        withClue("the 3/3 dealt 3 back; the 1/4 survives") { damageOn(driver, turtle) shouldBe 3 }
    }

    test("the power axis deals each creature's power") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val turtle = driver.putCreatureOnBattlefield(me, "Mutual Test Turtle")
        val giant = driver.putCreatureOnBattlefield(opp, "Mutual Test Giant")

        cast(driver, me, "Mutual Test Power Contest", turtle, giant)

        damageOn(driver, giant) shouldBe 1
        damageOn(driver, turtle) shouldBe 3
    }

    test("both amounts are read before either is dealt: wither can't shrink the other's toughness first") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val withering = driver.putCreatureOnBattlefield(me, "Mutual Test Withering")
        val bear = driver.putCreatureOnBattlefield(opp, "Mutual Test Bear")

        cast(driver, me, "Mutual Test Toughness Contest", withering, bear)

        withClue("the 2/2 got three -1/-1 counters and died") { (bear in driver.getGraveyard(opp)) shouldBe true }
        withClue("it still dealt its toughness, 2, to the wither creature") {
            damageOn(driver, withering) shouldBe 2
            (driver.state.getEntity(withering)?.get<CountersComponent>()?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0) shouldBe 0
        }
    }

    test("the creatures are the sources, so lifelink applies") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val healer = driver.putCreatureOnBattlefield(me, "Mutual Test Healer")
        val giant = driver.putCreatureOnBattlefield(opp, "Mutual Test Giant")
        val eventsBefore = driver.events.size

        cast(driver, me, "Mutual Test Toughness Contest", healer, giant)

        driver.events.drop(eventsBefore).filterIsInstance<DamageDealtEvent>()
            .map { it.sourceId to it.targetId }.toSet() shouldBe setOf(healer to giant, giant to healer)
        driver.getLifeTotal(me) shouldBe 25
    }

    test("it is not a fight: no fought event, no fight trigger") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "Mutual Test Watcher")
        driver.putPermanentOnBattlefield(opp, "Mutual Test Watcher")
        val turtle = driver.putCreatureOnBattlefield(me, "Mutual Test Turtle")
        val giant = driver.putCreatureOnBattlefield(opp, "Mutual Test Giant")
        val eventsBefore = driver.events.size

        cast(driver, me, "Mutual Test Toughness Contest", turtle, giant)

        driver.events.drop(eventsBefore).filterIsInstance<CreatureFoughtEvent>() shouldBe emptyList()
        driver.getLifeTotal(me) shouldBe 20
        driver.getLifeTotal(opp) shouldBe 20
    }

    test("if the opponent's creature is gone, yours neither deals nor is dealt damage") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val turtle = driver.putCreatureOnBattlefield(me, "Mutual Test Turtle")
        val giant = driver.putCreatureOnBattlefield(opp, "Mutual Test Giant")

        cast(driver, me, "Mutual Test Toughness Contest", turtle, giant) { driver.moveToGraveyard(giant) }

        damageOn(driver, turtle) shouldBe 0
    }

    test("if your creature is gone, the opponent's neither deals nor is dealt damage") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val turtle = driver.putCreatureOnBattlefield(me, "Mutual Test Turtle")
        val giant = driver.putCreatureOnBattlefield(opp, "Mutual Test Giant")
        val eventsBefore = driver.events.size

        cast(driver, me, "Mutual Test Toughness Contest", turtle, giant) { driver.moveToGraveyard(turtle) }

        damageOn(driver, giant) shouldBe 0
        driver.events.drop(eventsBefore).filterIsInstance<DamageDealtEvent>() shouldBe emptyList()
    }

    test("an illegal target still on the battlefield: neither creature deals or is dealt damage") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val turtle = driver.putCreatureOnBattlefield(me, "Mutual Test Turtle")
        val giant = driver.putCreatureOnBattlefield(opp, "Mutual Test Giant")
        val eventsBefore = driver.events.size

        cast(driver, me, "Mutual Test Toughness Contest", turtle, giant) {
            // The opponent answers by giving its Giant hexproof: still a creature on the battlefield,
            // but no longer a legal target as the contest resolves.
            val veil = driver.putCardInHand(opp, "Mutual Test Veil")
            driver.giveMana(opp, Color.GREEN, 1)
            driver.passPriority(me)
            driver.castSpell(opp, veil, listOf(giant)).outcome shouldBe Outcome.Done
        }

        (giant in driver.state.getBattlefield()) shouldBe true
        damageOn(driver, giant) shouldBe 0
        damageOn(driver, turtle) shouldBe 0
        driver.events.drop(eventsBefore).filterIsInstance<DamageDealtEvent>() shouldBe emptyList()
    }
})
