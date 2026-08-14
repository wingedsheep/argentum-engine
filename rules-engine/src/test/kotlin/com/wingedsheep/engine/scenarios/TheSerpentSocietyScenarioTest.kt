package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.msh.cards.TheSerpentSociety
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The Serpent Society (MSH #226) — deathtouch, "Ward—Get five poison counters", and "Whenever
 * another creature you control with deathtouch dies, each opponent sacrifices a nontoken creature
 * of their choice."
 *
 * Three things are worth pinning: the ward's counters land on the *targeting* player and never
 * become unpayable; the death trigger's "with deathtouch" is matched against last-known
 * information (the creature is already in the graveyard when it is detected); and the trigger is
 * OTHER-bound, so the Society's own death does nothing.
 */
class TheSerpentSocietyScenarioTest : FunSpec({

    val Venomous = CardDefinition.creature(
        name = "Serpent Test Viper",
        manaCost = ManaCost.parse("{1}{B}"),
        subtypes = setOf(Subtype("Snake")),
        power = 1,
        toughness = 1,
        keywords = setOf(Keyword.DEATHTOUCH),
    )

    val Harmless = CardDefinition.creature(
        name = "Serpent Test Grunt",
        manaCost = ManaCost.parse("{1}{B}"),
        subtypes = setOf(Subtype("Warrior")),
        power = 1,
        toughness = 1,
    )

    val Zap = card("Serpent Test Zap") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            val victim = target("target creature", Targets.Creature)
            effect = Effects.DealDamage(3, victim)
        }
    }

    // Lethal to the 3/4 Society in one shot, so its death happens inside a single priority window
    // (two 3-damage Zaps would need a step boundary in between, and cleanup wipes the first three).
    val Blast = card("Serpent Test Blast") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            val victim = target("target creature", Targets.Creature)
            effect = Effects.DealDamage(5, victim)
        }
    }

    // Grants deathtouch by a continuous effect, so the dying creature's *granted* keyword is what
    // the trigger has to see in last-known information.
    val Venom = card("Serpent Test Venom") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            val recipient = target("target creature", Targets.Creature)
            effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, recipient)
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(Venomous, Harmless))
        registerCard(TheSerpentSociety)
        registerCard(Zap)
        registerCard(Blast)
        registerCard(Venom)
        initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
    }

    fun poison(driver: GameTestDriver, playerId: EntityId): Int =
        driver.state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    /** Resolve whatever is on the stack without advancing the turn. */
    fun GameTestDriver.settle() {
        repeat(6) { if (state.priorityPlayerId != null && pendingDecision == null) bothPass() }
    }

    test("Ward—Get five poison counters: paying puts five poison on the targeting player") {
        val driver = driver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val society = driver.putCreatureOnBattlefield(opponent, "The Serpent Society")

        driver.giveMana(activePlayer, Color.RED, 1)
        val zap = driver.putCardInHand(activePlayer, "Serpent Test Zap")
        driver.castSpellWithTargets(activePlayer, zap, listOf(ChosenTarget.Permanent(society)))
        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<YesNoDecision>()
        decision.playerId shouldBe activePlayer

        driver.submitYesNo(activePlayer, true)
        driver.settle()

        withClue("the counters go on the player who targeted, not on the Society's controller") {
            poison(driver, activePlayer) shouldBe 5
            poison(driver, opponent) shouldBe 0
        }
        withClue("3 damage isn't lethal to a 3/4, so the Society survives either way") {
            driver.findPermanent(opponent, "The Serpent Society") shouldNotBe null
        }
    }

    test("declining the ward counters the spell and places no poison") {
        val driver = driver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val society = driver.putCreatureOnBattlefield(opponent, "The Serpent Society")

        driver.giveMana(activePlayer, Color.RED, 1)
        val zap = driver.putCardInHand(activePlayer, "Serpent Test Zap")
        driver.castSpellWithTargets(activePlayer, zap, listOf(ChosenTarget.Permanent(society)))
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(activePlayer, false)
        driver.settle()

        poison(driver, activePlayer) shouldBe 0
        withClue("the countered Zap is in its owner's graveyard and the Society is untouched") {
            driver.getGraveyardCardNames(activePlayer) shouldContain "Serpent Test Zap"
            driver.findPermanent(opponent, "The Serpent Society") shouldNotBe null
        }
    }

    test("another deathtouch creature you control dying makes each opponent sacrifice") {
        val driver = driver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // The active player owns the Society and a deathtouch Viper; the opponent has one creature.
        driver.putCreatureOnBattlefield(activePlayer, "The Serpent Society")
        val viper = driver.putCreatureOnBattlefield(activePlayer, "Serpent Test Viper")
        driver.putCreatureOnBattlefield(opponent, "Serpent Test Grunt")

        driver.giveMana(activePlayer, Color.RED, 1)
        val zap = driver.putCardInHand(activePlayer, "Serpent Test Zap")
        driver.castSpellWithTargets(activePlayer, zap, listOf(ChosenTarget.Permanent(viper)))
        driver.settle()

        withClue("the opponent's only nontoken creature is sacrificed with no choice to make") {
            driver.findPermanent(opponent, "Serpent Test Grunt") shouldBe null
        }
        withClue("the edict hits opponents only") {
            driver.findPermanent(activePlayer, "The Serpent Society") shouldNotBe null
        }
    }

    test("a creature without deathtouch dying does not trigger the edict") {
        val driver = driver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "The Serpent Society")
        val grunt = driver.putCreatureOnBattlefield(activePlayer, "Serpent Test Grunt")
        driver.putCreatureOnBattlefield(opponent, "Serpent Test Grunt")

        driver.giveMana(activePlayer, Color.RED, 1)
        val zap = driver.putCardInHand(activePlayer, "Serpent Test Zap")
        driver.castSpellWithTargets(activePlayer, zap, listOf(ChosenTarget.Permanent(grunt)))
        driver.settle()

        driver.findPermanent(opponent, "Serpent Test Grunt") shouldNotBe null
    }

    test("deathtouch granted by a continuous effect still triggers the edict (last-known info)") {
        val driver = driver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "The Serpent Society")
        val grunt = driver.putCreatureOnBattlefield(activePlayer, "Serpent Test Grunt")
        driver.putCreatureOnBattlefield(opponent, "Serpent Test Grunt")

        driver.giveMana(activePlayer, Color.RED, 2)

        // The Grunt has no printed deathtouch; grant it before it dies.
        val venom = driver.putCardInHand(activePlayer, "Serpent Test Venom")
        driver.castSpellWithTargets(activePlayer, venom, listOf(ChosenTarget.Permanent(grunt)))
        driver.bothPass()

        val zap = driver.putCardInHand(activePlayer, "Serpent Test Zap")
        driver.castSpellWithTargets(activePlayer, zap, listOf(ChosenTarget.Permanent(grunt)))
        driver.settle()

        withClue("the keyword frozen on the ZoneChangeEvent is the granted one, so the edict fires") {
            driver.findPermanent(opponent, "Serpent Test Grunt") shouldBe null
        }
    }

    test("the Society's own death does not trigger it (OTHER binding)") {
        val driver = driver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val society = driver.putCreatureOnBattlefield(activePlayer, "The Serpent Society")
        driver.putCreatureOnBattlefield(opponent, "Serpent Test Grunt")

        // One Blast kills the 3/4 Society. It has deathtouch itself, so only the OTHER binding
        // keeps its own death from matching the trigger.
        driver.giveMana(activePlayer, Color.RED, 1)
        val blast = driver.putCardInHand(activePlayer, "Serpent Test Blast")
        driver.castSpellWithTargets(activePlayer, blast, listOf(ChosenTarget.Permanent(society)))
        driver.settle()

        driver.findPermanent(activePlayer, "The Serpent Society") shouldBe null
        withClue("its own death is excluded by the trigger's OTHER binding") {
            driver.findPermanent(opponent, "Serpent Test Grunt") shouldNotBe null
        }
    }
})
