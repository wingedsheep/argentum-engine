package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mor.cards.WeightOfConscience
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Weight of Conscience — {1}{W} Aura.
 * Enchanted creature can't attack.
 * Tap two untapped creatures you control that share a creature type: Exile enchanted creature.
 */
class WeightOfConscienceScenarioTest : FunSpec({

    val abilityId = WeightOfConscience.activatedAbilities.first().id

    fun creature(name: String, vararg types: String) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse("{1}"),
        subtypes = types.map { Subtype(it) }.toSet(),
        power = 2,
        toughness = 2
    )

    val goblinWarrior = creature("Goblin Warrior", "Goblin", "Warrior")
    val elfWarrior = creature("Elf Warrior", "Elf", "Warrior")
    val pureElf = creature("Pure Elf", "Elf")
    val pureGoblin = creature("Pure Goblin", "Goblin")

    data class Board(val driver: GameTestDriver, val me: EntityId, val aura: EntityId, val victim: EntityId)

    fun setup(): Board {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WeightOfConscience, goblinWarrior, elfWarrior, pureElf, pureGoblin))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val victim = driver.putCreatureOnBattlefield(driver.getOpponent(me), "Grizzly Bears")
        val aura = driver.putPermanentOnBattlefield(me, "Weight of Conscience")
        var state = driver.state.updateEntity(aura) { it.with(AttachedToComponent(victim)) }
        state = state.updateEntity(victim) { it.with(AttachmentsComponent(listOf(aura))) }
        driver.replaceState(state)
        return Board(driver, me, aura, victim)
    }

    fun Board.offered() = driver.legalActions(me).firstOrNull {
        (it.action as? ActivateAbility)?.sourceId == aura
    }

    fun Board.activate(first: EntityId, second: EntityId) = driver.submit(
        ActivateAbility(
            playerId = me,
            sourceId = aura,
            abilityId = abilityId,
            costPayment = AdditionalCostPayment(tappedPermanents = listOf(first, second))
        )
    )

    test("tapping two creatures that share a creature type exiles the enchanted creature") {
        val board = setup()
        val goblin = board.driver.putCreatureOnBattlefield(board.me, "Goblin Warrior")
        val elf = board.driver.putCreatureOnBattlefield(board.me, "Elf Warrior")

        board.activate(goblin, elf).outcome shouldBe Outcome.Done
        board.driver.bothPass()

        board.driver.getExile(board.driver.getOpponent(board.me)) shouldBe listOf(board.victim)
        board.driver.isTapped(goblin) shouldBe true
        board.driver.isTapped(elf) shouldBe true
    }

    test("only creatures that can join a sharing pair are offered for the tap cost") {
        val board = setup()
        val goblin = board.driver.putCreatureOnBattlefield(board.me, "Goblin Warrior")
        val elf = board.driver.putCreatureOnBattlefield(board.me, "Elf Warrior")
        board.driver.putCreatureOnBattlefield(board.me, "Pure Goblin")
        val loner = board.driver.putCreatureOnBattlefield(board.me, "Grizzly Bears")

        val offered = board.offered()
        offered shouldNotBe null
        val targets = offered!!.additionalCostInfo!!.validTapTargets
        targets.contains(loner) shouldBe false
        targets.containsAll(listOf(goblin, elf)) shouldBe true
    }

    test("the ability isn't offered when no two creatures share a creature type") {
        val board = setup()
        board.driver.putCreatureOnBattlefield(board.me, "Pure Goblin")
        board.driver.putCreatureOnBattlefield(board.me, "Pure Elf")
        board.driver.putCreatureOnBattlefield(board.me, "Grizzly Bears")

        board.offered() shouldBe null
    }

    test("a chosen pair with no creature type in common is rejected") {
        val board = setup()
        val goblin = board.driver.putCreatureOnBattlefield(board.me, "Pure Goblin")
        val elf = board.driver.putCreatureOnBattlefield(board.me, "Pure Elf")
        // Elf Warrior makes the cost payable (it shares Elf with Pure Elf), but not with this pair.
        board.driver.putCreatureOnBattlefield(board.me, "Elf Warrior")
        board.driver.putCreatureOnBattlefield(board.me, "Goblin Warrior")

        board.activate(goblin, elf).outcome shouldNotBe Outcome.Done
        board.driver.isTapped(goblin) shouldBe false
        board.driver.isTapped(elf) shouldBe false
        board.driver.getPermanents(board.driver.getOpponent(board.me)).contains(board.victim) shouldBe true
    }
})
