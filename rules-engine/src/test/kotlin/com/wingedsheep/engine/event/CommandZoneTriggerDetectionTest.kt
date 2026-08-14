package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Unit tests for the command-zone arm of trigger detection — abilities that declare
 * [Zone.COMMAND] in `activeZones` (CR 113.6b), which is what makes an *eminence* ability work
 * while its card sits in the command zone.
 *
 * Drives [TriggerDetector] directly with hand-crafted events (the sibling style of
 * [TriggerDetectorBatchTriggerTest]) so the assertions are about detection — zone gating, owner
 * as controller, no double-fire — rather than about effect resolution.
 * `EdgarMarkovScenarioTest` covers the shipped card end to end.
 */
class CommandZoneTriggerDetectionTest : FunSpec({

    // The eminence *event*-trigger shape: functions from the battlefield and the command zone.
    val eminenceCastWatcher = card("Eminence Cast Watcher") {
        manaCost = "{3}{B}"
        typeLine = "Legendary Creature — Vampire"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.YouCastSubtype(Subtype.VAMPIRE)
            triggerZones = setOf(Zone.BATTLEFIELD, Zone.COMMAND)
            effect = LoseLifeEffect(1, EffectTarget.PlayerRef(Player.You))
        }
    }

    // The same, but command-zone-only, to prove the two zones are independent gates.
    val commandOnlyCastWatcher = card("Command Only Cast Watcher") {
        manaCost = "{3}{B}"
        typeLine = "Legendary Creature — Vampire"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.YouCastSubtype(Subtype.VAMPIRE)
            triggerZones = setOf(Zone.COMMAND)
            effect = LoseLifeEffect(1, EffectTarget.PlayerRef(Player.You))
        }
    }

    // The default shape — battlefield only — which must stay inert in the command zone.
    val battlefieldOnlyCastWatcher = card("Battlefield Only Cast Watcher") {
        manaCost = "{3}{B}"
        typeLine = "Legendary Creature — Vampire"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.YouCastSubtype(Subtype.VAMPIRE)
            effect = LoseLifeEffect(1, EffectTarget.PlayerRef(Player.You))
        }
    }

    // The eminence *step*-trigger shape (Arahbo, Roar of the World's beginning-of-combat pump),
    // which travels the detectPhaseStepTriggers path instead.
    val eminenceStepWatcher = card("Eminence Step Watcher") {
        manaCost = "{3}{G}"
        typeLine = "Legendary Creature — Cat"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.YourUpkeep
            triggerZones = setOf(Zone.BATTLEFIELD, Zone.COMMAND)
            effect = LoseLifeEffect(1, EffectTarget.PlayerRef(Player.You))
        }
    }

    val vampireSpell = CardDefinition.creature(
        name = "Test Vampire",
        manaCost = ManaCost.parse("{B}"),
        subtypes = setOf(Subtype.VAMPIRE),
        power = 1,
        toughness = 1
    )

    fun createDriver(vararg extras: CardDefinition): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + extras.toList())
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Forest" to 20))
        return driver
    }

    fun detectorFor(driver: GameTestDriver): TriggerDetector = TriggerDetector(driver.cardRegistry)

    /** A cast event for a Vampire spell put on the stack by [caster]. */
    fun castVampire(driver: GameTestDriver, caster: EntityId): SpellCastEvent {
        val spellId = driver.putCardInHand(caster, "Test Vampire")
        return SpellCastEvent(spellEntityId = spellId, cardName = "Test Vampire", casterId = caster)
    }

    test("a cast trigger fires while its card is in the command zone") {
        val driver = createDriver(eminenceCastWatcher, vampireSpell)
        val watcher = driver.putCardInCommandZone(driver.player1, "Eminence Cast Watcher")

        val cast = castVampire(driver, driver.player1)
        val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(cast))

        triggers shouldHaveSize 1
        triggers[0].sourceId shouldBe watcher
        // A card in the command zone is owned, not controlled — the owner controls the ability.
        triggers[0].controllerId shouldBe driver.player1
    }

    test("the same ability still fires from the battlefield") {
        val driver = createDriver(eminenceCastWatcher, vampireSpell)
        val watcher = driver.putCreatureOnBattlefield(driver.player1, "Eminence Cast Watcher")

        val cast = castVampire(driver, driver.player1)
        val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(cast))

        triggers shouldHaveSize 1
        triggers[0].sourceId shouldBe watcher
    }

    test("it fires exactly once — the battlefield and command-zone passes don't both claim it") {
        val driver = createDriver(eminenceCastWatcher, vampireSpell)
        driver.putCardInCommandZone(driver.player1, "Eminence Cast Watcher")

        val cast = castVampire(driver, driver.player1)
        val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(cast))

        triggers shouldHaveSize 1
    }

    test("a command-zone-only ability does not fire from the battlefield") {
        val driver = createDriver(commandOnlyCastWatcher, vampireSpell)
        driver.putCreatureOnBattlefield(driver.player1, "Command Only Cast Watcher")

        val cast = castVampire(driver, driver.player1)
        detectorFor(driver).detectTriggers(driver.state, listOf(cast)).shouldBeEmpty()
    }

    test("a battlefield-only ability does not fire from the command zone") {
        val driver = createDriver(battlefieldOnlyCastWatcher, vampireSpell)
        driver.putCardInCommandZone(driver.player1, "Battlefield Only Cast Watcher")

        val cast = castVampire(driver, driver.player1)
        detectorFor(driver).detectTriggers(driver.state, listOf(cast)).shouldBeEmpty()

        // Not vacuous: the very same board fires once when the ability declares COMMAND.
        val control = createDriver(eminenceCastWatcher, vampireSpell)
        control.putCardInCommandZone(control.player1, "Eminence Cast Watcher")
        val controlCast = castVampire(control, control.player1)
        detectorFor(control).detectTriggers(control.state, listOf(controlCast)) shouldHaveSize 1
    }

    test("casting the source itself does not trigger it — that is what \"another\" costs us nothing") {
        // Edgar's ability reads "another Vampire spell" and needs no filter for it: to be the
        // triggering spell, the card has to be on the stack, and the stack is neither of the two
        // zones the ability functions from. Model exactly that — the card has left the command zone
        // for the stack, which is the state when its own SpellCastEvent fires.
        val driver = createDriver(eminenceCastWatcher)
        val watcher = driver.putCardInCommandZone(driver.player1, "Eminence Cast Watcher")
        driver.replaceState(
            driver.state
                .removeFromZone(
                    com.wingedsheep.engine.state.ZoneKey(driver.player1, Zone.COMMAND),
                    watcher
                )
                .addToZone(
                    com.wingedsheep.engine.state.ZoneKey(driver.player1, Zone.STACK),
                    watcher
                )
        )

        val ownCast = SpellCastEvent(
            spellEntityId = watcher,
            cardName = "Eminence Cast Watcher",
            casterId = driver.player1
        )

        detectorFor(driver).detectTriggers(driver.state, listOf(ownCast)).shouldBeEmpty()
    }

    test("a command-zone ability ignores an opponent's cast when the trigger is scoped to you") {
        val driver = createDriver(eminenceCastWatcher, vampireSpell)
        driver.putCardInCommandZone(driver.player1, "Eminence Cast Watcher")

        val cast = castVampire(driver, driver.player2)
        detectorFor(driver).detectTriggers(driver.state, listOf(cast)).shouldBeEmpty()
    }

    test("a step trigger fires while its card is in the command zone") {
        val driver = createDriver(eminenceStepWatcher)
        val watcher = driver.putCardInCommandZone(driver.player1, "Eminence Step Watcher")

        val triggers = detectorFor(driver)
            .detectPhaseStepTriggers(driver.state, Step.UPKEEP, driver.player1)

        triggers.filter { it.sourceId == watcher } shouldHaveSize 1
    }

    test("a command-zone step trigger is scoped to its owner's turn") {
        // "At the beginning of your upkeep" resolves "your" against the *state's* active player, so
        // the owner is what decides it: player 1 is active, and player 2's command-zone copy stays
        // quiet. (Only the owner's copy fired in the test above, on the same active player.)
        val driver = createDriver(eminenceStepWatcher)
        val theirs = driver.putCardInCommandZone(driver.player2, "Eminence Step Watcher")
        driver.state.activePlayerId shouldBe driver.player1

        val triggers = detectorFor(driver)
            .detectPhaseStepTriggers(driver.state, Step.UPKEEP, driver.player1)

        triggers.filter { it.sourceId == theirs }.shouldBeEmpty()
    }
})
