package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.BolgOfTheNorth
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Bolg of the North (HOB #148) — {3}{B}{R} 5/5 Legendary Goblin Soldier.
 *
 * "When Bolg enters, you may sacrifice another creature. When you do, Bolg deals damage equal to
 *  that creature's power to another target creature. If excess damage was dealt this way, amass
 *  Goblins X, where X is that excess damage."
 *
 * The load-bearing detail is *when* the sacrificed creature's power is read. The reflexive ability
 * resolves from a fresh `EffectContext` on the far side of a stack round-trip, which carries the
 * pipeline but not the sacrifice's last-known-information snapshot — so the card stores the power
 * into the pipeline while the creature is still on the battlefield. The lord test below is the one
 * that fails if that snapshot slips to resolution time: the graveyard card would read its printed
 * power and drop the +2/+0.
 */
class BolgOfTheNorthScenarioTest : FunSpec({

    val projector = StateProjector()

    // "Other creatures you control get +2/+0" — makes projected power differ from printed power,
    // so the damage amount proves which one the card actually read.
    val GoblinWarchanter = card("Goblin Warchanter") {
        manaCost = "{2}{R}"
        colorIdentity = "R"
        typeLine = "Creature — Goblin Warrior"
        power = 1
        toughness = 1
        oracleText = "Other creatures you control get +2/+0."
        staticAbility {
            ability = ModifyStats(
                powerBonus = 2,
                toughnessBonus = 0,
                filter = GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true),
            )
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BolgOfTheNorth, GoblinWarchanter))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castBolg(driver: GameTestDriver, me: EntityId) {
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveMana(me, Color.RED, 1)
        driver.giveColorlessMana(me, 3)
        val card = driver.putCardInHand(me, "Bolg of the North")
        driver.castSpell(me, card).isSuccess shouldBe true
        driver.bothPass() // resolve the creature spell
        driver.bothPass() // resolve the enters trigger off the stack
    }

    fun GameTestDriver.armiesControlledBy(player: EntityId): List<EntityId> {
        val projected = projector.project(state)
        return projected.getBattlefieldControlledBy(player)
            .filter { projected.isCreature(it) && projected.hasSubtype(it, "Army") }
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("sacrificing a creature deals its power, and the excess amasses that many Goblins") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Hill Giant is a 3/3; Grizzly Bears a 2/2. 3 damage to a 2/2 is 1 excess.
        val fodder = driver.putCreatureOnBattlefield(me, "Hill Giant")
        val victim = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")

        castBolg(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitTargetSelection(me, listOf(fodder))

        // The "When you do" reflexive trigger goes on the stack; pick the damage target.
        driver.submitTargetSelection(me, listOf(victim))
        driver.bothPass()

        driver.getGraveyard(me).contains(fodder) shouldBe true
        driver.getGraveyard(opp).contains(victim) shouldBe true

        val armies = driver.armiesControlledBy(me)
        armies.size shouldBe 1
        driver.plusOneCounters(armies.single()) shouldBe 1
        projector.project(driver.state).hasSubtype(armies.single(), "Goblin") shouldBe true
    }

    test("the power is the sacrificed creature's last power on the battlefield, lord bonus included") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Warchanter makes Grizzly Bears a 4/2 while it lives; its card in the graveyard is a 2/2.
        driver.putCreatureOnBattlefield(me, "Goblin Warchanter")
        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        projector.project(driver.state).getPower(fodder) shouldBe 4

        // Wall of Ice is a 0/7, so it survives and keeps the damage marked on it — the assertion
        // then reads the exact amount Bolg dealt rather than inferring it from a death.
        val victim = driver.putCreatureOnBattlefield(opp, "Wall of Ice")

        castBolg(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitTargetSelection(me, listOf(fodder))
        driver.submitTargetSelection(me, listOf(victim))
        driver.bothPass()

        withClue(
            "4 = the Bears' power on the battlefield. The printed 2 would mean the graveyard card " +
                "was read; null/0 would mean the stored snapshot never crossed the pause."
        ) {
            driver.state.getEntity(victim)
                ?.get<DamageComponent>()
                ?.amount shouldBe 4
        }
    }

    test("damage short of lethal deals no excess, so nothing is amassed") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // 2 power into a 3/3: lethal is never reached, so no excess and no Army.
        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val victim = driver.putCreatureOnBattlefield(opp, "Hill Giant")

        castBolg(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitTargetSelection(me, listOf(fodder))
        driver.submitTargetSelection(me, listOf(victim))
        driver.bothPass()

        driver.getGraveyard(me).contains(fodder) shouldBe true
        driver.getPermanents(opp).contains(victim) shouldBe true
        driver.armiesControlledBy(me).size shouldBe 0
    }

    test("declining the optional sacrifice deals no damage and amasses nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val victim = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")

        castBolg(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        driver.getPermanents(me).contains(fodder) shouldBe true
        driver.getPermanents(opp).contains(victim) shouldBe true
        driver.armiesControlledBy(me).size shouldBe 0
    }

    test("with no other creature the sacrifice is infeasible and no decision is offered") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        castBolg(driver, me)

        driver.pendingDecision shouldBe null
        driver.armiesControlledBy(me).size shouldBe 0
    }
})
