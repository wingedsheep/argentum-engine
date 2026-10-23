package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * `MarkExileOnDeath`'s duration axis: the default "this turn" mark lapses at cleanup, a
 * [Duration.Permanent] mark ("it gains 'If this creature would die, exile it instead'") outlives the
 * turn but ends when the creature leaves the battlefield (CR 400.7), and either mark replaces every
 * way of dying — destruction, lethal damage, and sacrifice alike. The permanent mark is a *granted
 * ability*, so a later lose-all-abilities effect strips it (layer 6, timestamp order) while an
 * earlier one doesn't; the "this turn" mark is the spell's own replacement effect and survives both.
 */
class ExileOnDeathMarkDurationTest : FunSpec({

    val markForever = card("Test Lasting Exile Mark") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.MarkExileOnDeath(t, Duration.Permanent)
        }
    }
    val markThisTurn = card("Test Turn Exile Mark") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.MarkExileOnDeath(t)
        }
    }
    val sacrificeIt = card("Test Make Sacrifice") {
        manaCost = "{B}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.SacrificeTarget(t, sacrificedByItsController = true)
        }
    }
    val bounceIt = card("Test Bounce") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Move(t, Zone.HAND)
        }
    }

    val loseAbilities = card("Test Lose All Abilities") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.RemoveAllAbilities(t)
        }
    }

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(markForever, markThisTurn, sacrificeIt, bounceIt, loseAbilities))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.cast(player: EntityId, name: String, color: Color, target: EntityId, extra: Int = 0) {
        val spell = putCardInHand(player, name)
        giveMana(player, color, 1)
        if (extra > 0) giveColorlessMana(player, extra)
        castSpell(player, spell, listOf(target)).error shouldBe null
        bothPass()
    }

    /** Pass to the next turn's precombat main, so the mark has been through a cleanup step. */
    fun GameTestDriver.toNextTurnMain() {
        passPriorityUntil(Step.END)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("a permanent mark outlives the turn: destroying the creature next turn exiles it") {
        val driver = setup()
        val active = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        driver.cast(active, "Test Lasting Exile Mark", Color.WHITE, bears)

        driver.toNextTurnMain()
        val caster = driver.activePlayer!!
        driver.cast(caster, "Doom Blade", Color.BLACK, bears, extra = 1)

        driver.getExile(active) shouldContain bears
        driver.getGraveyard(active) shouldNotContain bears
    }

    test("a permanent mark exiles a creature that dies to lethal damage") {
        val driver = setup()
        val active = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        driver.cast(active, "Test Lasting Exile Mark", Color.WHITE, bears)
        driver.cast(active, "Lightning Bolt", Color.RED, bears)

        driver.getExile(active) shouldContain bears
    }

    test("a sacrificed creature with the mark is exiled instead") {
        val driver = setup()
        val active = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        driver.cast(active, "Test Lasting Exile Mark", Color.WHITE, bears)
        driver.cast(active, "Test Make Sacrifice", Color.BLACK, bears)

        driver.getExile(active) shouldContain bears
        driver.getGraveyard(active) shouldNotContain bears
    }

    test("the this-turn mark also covers sacrifice") {
        val driver = setup()
        val active = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        driver.cast(active, "Test Turn Exile Mark", Color.WHITE, bears)
        driver.cast(active, "Test Make Sacrifice", Color.BLACK, bears)

        driver.getExile(active) shouldContain bears
    }

    test("the this-turn mark lapses at cleanup: next turn the creature dies normally") {
        val driver = setup()
        val active = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        driver.cast(active, "Test Turn Exile Mark", Color.WHITE, bears)

        driver.toNextTurnMain()
        driver.cast(driver.activePlayer!!, "Doom Blade", Color.BLACK, bears, extra = 1)

        driver.getGraveyard(active) shouldContain bears
    }

    test("a permanent mark ends when the creature leaves the battlefield and comes back") {
        val driver = setup()
        val active = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        driver.cast(active, "Test Lasting Exile Mark", Color.WHITE, bears)
        driver.cast(active, "Test Bounce", Color.BLUE, bears)
        driver.getHand(active) shouldContain bears

        driver.giveMana(active, Color.GREEN, 2)
        driver.castSpell(active, bears).error shouldBe null
        driver.bothPass()
        driver.findPermanent(active, "Grizzly Bears") shouldBe bears

        driver.cast(active, "Lightning Bolt", Color.RED, bears)
        driver.getGraveyard(active) shouldContain bears
    }

    test("losing all abilities after the grant removes the permanent mark") {
        val driver = setup()
        val active = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        driver.cast(active, "Test Lasting Exile Mark", Color.WHITE, bears)
        driver.cast(active, "Test Lose All Abilities", Color.BLUE, bears)
        driver.cast(active, "Lightning Bolt", Color.RED, bears)

        driver.getGraveyard(active) shouldContain bears
    }

    test("a lose-all-abilities effect older than the grant doesn't remove the permanent mark") {
        val driver = setup()
        val active = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        driver.cast(active, "Test Lose All Abilities", Color.BLUE, bears)
        driver.cast(active, "Test Lasting Exile Mark", Color.WHITE, bears)
        driver.cast(active, "Test Make Sacrifice", Color.BLACK, bears)

        driver.getExile(active) shouldContain bears
    }

    test("the this-turn mark is not an ability, so losing all abilities leaves it in place") {
        val driver = setup()
        val active = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        driver.cast(active, "Test Turn Exile Mark", Color.WHITE, bears)
        driver.cast(active, "Test Lose All Abilities", Color.BLUE, bears)
        driver.cast(active, "Lightning Bolt", Color.RED, bears)

        driver.getExile(active) shouldContain bears
    }
})
