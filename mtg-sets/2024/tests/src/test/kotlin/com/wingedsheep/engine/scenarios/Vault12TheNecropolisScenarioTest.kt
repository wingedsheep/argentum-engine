package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.Vault12TheNecropolis
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Vault 12: The Necropolis (PIP #51): chapter II counts rad counters across *every* player when it
 * resolves, and chapter III's "Zombie or Mutant" gives a Zombie Mutant two counters, not four, and
 * skips other creatures and the opponent's Zombies.
 */
class Vault12TheNecropolisScenarioTest : FunSpec({
    // Run each real chapter's effect as a sorcery so the test doesn't walk turns through draw steps.
    fun chapterProbe(n: Int) = card("Vault 12 Chapter $n Probe") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell { effect = Vault12TheNecropolis.sagaChapters.single { it.chapter == n }.effect }
    }
    val probes = (1..3).associateWith { chapterProbe(it) }
    fun creature(name: String, types: String) = card(name) {
        manaCost = "{B}"
        typeLine = "Creature — $types"
        power = 1
        toughness = 1
    }
    val zombie = creature("Necropolis Zombie Probe", "Zombie")
    val mutant = creature("Necropolis Mutant Probe", "Mutant")

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(Vault12TheNecropolis, zombie, mutant) + probes.values)
        initMirrorMatch(Deck.of("Swamp" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.runChapter(n: Int) {
        val me = activePlayer!!
        val spell = putCardInHand(me, probes.getValue(n).name)
        giveMana(me, Color.BLACK, 1)
        castSpell(me, spell).error shouldBe null
        bothPass()
    }
    fun GameTestDriver.rad(player: EntityId): Int =
        state.getEntity(player)?.get<CountersComponent>()?.getCount(CounterType.RAD) ?: 0
    fun GameTestDriver.plusOnes(entity: EntityId): Int =
        state.getEntity(entity)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("chapter II creates one Zombie Mutant per rad counter among all players") {
        val d = driver()
        val me = d.activePlayer!!
        d.runChapter(1)
        d.rad(d.player1) shouldBe 3
        d.rad(d.player2) shouldBe 3

        val before = d.getPermanents(me).toSet()
        d.runChapter(2)
        val tokens = d.getPermanents(me).filterNot { it in before }
        tokens.size shouldBe 6
        tokens.forEach { token ->
            val cardComponent = d.state.getEntity(token)!!.get<CardComponent>()!!
            cardComponent.typeLine.subtypes.map { it.value }.toSet() shouldBe setOf("Zombie", "Mutant")
            cardComponent.colors shouldBe setOf(Color.BLACK)
        }
    }

    test("chapter III puts two counters on each Zombie or Mutant you control, once each") {
        val d = driver()
        val me = d.activePlayer!!
        val opponent = if (me == d.player1) d.player2 else d.player1
        val myZombie = d.putCreatureOnBattlefield(me, zombie.name)
        val myMutant = d.putCreatureOnBattlefield(me, mutant.name)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val theirZombie = d.putCreatureOnBattlefield(opponent, zombie.name)
        d.runChapter(1)
        val before = d.getPermanents(me).toSet()
        d.runChapter(2)
        val zombieMutant = d.getPermanents(me).first { it !in before }

        d.runChapter(3)

        d.plusOnes(myZombie) shouldBe 2
        d.plusOnes(myMutant) shouldBe 2
        d.plusOnes(zombieMutant) shouldBe 2
        d.plusOnes(bears) shouldBe 0
        d.plusOnes(theirZombie) shouldBe 0
    }
})
