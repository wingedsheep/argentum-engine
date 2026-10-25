package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.CriticalHit
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Critical Hit: double strike for the turn, and a graveyard trigger on the die's *natural* 20 —
 * the face it shows before modifiers (CR 706.2), not its result.
 */
class CriticalHitScenarioTest : FunSpec({
    fun roller(name: String, modifier: Int) = card(name) {
        typeLine = "Artifact"
        activatedAbility { effect = Effects.RollDice(sides = 20, modifier = DynamicAmount.Fixed(modifier)) }
    }
    val plainD20 = roller("Test Plain d20", 0)
    val raisedD20 = roller("Test Raised d20", 5)
    val loweredD20 = roller("Test Lowered d20", -5)

    /** First seed whose d20 comes up [natural]. */
    fun seedFor(natural: Int): Long = (0L..10_000L).first { GameRng.seeded(it).nextInt(20).first + 1 == natural }

    fun setup() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(CriticalHit, plainD20, raisedD20, loweredD20))
        it.initMirrorMatch(Deck.of("Mountain" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** [roller] activates a d20 forced to [natural] and everything resolves; returns that die. */
    fun GameTestDriver.roll(roller: EntityId, rollerCard: CardDefinition, natural: Int): DieRolledEvent {
        val source = putPermanentOnBattlefield(roller, rollerCard.name)
        submit(ActivateAbility(roller, source, rollerCard.script.activatedAbilities.single().id)).outcome shouldBe Outcome.Done
        replaceState(state.copy(rng = GameRng.seeded(seedFor(natural))))
        val die = bothPass().events.filterIsInstance<DieRolledEvent>().single()
        while (state.stack.isNotEmpty()) bothPass()
        return die
    }

    test("the spell gives target creature double strike") {
        val d = setup(); val player = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(player, "Grizzly Bears")
        val hit = d.putCardInHand(player, "Critical Hit")
        d.giveMana(player, Color.RED, 2)
        d.castSpellWithTargets(player, hit, listOf(ChosenTarget.Permanent(bears))).error shouldBe null
        d.bothPass()
        d.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe true
        (hit in d.getGraveyard(player)) shouldBe true
    }

    test("a natural 20 returns it from your graveyard to your hand") {
        val d = setup(); val player = d.activePlayer!!
        val hit = d.putCardInGraveyard(player, "Critical Hit")
        d.roll(player, plainD20, natural = 20).natural shouldBe 20
        (hit in d.getHand(player)) shouldBe true
    }

    test("a lower face modified up to a result of 20 is not a natural 20") {
        val d = setup(); val player = d.activePlayer!!
        val hit = d.putCardInGraveyard(player, "Critical Hit")
        d.roll(player, raisedD20, natural = 15).result shouldBe 20
        (hit in d.getGraveyard(player)) shouldBe true
    }

    test("a natural 20 still counts when a modifier lowers its result") {
        val d = setup(); val player = d.activePlayer!!
        val hit = d.putCardInGraveyard(player, "Critical Hit")
        d.roll(player, loweredD20, natural = 20).result shouldBe 15
        (hit in d.getHand(player)) shouldBe true
    }

    test("an opponent's natural 20 doesn't return it") {
        val d = setup(); val player = d.activePlayer!!; val opponent = d.getOpponent(player)
        val hit = d.putCardInGraveyard(player, "Critical Hit")
        d.passPriority(player)
        d.roll(opponent, plainD20, natural = 20)
        (hit in d.getGraveyard(player)) shouldBe true
    }

    test("it doesn't trigger from your hand") {
        val d = setup(); val player = d.activePlayer!!
        val hit = d.putCardInHand(player, "Critical Hit")
        d.roll(player, plainD20, natural = 20)
        d.state.stack.isEmpty() shouldBe true
        (hit in d.getHand(player)) shouldBe true
    }
})
