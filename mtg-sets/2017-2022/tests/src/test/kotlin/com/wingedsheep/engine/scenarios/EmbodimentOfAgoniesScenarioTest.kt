package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m20.cards.EmbodimentOfAgonies
import com.wingedsheep.mtg.sets.definitions.shm.cards.Scar
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Embodiment of Agonies — "This creature enters with a +1/+1 counter on it for each different
 * mana cost among nonland cards in your graveyard."
 */
class EmbodimentOfAgoniesScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(EmbodimentOfAgonies, Scar))
        initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
    }

    fun GameTestDriver.castEmbodiment(you: EntityId): EntityId? {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        giveMana(you, Color.BLACK, 3)
        val card = putCardInHand(you, "Embodiment of Agonies")
        castSpell(you, card).error shouldBe null
        bothPass()
        return findPermanent(you, "Embodiment of Agonies")
    }

    fun GameTestDriver.plusCounters(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("one counter per different mana cost: duplicates count once, lands not at all, hybrid is its own cost") {
        val d = setup()
        val you = d.activePlayer!!
        listOf("Lightning Bolt", "Goblin Guide", "Doom Blade", "Scar", "Forest")
            .forEach { d.putCardInGraveyard(you, it) }

        val embodiment = d.castEmbodiment(you)!!

        // {R} (Bolt and Goblin Guide share it), {1}{B}, {R/B} — Forest has no mana cost.
        d.plusCounters(embodiment) shouldBe 3
        d.state.projectedState.getPower(embodiment) shouldBe 3
        d.state.projectedState.getToughness(embodiment) shouldBe 3
    }

    test("only your graveyard counts, and with nothing in it the 0/0 dies") {
        val d = setup()
        val you = d.activePlayer!!
        d.putCardInGraveyard(d.getOpponent(you), "Doom Blade")

        d.castEmbodiment(you) shouldBe null
    }
})
