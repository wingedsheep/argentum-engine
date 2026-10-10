package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.atq.cards.MishrasFactory
import com.wingedsheep.mtg.sets.definitions.dmu.cards.BraidsArisenNightmare
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Braids, Arisen Nightmare — {1}{B}{B} 3/3.
 *
 * At the beginning of your end step, you may sacrifice an artifact, creature, enchantment, land, or
 * planeswalker. If you do, each opponent may sacrifice a permanent of their choice that shares a
 * card type with it. For each opponent who doesn't, that player loses 2 life and you draw a card.
 *
 * Pins the last-known read of "it": the sacrificed permanent is gone by the time each opponent
 * chooses, so the card types they may answer with come from the snapshot the sacrifice captured —
 * an animated land still offers Creature and Artifact though its printed type line says only Land.
 */
class BraidsArisenNightmareScenarioTest : FunSpec({

    class Table(val driver: GameTestDriver, val you: EntityId, val opp1: EntityId, val opp2: EntityId)

    fun threePlayerTable(): Table {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + BraidsArisenNightmare)
        val ids = driver.initMultiplayer(
            decks = List(3) { Deck.of("Swamp" to 40) },
            startingPlayer = 0,
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Table(driver, ids[0], ids[1], ids[2])
    }

    fun GameTestDriver.nextSelection(): SelectCardsDecision? {
        var guard = 0
        while (pendingDecision !is SelectCardsDecision && guard++ < 60) {
            if (state.step == Step.CLEANUP) return null
            if (pendingDecision != null) autoResolveDecision()
            else if (state.priorityPlayerId != null) passPriority(state.priorityPlayerId!!)
        }
        return pendingDecision as? SelectCardsDecision
    }

    fun GameTestDriver.mintToken(id: EntityId) {
        replaceState(state.updateEntity(id) { it.with(TokenComponent) })
    }

    test("the sacrificed permanent is read as it last existed: an animated land offers Creature and Artifact") {
        val t = threePlayerTable()
        val d = t.driver
        d.putCreatureOnBattlefield(t.you, "Braids, Arisen Nightmare")
        val factory = d.putLandOnBattlefield(t.you, "Mishra's Factory")
        // Opponent 1 controls only a creature; opponent 2 only a land.
        val oppBear = d.putCreatureOnBattlefield(t.opp1, "Grizzly Bears")
        val oppLand = d.putLandOnBattlefield(t.opp2, "Swamp")
        val handBefore = d.getHandSize(t.you)

        // {1}: Mishra's Factory becomes a 2/2 artifact creature until end of turn. It's still a land.
        d.giveColorlessMana(t.you, 1)
        d.submit(ActivateAbility(t.you, factory, MishrasFactory.activatedAbilities[1].id)).error shouldBe null
        while (d.stackSize > 0) d.passPriority(d.state.priorityPlayerId!!)

        d.passPriorityUntil(Step.END)
        val mine = d.nextSelection() ?: error("expected Braids's sacrifice choice")
        mine.playerId shouldBe t.you
        d.submitCardSelection(t.you, listOf(factory))
        d.findPermanent(t.you, "Mishra's Factory") shouldBe null

        // Its printed type line says only Land; the creature it last was lets opponent 1 answer.
        val first = d.nextSelection() ?: error("expected opponent 1's choice")
        first.playerId shouldBe t.opp1
        first.options shouldContainExactlyInAnyOrder listOf(oppBear)
        d.submitCardSelection(t.opp1, listOf(oppBear))

        val second = d.nextSelection() ?: error("expected opponent 2's choice")
        second.playerId shouldBe t.opp2
        second.options shouldContainExactlyInAnyOrder listOf(oppLand)
        d.submitCardSelection(t.opp2, emptyList())
        d.passPriorityUntil(Step.CLEANUP)

        d.findPermanent(t.opp1, "Grizzly Bears") shouldBe null
        d.getLifeTotal(t.opp1) shouldBe 20
        d.getLifeTotal(t.opp2) shouldBe 18
        d.getHandSize(t.you) shouldBe handBefore + 1
    }

    test("an opponent with nothing that shares a card type loses 2 life without being able to answer") {
        val t = threePlayerTable()
        val d = t.driver
        d.putCreatureOnBattlefield(t.you, "Braids, Arisen Nightmare")
        val token = d.putCreatureOnBattlefield(t.you, "Grizzly Bears")
        d.mintToken(token)
        val handBefore = d.getHandSize(t.you)

        d.passPriorityUntil(Step.END)
        val mine = d.nextSelection() ?: error("expected Braids's sacrifice choice")
        d.submitCardSelection(mine.playerId, listOf(token))
        // Neither opponent controls a creature: any prompt they get offers nothing.
        generateSequence { d.nextSelection()?.takeIf { d.state.step == Step.END } }.take(2).forEach { choice ->
            choice.playerId shouldNotBe t.you
            choice.options shouldBe emptyList()
            d.submitCardSelection(choice.playerId, emptyList())
        }
        d.passPriorityUntil(Step.CLEANUP)

        d.getLifeTotal(t.opp1) shouldBe 18
        d.getLifeTotal(t.opp2) shouldBe 18
        d.getHandSize(t.you) shouldBe handBefore + 2
    }

    test("an opponent who could answer but declines loses 2 life, and you draw for each such opponent") {
        val t = threePlayerTable()
        val d = t.driver
        d.putCreatureOnBattlefield(t.you, "Braids, Arisen Nightmare")
        val myLand = d.putLandOnBattlefield(t.you, "Swamp")
        d.putLandOnBattlefield(t.opp1, "Swamp")
        d.putLandOnBattlefield(t.opp2, "Swamp")
        val handBefore = d.getHandSize(t.you)

        d.passPriorityUntil(Step.END)
        val mine = d.nextSelection() ?: error("expected Braids's sacrifice choice")
        d.submitCardSelection(mine.playerId, listOf(myLand))

        repeat(2) {
            val choice = d.nextSelection() ?: error("expected an opponent's choice")
            choice.playerId shouldNotBe t.you
            choice.options.size shouldBe 1
            d.submitCardSelection(choice.playerId, emptyList())
        }
        d.passPriorityUntil(Step.CLEANUP)

        d.getLifeTotal(t.opp1) shouldBe 18
        d.getLifeTotal(t.opp2) shouldBe 18
        d.getHandSize(t.you) shouldBe handBefore + 2
    }

    test("declining your own sacrifice does nothing") {
        val t = threePlayerTable()
        val d = t.driver
        d.putCreatureOnBattlefield(t.you, "Braids, Arisen Nightmare")
        d.putLandOnBattlefield(t.you, "Swamp")
        val handBefore = d.getHandSize(t.you)

        d.passPriorityUntil(Step.END)
        val mine = d.nextSelection() ?: error("expected Braids's sacrifice choice")
        d.submitCardSelection(mine.playerId, emptyList())
        d.passPriorityUntil(Step.CLEANUP)

        d.getLifeTotal(t.opp1) shouldBe 20
        d.getLifeTotal(t.opp2) shouldBe 20
        d.getHandSize(t.you) shouldBe handBefore
        d.findPermanent(t.you, "Swamp") shouldNotBe null
    }
})
