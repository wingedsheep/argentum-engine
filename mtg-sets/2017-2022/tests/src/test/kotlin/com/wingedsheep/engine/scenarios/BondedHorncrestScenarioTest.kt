package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.xln.cards.BondedHorncrest
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class BondedHorncrestScenarioTest : FunSpec({
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + BondedHorncrest)
        it.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("another creature on the battlefield is insufficient but two Horncrests can attack together") {
        val driver = driver()
        val first = driver.putCreatureOnBattlefield(driver.player1, "Bonded Horncrest")
        val second = driver.putCreatureOnBattlefield(driver.player1, "Bonded Horncrest")
        driver.removeSummoningSickness(first)
        driver.removeSummoningSickness(second)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, listOf(first), driver.player2).error shouldNotBe null
        driver.declareAttackers(driver.player1, listOf(first, second), driver.player2).error shouldBe null
    }

    test("Horncrest cannot block alone but two Horncrests can block different attackers") {
        val driver = driver()
        val first = driver.putCreatureOnBattlefield(driver.player1, "Bonded Horncrest")
        val second = driver.putCreatureOnBattlefield(driver.player1, "Bonded Horncrest")
        val attacker1 = driver.putCreatureOnBattlefield(driver.player2, "Bonded Horncrest")
        val attacker2 = driver.putCreatureOnBattlefield(driver.player2, "Bonded Horncrest")
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player2, listOf(attacker1, attacker2), driver.player1).error shouldBe null
        driver.bothPass()

        driver.declareBlockers(driver.player1, mapOf(first to listOf(attacker1))).error shouldNotBe null
        driver.declareBlockers(
            driver.player1, mapOf(first to listOf(attacker1), second to listOf(attacker2))
        ).error shouldBe null
    }

    test("Horncrest attacks with only a teammate's creature and still deals damage after that partner dies") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + BondedHorncrest)
        val players = driver.initMultiplayer(
            decks = List(4) { Deck.of("Mountain" to 40) },
            format = Format.TwoHeadedGiant(),
            teams = listOf(listOf(0, 1), listOf(2, 3)),
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val horncrest = driver.putCreatureOnBattlefield(players[0], "Bonded Horncrest")
        val partner = driver.putCreatureOnBattlefield(players[1], "Centaur Courser")
        driver.removeSummoningSickness(horncrest)
        driver.removeSummoningSickness(partner)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(players[0], listOf(horncrest), players[2]).error shouldNotBe null
        driver.declareAttackers(players[0], listOf(horncrest, partner), players[2]).error shouldBe null
        driver.state.getEntity(horncrest)!!.has<AttackingComponent>() shouldBe true
        driver.state.getEntity(partner)!!.has<AttackingComponent>() shouldBe true

        val bolt = driver.putCardInHand(players[0], "Lightning Bolt")
        driver.giveMana(players[0], Color.RED, 1)
        driver.castSpell(players[0], bolt, listOf(partner)).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.getGraveyard(players[1]).contains(partner) shouldBe true
        driver.state.getEntity(horncrest)!!.has<AttackingComponent>() shouldBe true

        val lifeBeforeDamage = driver.getLifeTotal(players[2])
        driver.declareNoBlockers(players[2]).error shouldBe null
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.getLifeTotal(players[2]) shouldBe lifeBeforeDamage - 5
        driver.events.filterIsInstance<DamageDealtEvent>().any {
            it.sourceId == horncrest && it.targetId == players[2] && it.amount == 5 && it.isCombatDamage
        } shouldBe true
    }

    test("Horncrest blocks with only a teammate's creature and still deals damage after that partner dies") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + BondedHorncrest)
        val players = driver.initMultiplayer(
            decks = List(4) { Deck.of("Mountain" to 40) },
            format = Format.TwoHeadedGiant(),
            teams = listOf(listOf(0, 1), listOf(2, 3)),
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val horncrest = driver.putCreatureOnBattlefield(players[2], "Bonded Horncrest")
        val partner = driver.putCreatureOnBattlefield(players[3], "Centaur Courser")
        val attacker1 = driver.putCreatureOnBattlefield(players[0], "Centaur Courser")
        val attacker2 = driver.putCreatureOnBattlefield(players[0], "Centaur Courser")
        driver.removeSummoningSickness(attacker1)
        driver.removeSummoningSickness(attacker2)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(players[0], listOf(attacker1, attacker2), players[2]).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        driver.declareBlockers(players[2], mapOf(horncrest to listOf(attacker1))).error shouldNotBe null
        driver.declareBlockers(
            players[2], mapOf(horncrest to listOf(attacker1), partner to listOf(attacker2))
        ).error shouldBe null
        driver.state.getEntity(horncrest)!!.has<BlockingComponent>() shouldBe true
        driver.state.getEntity(partner)!!.has<BlockingComponent>() shouldBe true

        var passesToCaster = 0
        while (driver.priorityPlayer != players[0] && passesToCaster++ < players.size) {
            driver.passPriority(driver.priorityPlayer!!).error shouldBe null
            driver.state.step shouldBe Step.DECLARE_BLOCKERS
        }
        driver.priorityPlayer shouldBe players[0]
        val bolt = driver.putCardInHand(players[0], "Lightning Bolt")
        driver.giveMana(players[0], Color.RED, 1)
        driver.castSpell(players[0], bolt, listOf(partner)).error shouldBe null
        var passes = 0
        while (driver.stackSize > 0 && passes++ < 10) {
            driver.bothPass().error shouldBe null
        }
        driver.stackSize shouldBe 0
        driver.getGraveyard(players[3]).contains(partner) shouldBe true
        driver.state.getEntity(horncrest)!!.has<BlockingComponent>() shouldBe true

        val lifeBeforeDamage = driver.getLifeTotal(players[2])
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.getLifeTotal(players[2]) shouldBe lifeBeforeDamage
        driver.getGraveyard(players[0]).contains(attacker1) shouldBe true
        driver.events.filterIsInstance<DamageDealtEvent>().any {
            it.sourceId == horncrest && it.targetId == attacker1 && it.amount == 5 && it.isCombatDamage
        } shouldBe true
    }
})
