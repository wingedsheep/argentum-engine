package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.UnstableAmulet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unstable Amulet (MH3 #142) — two energy on entry; 1 damage to each opponent whenever you cast a
 * spell from anywhere other than your hand; {T}, Pay {E}{E}: impulse the top card, playable until
 * the Amulet exiles another card.
 */
class UnstableAmuletScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + UnstableAmulet)
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.energy(): Int =
        state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0
    fun GameTestDriver.giveEnergy(n: Int) = replaceState(state.updateEntity(player1) {
        it.with(CountersComponent(mapOf(CounterType.ENERGY to n)))
    })
    val ability = UnstableAmulet.activatedAbilities.single().id
    fun GameTestDriver.activate(amulet: EntityId) = submit(ActivateAbility(player1, amulet, ability))
    fun GameTestDriver.canPlay(card: EntityId) = state.mayPlayPermissions.any { card in it.cardIds }

    test("entering gives two energy") {
        val d = driver()
        val amulet = d.putCardInHand(d.player1, "Unstable Amulet")
        d.giveMana(d.player1, Color.RED, 2)
        d.castSpell(d.player1, amulet).error shouldBe null
        d.bothPass() // resolve the artifact spell
        d.bothPass() // resolve the enters trigger
        d.energy() shouldBe 2
    }

    test("impulse a card, cast it from exile, and the cast pings each opponent") {
        val d = driver()
        val amulet = d.putPermanentOnBattlefield(d.player1, "Unstable Amulet")
        d.giveEnergy(3)
        val bolt = d.putCardOnTopOfLibrary(d.player1, "Lightning Bolt")

        d.activate(amulet).error shouldBe null
        d.isTapped(amulet) shouldBe true
        d.energy() shouldBe 1
        d.bothPass()
        d.getExile(d.player1) shouldBe listOf(bolt)
        d.canPlay(bolt) shouldBe true

        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, bolt, targets = listOf(d.player2)).error shouldBe null
        withClue("the Amulet trigger sits above the Bolt") { d.state.stack.size shouldBe 2 }
        while (d.state.stack.isNotEmpty()) d.bothPass()
        d.getLifeTotal(d.player2) shouldBe 20 - 1 - 3
        d.getLifeTotal(d.player1) shouldBe 20
    }

    test("casting a spell from hand does not trigger the damage") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Unstable Amulet")
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, bolt, targets = listOf(d.player2)).error shouldBe null
        d.state.stack.size shouldBe 1
        d.bothPass()
        d.getLifeTotal(d.player2) shouldBe 17
    }

    test("needs two energy, and exiling another card revokes the earlier permission") {
        val d = driver()
        val amulet = d.putPermanentOnBattlefield(d.player1, "Unstable Amulet")
        d.giveEnergy(1)
        withClue("one energy can't pay {E}{E}") { d.activate(amulet).error shouldNotBe null }

        d.giveEnergy(4)
        val forest = d.putCardOnTopOfLibrary(d.player1, "Forest")
        d.activate(amulet).error shouldBe null
        d.bothPass()
        d.canPlay(forest) shouldBe true

        d.untapPermanent(amulet)
        val island = d.putCardOnTopOfLibrary(d.player1, "Island")
        d.activate(amulet).error shouldBe null
        d.bothPass()
        d.getExile(d.player1).toSet() shouldBe setOf(forest, island)
        d.canPlay(island) shouldBe true
        withClue("the earlier card is no longer playable") { d.canPlay(forest) shouldBe false }

        withClue("the latest card's land play is still allowed") { d.playLand(d.player1, island).error shouldBe null }
    }
})
