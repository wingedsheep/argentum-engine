package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.SummonBahamut
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Behaviour coverage for **Summon: Bahamut** (FIN), an Enchantment Creature — Saga Dragon (9/9, Flying).
 *
 *   I, II — Destroy up to one target nonland permanent.
 *   III   — Draw two cards.
 *   IV    — Mega Flare — This creature deals damage equal to the total mana value of other
 *           permanents you control to each opponent.
 *
 * The generic saga-creature machinery (lore accrual, chapter triggers, final-chapter sacrifice) is
 * proven by [CreatureSagaTest]; this pins Bahamut's specific chapters: the optional nonland-permanent
 * destroy (chapter I/II), the chapter III draw, and the chapter IV mana-value-scaled damage to each
 * opponent (an `AggregateBattlefield(SUM, MANA_VALUE, excludeSelf = true)` dynamic amount).
 */
class SummonBahamutScenarioTest : FunSpec({

    val projector = StateProjector()

    // Plain vanilla bodies with distinct, non-trivial mana values so the Mega Flare total is
    // unambiguously the SUM of their mana values (3 + 5 = 8), not a count or a power total.
    val mvThree = card("MV Three Beast") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Beast"
        power = 1
        toughness = 1
    }
    val mvFive = card("MV Five Beast") {
        manaCost = "{4}{G}"
        typeLine = "Creature — Beast"
        power = 1
        toughness = 1
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SummonBahamut, mvThree, mvFive))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castBahamut(controller: EntityId): EntityId {
        val spell = putCardInHand(controller, "Summon: Bahamut")
        giveColorlessMana(controller, 9)
        castSpell(controller, spell)
        return spell
    }

    /** Resolve the whole stack, declining every optional target ([chooseTargets] default = none). */
    fun GameTestDriver.resolveStack(chooseTargets: () -> List<EntityId> = { emptyList() }) {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 100) {
            val pd = state.pendingDecision
            when {
                pd is ChooseTargetsDecision -> submitTargetSelection(pd.playerId, chooseTargets())
                pd != null -> autoResolveDecision()
                else -> bothPass()
            }
        }
    }

    fun GameTestDriver.lore(saga: EntityId): Int =
        state.getEntity(saga)?.get<CountersComponent>()?.getCount(CounterType.LORE) ?: Int.MAX_VALUE

    /** Advance turns until the saga has at least [targetLore] lore, declining any chapter targets. */
    fun GameTestDriver.advanceToLore(saga: EntityId, targetLore: Int) {
        var guard = 0
        while (guard++ < 1000) {
            if (state.gameOver) throw AssertionError("Game ended before reaching lore $targetLore")
            if (lore(saga) >= targetLore) return
            val pd = state.pendingDecision
            when {
                pd is ChooseTargetsDecision -> submitTargetSelection(pd.playerId, emptyList())
                pd != null -> autoResolveDecision()
                state.priorityPlayerId != null -> {
                    autoSubmitCombatDeclarationIfNeeded()
                    passPriority(state.priorityPlayerId!!)
                }
            }
        }
        throw AssertionError("Saga never reached lore $targetLore")
    }

    /** Advance turns until the saga has left the battlefield (sacrificed after its final chapter). */
    fun GameTestDriver.advanceUntilGone(controller: EntityId, name: String) {
        var guard = 0
        while (guard++ < 1000) {
            if (findPermanent(controller, name) == null) return
            if (state.gameOver) throw AssertionError("Game ended before the saga was sacrificed")
            val pd = state.pendingDecision
            when {
                pd is ChooseTargetsDecision -> submitTargetSelection(pd.playerId, emptyList())
                pd != null -> autoResolveDecision()
                state.priorityPlayerId != null -> {
                    autoSubmitCombatDeclarationIfNeeded()
                    passPriority(state.priorityPlayerId!!)
                }
            }
        }
        throw AssertionError("Saga was never sacrificed")
    }

    test("enters as a 9/9 flying Saga Dragon and chapter I destroys the chosen nonland permanent") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        // A nonland permanent for chapter I to destroy.
        val victim = driver.putCreatureOnBattlefield(opponent, "MV Three Beast")

        driver.castBahamut(active)
        // Saga resolves, enters, chapter I triggers — choose to destroy the opponent's creature.
        driver.resolveStack(chooseTargets = { listOf(victim) })

        // It is simultaneously a creature and a Saga, 9/9 with Flying.
        val saga = driver.findPermanent(active, "Summon: Bahamut")
        saga shouldNotBe null
        val projected = projector.project(driver.state)
        projected.isCreature(saga!!) shouldBe true
        projected.hasType(saga, "Saga") shouldBe true
        projected.hasType(saga, "Dragon") shouldBe true
        projected.getPower(saga) shouldBe 9
        projected.getToughness(saga) shouldBe 9
        projected.hasKeyword(saga, Keyword.FLYING) shouldBe true

        // Chapter I destroyed the targeted nonland permanent.
        driver.findPermanent(opponent, "MV Three Beast") shouldBe null
        driver.getGraveyardCardNames(opponent).contains("MV Three Beast") shouldBe true
    }

    test("chapter I/II decline (up to one), chapter III draws two, chapter IV Mega Flares each opponent, then sacrifices") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        val oppStartLife = driver.getLifeTotal(opponent)

        // Other permanents you control: total mana value 3 + 5 = 8 (the saga itself is excluded).
        driver.putCreatureOnBattlefield(active, "MV Three Beast")
        driver.putCreatureOnBattlefield(active, "MV Five Beast")

        driver.castBahamut(active)
        // Chapter I on entry — decline the optional target (destroy nothing).
        driver.resolveStack(chooseTargets = { emptyList() })
        val saga = driver.findPermanent(active, "Summon: Bahamut")!!
        driver.lore(saga) shouldBe 1

        // Advance to chapter III's main (chapter II is auto-declined en route). Stop with the
        // chapter III ability on the stack so the draw can be isolated from the turn's own draw.
        driver.advanceToLore(saga, 3)
        val handBeforeDraw = driver.getHandSize(active)
        driver.resolveStack() // chapter III resolves
        driver.getHandSize(active) shouldBe handBeforeDraw + 2

        // Advance through chapter IV (Mega Flare) and the final-chapter sacrifice.
        driver.advanceUntilGone(active, "Summon: Bahamut")

        // Mega Flare dealt 8 (total MV of the two other permanents) to the opponent; chapters I–III
        // never touched the opponent's life.
        driver.getLifeTotal(opponent) shouldBe oppStartLife - 8
        // CR 714.4 — sacrificed after its final chapter.
        driver.findPermanent(active, "Summon: Bahamut") shouldBe null
        driver.getGraveyardCardNames(active).contains("Summon: Bahamut") shouldBe true
    }
})
