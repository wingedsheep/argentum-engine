package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.CreatureDiedThisTurnMatching
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.engine.support.creatureDeath
import com.wingedsheep.engine.support.recordCreatureDeaths
import io.kotest.matchers.string.shouldContain
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `CreatureDiedThisTurnMatching` — its subtype test, its `notNamed` test (Ebondeath, Dracolich) and its
 * `player` scope — "a non-Skeleton creature died under
 * your control this turn" (Cult Conscript) versus the global "a non-Zombie creature died this turn"
 * (Undead Sprinter).
 *
 * Each death is recorded under its last-known controller with its last-known subtypes, so the
 * scoped form needs one death that is both under that player's control and passes the subtype
 * test — two different deaths can't satisfy it between them.
 */
class CreatureDiedThisTurnMatchingTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    val evaluator = PredicateEvaluator(cardRegistry = null)

    fun GameTestDriver.holds(condition: Condition): Boolean =
        evaluator.conditions.evaluate(state, condition, EffectContext(sourceId = null, controllerId = player1))

    /** Bolt [creature] (all test creatures here have toughness <= 3). */
    fun GameTestDriver.kill(creature: EntityId) {
        val bolt = putCardInHand(player1, "Lightning Bolt")
        giveMana(player1, Color.RED, 1)
        castSpell(player1, bolt, targets = listOf(creature)).error shouldBe null
        bothPass()
    }

    val nonGoblinMine = Conditions.NonSubtypeCreatureDiedThisTurn(Subtype.GOBLIN, player = Player.You)
    val nonGoblinAnywhere = Conditions.NonSubtypeCreatureDiedThisTurn(Subtype.GOBLIN)
    val goblinMine = Conditions.SubtypeCreatureDiedThisTurn(Subtype.GOBLIN, player = Player.You)
    val goblinTheirs = Conditions.SubtypeCreatureDiedThisTurn(Subtype.GOBLIN, player = Player.AnOpponent)

    test("nothing died — every form is false") {
        val d = driver()
        d.holds(nonGoblinMine) shouldBe false
        d.holds(nonGoblinAnywhere) shouldBe false
        d.holds(goblinMine) shouldBe false
    }

    test("your non-Goblin died — the scoped and global non-Goblin forms both hold") {
        val d = driver()
        d.kill(d.putPermanentOnBattlefield(d.player1, "Savannah Lions"))
        d.holds(nonGoblinMine) shouldBe true
        d.holds(nonGoblinAnywhere) shouldBe true
        d.holds(goblinMine) shouldBe false
    }

    test("an opponent's non-Goblin died — global holds, your scope does not") {
        val d = driver()
        d.kill(d.putPermanentOnBattlefield(d.player2, "Savannah Lions"))
        d.holds(nonGoblinAnywhere) shouldBe true
        d.holds(nonGoblinMine) shouldBe false
    }

    test("your Goblin and an opponent's non-Goblin died — no single death satisfies the scoped form") {
        val d = driver()
        d.kill(d.putPermanentOnBattlefield(d.player1, "Goblin Guide"))
        d.kill(d.putPermanentOnBattlefield(d.player2, "Savannah Lions"))
        d.holds(nonGoblinMine) shouldBe false
        d.holds(goblinMine) shouldBe true
        d.holds(nonGoblinAnywhere) shouldBe true
        d.holds(goblinTheirs) shouldBe false
    }

    val notLions = Conditions.CreatureNotNamedDiedThisTurn("Savannah Lions")
    val notLionsMine = Conditions.CreatureNotNamedDiedThisTurn("Savannah Lions", player = Player.You)

    test("not-named form — false with no deaths and when only that name died") {
        val d = driver()
        d.holds(notLions) shouldBe false
        d.kill(d.putPermanentOnBattlefield(d.player1, "Savannah Lions"))
        d.kill(d.putPermanentOnBattlefield(d.player2, "Savannah Lions"))
        d.holds(notLions) shouldBe false
    }

    test("not-named form — any differently named death satisfies it, under any controller") {
        val d = driver()
        d.kill(d.putPermanentOnBattlefield(d.player1, "Savannah Lions"))
        d.kill(d.putPermanentOnBattlefield(d.player2, "Goblin Guide"))
        d.holds(notLions) shouldBe true
        d.holds(notLionsMine) shouldBe false
        d.holds(Conditions.CreatureNotNamedDiedThisTurn("Goblin Guide", player = Player.You)) shouldBe true
    }

    test("subtype and name tests combine on a single death") {
        val d = driver()
        d.kill(d.putPermanentOnBattlefield(d.player1, "Goblin Guide"))
        d.kill(d.putPermanentOnBattlefield(d.player1, "Savannah Lions"))
        val goblin = GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN)
        d.holds(CreatureDiedThisTurnMatching(goblin.notNamed("Goblin Guide"))) shouldBe false
        d.holds(CreatureDiedThisTurnMatching(goblin.named("Goblin Guide"))) shouldBe true
        d.holds(CreatureDiedThisTurnMatching(GameObjectFilter.Creature.notSubtype(Subtype.GOBLIN).notNamed("Goblin Guide"))) shouldBe true
    }

    test("a face-down creature dies nameless and with no subtypes (CR 708.2a)") {
        val d = driver()
        d.recordCreatureDeaths(
            d.player1,
            creatureDeath(name = "Savannah Lions").copy(name = null, wasFaceDown = true, subtypes = emptySet())
        )
        d.holds(notLions) shouldBe true
        d.holds(CreatureDiedThisTurnMatching(GameObjectFilter.Creature.named("Savannah Lions"))) shouldBe false
        d.holds(nonGoblinMine) shouldBe true
    }

    test("description names the filter and the scope") {
        nonGoblinMine.description shouldContain "non-Goblin"
        nonGoblinMine.description shouldContain "died under your control this turn"
        notLions.description shouldContain "Savannah Lions"
    }
})
