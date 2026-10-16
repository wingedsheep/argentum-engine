package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.MustAttackDefender
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `MustAttack(defender = MustAttackDefender.AttachedPermanentController)` — "All creatures attack
 * enchanted creature's controller each combat if able" (Public Enemy). The requirement names a
 * player, read live from the Aura's host; it binds only creatures that could attack that player at
 * no cost (CR 508.1d), and only attacking that player — not their planeswalker — obeys it.
 */
class DirectedMustAttackTest : FunSpec({

    val bounty = card("Bounty Mark") {
        manaCost = "{2}{U}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature\nAll creatures attack enchanted creature's controller each combat if able."
        auraTarget = TargetObject(filter = TargetFilter.Creature)
        staticAbility {
            ability = MustAttack(GroupFilter.AllCreatures, defender = MustAttackDefender.AttachedPermanentController)
        }
    }

    val pacifist = card("Pacifist Mark") {
        manaCost = "{W}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature\nEnchanted creature can't attack."
        auraTarget = TargetObject(filter = TargetFilter.Creature)
        staticAbility { ability = CantAttack(GroupFilter.attachedCreature()) }
    }

    val walker = card("Test Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(bounty, pacifist, walker))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.enchant(owner: EntityId, auraName: String, host: EntityId): EntityId {
        val aura = putPermanentOnBattlefield(owner, auraName)
        replaceState(state.updateEntity(aura) { it.with(AttachedToComponent(host)) })
        return aura
    }

    fun GameTestDriver.mandatoryAttackers(): List<EntityId> =
        LegalActionEnumerator.create(cardRegistry).enumerate(state, player1)
            .single { it.actionType == "DeclareAttackers" }.mandatoryAttackers.orEmpty()

    fun GameTestDriver.readyCreature(owner: EntityId): EntityId =
        putCreatureOnBattlefield(owner, "Centaur Courser").also { removeSummoningSickness(it) }

    test("the directed requirement is not projected as a plain must-attack") {
        val driver = newDriver()
        val target = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        val attacker = driver.readyCreature(driver.player1)
        driver.enchant(driver.player1, "Bounty Mark", target)
        driver.state.projectedState.mustAttack(attacker) shouldBe false
        driver.state.projectedState.mustAttack(target) shouldBe false
    }

    test("a creature that can attack the enchanted creature's controller must attack that player") {
        val driver = newDriver()
        val target = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        val attacker = driver.readyCreature(driver.player1)
        driver.enchant(driver.player1, "Bounty Mark", target)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, emptyMap()).error shouldNotBe null
        driver.declareAttackers(driver.player1, mapOf(attacker to driver.player2)).error shouldBe null
    }

    test("the requirement is listed as a mandatory attacker for the client and AI") {
        val driver = newDriver()
        val target = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        val attacker = driver.readyCreature(driver.player1)
        driver.enchant(driver.player2, "Bounty Mark", target)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val mandatory = driver.mandatoryAttackers()
        mandatory shouldContain attacker
    }

    test("attacking the named player's planeswalker does not obey it") {
        val driver = newDriver()
        val target = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        val attacker = driver.readyCreature(driver.player1)
        val pw = driver.putPermanentOnBattlefield(driver.player2, "Test Walker")
        driver.replaceState(
            driver.state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3)) }
        )
        driver.enchant(driver.player1, "Bounty Mark", target)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, mapOf(attacker to pw)).error shouldNotBe null
        driver.declareAttackers(driver.player1, mapOf(attacker to driver.player2)).error shouldBe null
    }

    test("the enchanted creature's controller is under no requirement — their creatures can't attack them") {
        val driver = newDriver()
        val mine = driver.readyCreature(driver.player1)
        driver.enchant(driver.player2, "Bounty Mark", mine)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.mandatoryAttackers() shouldNotContain mine
        driver.declareAttackers(driver.player1, emptyMap()).error shouldBe null
    }

    test("a creature that can't attack is not bound and the declaration stays legal") {
        val driver = newDriver()
        val target = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        val attacker = driver.readyCreature(driver.player1)
        driver.enchant(driver.player1, "Bounty Mark", target)
        driver.enchant(driver.player2, "Pacifist Mark", attacker)

        // With no creature able to attack, the declaration is empty and combat moves on.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.state.step shouldBe Step.POSTCOMBAT_MAIN
    }

    test("the requirement ends when the Aura leaves") {
        val driver = newDriver()
        val target = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        driver.readyCreature(driver.player1)
        val aura = driver.enchant(driver.player1, "Bounty Mark", target)
        driver.replaceState(driver.state.updateEntity(aura) { it.without<AttachedToComponent>() })
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, emptyMap()).error shouldBe null
    }
})
