package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CreatureFoughtEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Whenever a creature … fights" (CR 701.14) — `EventPattern.FoughtEvent` fed by the fight
 * executor's `CreatureFoughtEvent`. Card-level coverage: `FoeRazerRegentScenarioTest`.
 *
 *  - CR 701.14a — both creatures fight, so each fighter fires its own trigger; two of your
 *    creatures fighting each other trigger a "creature you control" watcher twice.
 *  - CR 701.14a — a fight that deals no damage (a 0-power fighter) is still a fight.
 *  - CR 701.14b — a fighter that left the battlefield before the fight: nobody fights, no trigger.
 *  - CR 701.14c — a creature that fights itself fights once, taking twice its power.
 *  - A fighter killed by the fight still fought (triggers are detected before state-based actions).
 *  - SELF binding fires only for the source's own fights.
 */
class FightTriggerTest : FunSpec({

    fun creature(name: String, power: Int, toughness: Int) = card(name) {
        manaCost = "{1}"
        typeLine = "Creature — Beast"
        this.power = power
        this.toughness = toughness
    }

    val Bear = creature("Fight Test Bear", 2, 2)
    val Giant = creature("Fight Test Giant", 5, 5)
    val ZeroWall = creature("Fight Test Zero Wall", 0, 4)

    /** "Whenever a creature you control fights, you gain 1 life." */
    val Watcher = card("Fight Test Watcher") {
        manaCost = "{G}"
        typeLine = "Enchantment"
        oracleText = "Whenever a creature you control fights, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl()).fights()
            effect = Effects.GainLife(1)
        }
    }

    /** "Whenever this creature fights, you gain 5 life." */
    val Duelist = card("Fight Test Duelist") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Human"
        power = 2
        toughness = 2
        oracleText = "Whenever this creature fights, you gain 5 life."
        triggeredAbility {
            trigger = Triggers.self.fights()
            effect = Effects.GainLife(5)
        }
    }

    /** "{T}: This creature fights itself." (CR 701.14c) */
    val SelfBrawler = card("Fight Test Self Brawler") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Beast"
        power = 3
        toughness = 7
        activatedAbility {
            cost = AbilityCost.Tap
            effect = Effects.Fight(EffectTarget.Self, EffectTarget.Self)
        }
    }

    /** "Target creature fights another target creature." */
    val Brawl = card("Fight Test Brawl") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "Target creature fights another target creature."
        spell {
            val a = target(TargetFilter.Creature)
            val b = target(TargetFilter.Creature)
            effect = Effects.Fight(a, b)
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Bear, Giant, ZeroWall, Watcher, Duelist, SelfBrawler, Brawl))
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

    fun brawl(driver: GameTestDriver, player: EntityId, a: EntityId, b: EntityId, beforeResolve: () -> Unit = {}) {
        val spell = driver.putCardInHand(player, "Fight Test Brawl")
        driver.giveMana(player, Color.GREEN, 1)
        driver.castSpell(player, spell, listOf(a, b)).outcome shouldBe Outcome.Done
        beforeResolve()
        drain(driver)
    }

    fun damageOn(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    test("each fighter triggers its own controller's watcher once") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "Fight Test Watcher")
        driver.putPermanentOnBattlefield(opp, "Fight Test Watcher")
        val mine = driver.putCreatureOnBattlefield(me, "Fight Test Bear")
        val theirs = driver.putCreatureOnBattlefield(opp, "Fight Test Zero Wall")
        val eventsBefore = driver.events.size

        brawl(driver, me, mine, theirs)

        driver.events.drop(eventsBefore).filterIsInstance<CreatureFoughtEvent>()
            .map { it.creatureId }.toSet() shouldBe setOf(mine, theirs)
        driver.getLifeTotal(me) shouldBe 21
        withClue("a 0-power fighter still fought (CR 701.14a)") { driver.getLifeTotal(opp) shouldBe 21 }
    }

    test("two of your creatures fighting each other trigger a creature-you-control watcher twice") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Fight Test Watcher")
        val a = driver.putCreatureOnBattlefield(me, "Fight Test Zero Wall")
        val b = driver.putCreatureOnBattlefield(me, "Fight Test Bear")

        brawl(driver, me, a, b)

        driver.getLifeTotal(me) shouldBe 22
    }

    test("a fighter that left the battlefield before the fight means no fight and no trigger (CR 701.14b)") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "Fight Test Watcher")
        val mine = driver.putCreatureOnBattlefield(me, "Fight Test Bear")
        val theirs = driver.putCreatureOnBattlefield(opp, "Fight Test Giant")
        val eventsBefore = driver.events.size

        brawl(driver, me, mine, theirs) { driver.moveToGraveyard(theirs) }

        driver.events.drop(eventsBefore).filterIsInstance<CreatureFoughtEvent>() shouldBe emptyList()
        driver.getLifeTotal(me) shouldBe 20
        damageOn(driver, mine) shouldBe 0
    }

    test("a fighter killed by the fight still fought") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "Fight Test Watcher")
        val mine = driver.putCreatureOnBattlefield(me, "Fight Test Bear")
        val theirs = driver.putCreatureOnBattlefield(opp, "Fight Test Giant")

        brawl(driver, me, mine, theirs)

        (mine in driver.getGraveyard(me)) shouldBe true
        driver.getLifeTotal(me) shouldBe 21
    }

    test("a creature that fights itself fights once and takes twice its power (CR 701.14c)") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Fight Test Watcher")
        val brawler = driver.putCreatureOnBattlefield(me, "Fight Test Self Brawler")
        driver.removeSummoningSickness(brawler)
        val abilityId = SelfBrawler.activatedAbilities.single().id
        driver.submit(ActivateAbility(playerId = me, sourceId = brawler, abilityId = abilityId))
            .outcome shouldBe Outcome.Done
        drain(driver)

        damageOn(driver, brawler) shouldBe 6
        driver.getLifeTotal(me) shouldBe 21
    }

    test("a this-creature-fights trigger fires only for the source's own fights") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val duelist = driver.putCreatureOnBattlefield(me, "Fight Test Duelist")
        val bear = driver.putCreatureOnBattlefield(me, "Fight Test Bear")
        val wall = driver.putCreatureOnBattlefield(opp, "Fight Test Zero Wall")

        brawl(driver, me, bear, wall)
        driver.getLifeTotal(me) shouldBe 20

        brawl(driver, me, duelist, wall)
        driver.getLifeTotal(me) shouldBe 25
    }
})
