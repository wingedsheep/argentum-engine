package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
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
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Feature test for a granted *self-scoped* zone-change replacement:
 * `GrantReplacementEffect(RedirectZoneChange(EXILE, from = BATTLEFIELD, selfOnly = true), target,
 * EndOfTurn)` — "until end of turn, it gains 'If this creature would leave the battlefield, exile it
 * instead of putting it anywhere else'" (Elemental Expressionist).
 *
 * The replacement is an ability the target gained, so it redirects only that creature (not every
 * creature its controller has), whatever zone it was headed to; it ends with the turn; and it ends
 * when the creature becomes a new object (CR 400.7), so a creature blinked back the same turn dies
 * normally.
 */
class GrantedSelfRedirectReplacementTest : FunSpec({

    val exileWard = card("Test Exile Ward") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Until end of turn, target creature gains \"If this creature would leave the battlefield, exile it instead of putting it anywhere else.\""
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.GrantReplacementEffect(
                RedirectZoneChange(
                    newDestination = Zone.EXILE,
                    appliesTo = EventPattern.ZoneChangeEvent(from = Zone.BATTLEFIELD),
                    selfOnly = true,
                ),
                t,
                Duration.EndOfTurn,
            )
        }
    }

    val slay = card("Test Slay") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Destroy target creature."
        spell { effect = Effects.Destroy(target(TargetFilter.Creature)) }
    }

    val unsummon = card("Test Unsummon") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Return target creature to its owner's hand."
        spell { effect = Effects.ReturnToHand(target(TargetFilter.Creature)) }
    }

    val blink = card("Test Blink") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Exile target creature, then return it to the battlefield under its owner's control."
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Exile(t) then Effects.Move(t, Zone.BATTLEFIELD, fromZone = Zone.EXILE)
        }
    }

    data class Setup(val driver: GameTestDriver, val me: EntityId)

    fun setup(): Setup {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(exileWard, slay, unsummon, blink))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Setup(driver, me)
    }

    fun GameTestDriver.cast(player: EntityId, name: String, target: EntityId) {
        val spell = putCardInHand(player, name)
        giveMana(player, Color.BLUE, 1)
        castSpellWithTargets(player, spell, listOf(ChosenTarget.Permanent(target))).error shouldBe null
        bothPass()
    }

    test("the holder is exiled instead of dying; another creature is not") {
        val (driver, me) = setup()
        val warded = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val other = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.cast(me, "Test Exile Ward", warded)

        driver.cast(me, "Test Slay", warded)
        driver.getExile(me) shouldContain warded
        driver.getGraveyard(me) shouldNotContain warded

        driver.cast(me, "Test Slay", other)
        driver.getGraveyard(me) shouldContain other
    }

    test("it replaces any destination, not just the graveyard") {
        val (driver, me) = setup()
        val warded = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.cast(me, "Test Exile Ward", warded)

        driver.cast(me, "Test Unsummon", warded)
        driver.getExile(me) shouldContain warded
        driver.getHand(me) shouldNotContain warded
    }

    test("it ends with the turn") {
        val (driver, me) = setup()
        val warded = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.cast(me, "Test Exile Ward", warded)

        // The opponent's turn: they hold priority in their own upkeep.
        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.grantedReplacementEffects.shouldBeEmpty()
        driver.cast(driver.getOpponent(me), "Test Slay", warded)
        driver.getGraveyard(me) shouldContain warded
    }

    test("a creature that leaves and returns is a new object without the ability") {
        val (driver, me) = setup()
        val warded = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.cast(me, "Test Exile Ward", warded)

        driver.cast(me, "Test Blink", warded)
        driver.state.getBattlefield() shouldContain warded
        driver.state.grantedReplacementEffects.shouldBeEmpty()

        driver.cast(me, "Test Slay", warded)
        driver.getGraveyard(me) shouldContain warded
    }
})
