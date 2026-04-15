package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.ControlChangedEvent
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.BuildersTalent
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.CamelliaTheSeedmiser
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.DourPortMage
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.KastralTheWindcrested
import com.wingedsheep.mtg.sets.definitions.khans.cards.SidisiBroodTyrant
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unit tests for batch trigger detection paths in [TriggerDetector].
 *
 * These exercise the detect*BatchTriggers methods directly by placing the observer
 * card on the battlefield and feeding hand-crafted events into detectTriggers(),
 * rather than driving full game scenarios that cause the events organically. This
 * keeps tests focused on the detection logic (filtering, batching, excludeSelf,
 * controller scoping) without coupling to effect resolution machinery.
 */
class TriggerDetectorBatchTriggerTest : FunSpec({

    // --- Helper inline test cards -------------------------------------------------

    val foodToken = CardDefinition.artifact(
        name = "Food Token",
        manaCost = ManaCost.ZERO,
        subtypes = setOf(Subtype.FOOD)
    )

    val plainArtifact = CardDefinition.artifact(
        name = "Chrome Mox",
        manaCost = ManaCost.ZERO
    )

    val birdCreature = CardDefinition.creature(
        name = "Test Sparrow",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype.BIRD),
        power = 1,
        toughness = 1
    )

    val nonBirdCreature = CardDefinition.creature(
        name = "Test Squirrel",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype.SQUIRREL),
        power = 1,
        toughness = 1
    )

    fun createDriver(vararg extras: CardDefinition): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + extras.toList())
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20))
        return driver
    }

    fun detectorFor(driver: GameTestDriver): TriggerDetector =
        TriggerDetector(driver.cardRegistry)

    // --- detectLibraryToGraveyardBatchTriggers ----------------------------------

    context("library-to-graveyard batch (Sidisi, Brood Tyrant)") {

        test("fires once when a creature card goes from library to graveyard") {
            val driver = createDriver(SidisiBroodTyrant)
            driver.putCreatureOnBattlefield(driver.player1, "Sidisi, Brood Tyrant")

            val milled = driver.putCardInGraveyard(driver.player1, "Grizzly Bears")
            val event = ZoneChangeEvent(
                entityId = milled,
                entityName = "Grizzly Bears",
                fromZone = Zone.LIBRARY,
                toZone = Zone.GRAVEYARD,
                ownerId = driver.player1
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            triggers shouldHaveSize 1
            triggers[0].ability.trigger.shouldBeInstanceOf<GameEvent.CardsPutIntoGraveyardFromLibraryEvent>()
            triggers[0].controllerId shouldBe driver.player1
        }

        test("batches: multiple creature mills produce exactly one trigger") {
            val driver = createDriver(SidisiBroodTyrant)
            driver.putCreatureOnBattlefield(driver.player1, "Sidisi, Brood Tyrant")

            val events = (1..3).map {
                val id = driver.putCardInGraveyard(driver.player1, "Grizzly Bears")
                ZoneChangeEvent(id, "Grizzly Bears", Zone.LIBRARY, Zone.GRAVEYARD, driver.player1)
            }

            val triggers = detectorFor(driver).detectTriggers(driver.state, events)

            // Sidisi has three triggered abilities; only the library-to-graveyard one
            // should fire here, and it must fire at most once per batch.
            val batchTriggers = triggers.filter {
                it.ability.trigger is GameEvent.CardsPutIntoGraveyardFromLibraryEvent
            }
            batchTriggers shouldHaveSize 1
        }

        test("filter rejects noncreature cards entering graveyard from library") {
            val driver = createDriver(SidisiBroodTyrant)
            driver.putCreatureOnBattlefield(driver.player1, "Sidisi, Brood Tyrant")

            // Lightning Bolt is an instant, not a creature
            val milled = driver.putCardInGraveyard(driver.player1, "Lightning Bolt")
            val event = ZoneChangeEvent(
                milled, "Lightning Bolt", Zone.LIBRARY, Zone.GRAVEYARD, driver.player1
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            triggers.filter {
                it.ability.trigger is GameEvent.CardsPutIntoGraveyardFromLibraryEvent
            }.shouldBeEmpty()
        }

        test("does not fire for mills into an opponent's graveyard") {
            val driver = createDriver(SidisiBroodTyrant)
            driver.putCreatureOnBattlefield(driver.player1, "Sidisi, Brood Tyrant")

            // Owner of the zone change is the opponent
            val milled = driver.putCardInGraveyard(driver.player2, "Grizzly Bears")
            val event = ZoneChangeEvent(
                milled, "Grizzly Bears", Zone.LIBRARY, Zone.GRAVEYARD, driver.player2
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            triggers.filter {
                it.ability.trigger is GameEvent.CardsPutIntoGraveyardFromLibraryEvent
            }.shouldBeEmpty()
        }
    }

    // --- detectSacrificeBatchTriggers -------------------------------------------

    context("sacrifice batch (Camellia, the Seedmiser)") {

        test("fires once when a Food is sacrificed") {
            val driver = createDriver(CamelliaTheSeedmiser, foodToken)
            driver.putCreatureOnBattlefield(driver.player1, "Camellia, the Seedmiser")

            // The sacrificed entity needs to be looked up via state.getEntity —
            // after a sacrifice it lives in the graveyard.
            val foodId = driver.putCardInGraveyard(driver.player1, "Food Token")
            val event = PermanentsSacrificedEvent(
                playerId = driver.player1,
                permanentIds = listOf(foodId),
                permanentNames = listOf("Food Token")
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            triggers shouldHaveSize 1
            triggers[0].ability.trigger.shouldBeInstanceOf<GameEvent.PermanentsSacrificedEvent>()
            triggers[0].controllerId shouldBe driver.player1
        }

        test("batches multiple sacrifices from the same controller into a single trigger") {
            val driver = createDriver(CamelliaTheSeedmiser, foodToken)
            driver.putCreatureOnBattlefield(driver.player1, "Camellia, the Seedmiser")

            val foodIds = (1..3).map { driver.putCardInGraveyard(driver.player1, "Food Token") }
            val events = listOf(
                PermanentsSacrificedEvent(driver.player1, foodIds.take(1), listOf("Food Token")),
                PermanentsSacrificedEvent(driver.player1, foodIds.drop(1), listOf("Food Token", "Food Token"))
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, events)

            triggers.filter {
                it.ability.trigger is GameEvent.PermanentsSacrificedEvent
            } shouldHaveSize 1
        }

        test("filter rejects non-Food sacrifices") {
            val driver = createDriver(CamelliaTheSeedmiser, plainArtifact)
            driver.putCreatureOnBattlefield(driver.player1, "Camellia, the Seedmiser")

            val artifactId = driver.putCardInGraveyard(driver.player1, "Chrome Mox")
            val event = PermanentsSacrificedEvent(
                playerId = driver.player1,
                permanentIds = listOf(artifactId),
                permanentNames = listOf("Chrome Mox")
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            triggers.filter {
                it.ability.trigger is GameEvent.PermanentsSacrificedEvent
            }.shouldBeEmpty()
        }

        test("opponent's sacrifices do not fire Camellia's trigger") {
            val driver = createDriver(CamelliaTheSeedmiser, foodToken)
            driver.putCreatureOnBattlefield(driver.player1, "Camellia, the Seedmiser")

            val foodId = driver.putCardInGraveyard(driver.player2, "Food Token")
            val event = PermanentsSacrificedEvent(
                playerId = driver.player2,
                permanentIds = listOf(foodId),
                permanentNames = listOf("Food Token")
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            triggers.filter {
                it.ability.trigger is GameEvent.PermanentsSacrificedEvent
            }.shouldBeEmpty()
        }
    }

    // --- detectLeaveBattlefieldWithoutDyingBatchTriggers ------------------------

    context("leave-without-dying batch (Dour Port-Mage)") {

        test("fires when another creature you control leaves the battlefield to hand") {
            val driver = createDriver(DourPortMage)
            driver.putCreatureOnBattlefield(driver.player1, "Dour Port-Mage")

            // A bounced creature ends up in hand, so place it there first, then emit
            // the zone-change event reflecting that transition.
            val bounced = driver.putCardInHand(driver.player1, "Grizzly Bears")
            val event = ZoneChangeEvent(
                entityId = bounced,
                entityName = "Grizzly Bears",
                fromZone = Zone.BATTLEFIELD,
                toZone = Zone.HAND,
                ownerId = driver.player1
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            triggers shouldHaveSize 1
            triggers[0].ability.trigger.shouldBeInstanceOf<GameEvent.LeaveBattlefieldWithoutDyingEvent>()
        }

        test("excludeSelf: only Dour Port-Mage itself leaving does not trigger") {
            val driver = createDriver(DourPortMage)
            val mage = driver.putCreatureOnBattlefield(driver.player1, "Dour Port-Mage")

            val event = ZoneChangeEvent(
                entityId = mage,
                entityName = "Dour Port-Mage",
                fromZone = Zone.BATTLEFIELD,
                toZone = Zone.HAND,
                ownerId = driver.player1
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            triggers.filter {
                it.ability.trigger is GameEvent.LeaveBattlefieldWithoutDyingEvent
            }.shouldBeEmpty()
        }

        test("creature going to graveyard (dying) does not fire the leave-without-dying trigger") {
            val driver = createDriver(DourPortMage)
            driver.putCreatureOnBattlefield(driver.player1, "Dour Port-Mage")

            val died = driver.putCardInGraveyard(driver.player1, "Grizzly Bears")
            val event = ZoneChangeEvent(
                entityId = died,
                entityName = "Grizzly Bears",
                fromZone = Zone.BATTLEFIELD,
                toZone = Zone.GRAVEYARD,
                ownerId = driver.player1
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            triggers.filter {
                it.ability.trigger is GameEvent.LeaveBattlefieldWithoutDyingEvent
            }.shouldBeEmpty()
        }

        test("non-creature leaving does not trigger (filter = Creature)") {
            val driver = createDriver(DourPortMage, plainArtifact)
            driver.putCreatureOnBattlefield(driver.player1, "Dour Port-Mage")

            val leftArtifact = driver.putCardInHand(driver.player1, "Chrome Mox")
            val event = ZoneChangeEvent(
                entityId = leftArtifact,
                entityName = "Chrome Mox",
                fromZone = Zone.BATTLEFIELD,
                toZone = Zone.HAND,
                ownerId = driver.player1
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            triggers.filter {
                it.ability.trigger is GameEvent.LeaveBattlefieldWithoutDyingEvent
            }.shouldBeEmpty()
        }
    }

    // --- detectCombatDamageBatchTriggers ----------------------------------------

    context("combat-damage batch (Kastral, the Windcrested)") {

        test("fires once when a Bird you control deals combat damage to a player") {
            val driver = createDriver(KastralTheWindcrested, birdCreature)
            driver.putCreatureOnBattlefield(driver.player1, "Kastral, the Windcrested")
            val bird = driver.putCreatureOnBattlefield(driver.player1, "Test Sparrow")

            val event = DamageDealtEvent(
                sourceId = bird,
                targetId = driver.player2,
                amount = 1,
                isCombatDamage = true,
                targetIsPlayer = true
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            triggers shouldHaveSize 1
            triggers[0].ability.trigger
                .shouldBeInstanceOf<GameEvent.OneOrMoreDealCombatDamageToPlayerEvent>()
            triggers[0].controllerId shouldBe driver.player1
        }

        test("batches multiple Birds dealing damage into a single trigger") {
            val driver = createDriver(KastralTheWindcrested, birdCreature)
            driver.putCreatureOnBattlefield(driver.player1, "Kastral, the Windcrested")
            val birdA = driver.putCreatureOnBattlefield(driver.player1, "Test Sparrow")
            val birdB = driver.putCreatureOnBattlefield(driver.player1, "Test Sparrow")

            val events = listOf(
                DamageDealtEvent(birdA, driver.player2, 1, isCombatDamage = true, targetIsPlayer = true),
                DamageDealtEvent(birdB, driver.player2, 1, isCombatDamage = true, targetIsPlayer = true)
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, events)

            triggers.filter {
                it.ability.trigger is GameEvent.OneOrMoreDealCombatDamageToPlayerEvent
            } shouldHaveSize 1
        }

        test("non-Bird creature dealing combat damage does not match sourceFilter") {
            val driver = createDriver(KastralTheWindcrested, nonBirdCreature)
            driver.putCreatureOnBattlefield(driver.player1, "Kastral, the Windcrested")
            val squirrel = driver.putCreatureOnBattlefield(driver.player1, "Test Squirrel")

            val event = DamageDealtEvent(
                sourceId = squirrel,
                targetId = driver.player2,
                amount = 1,
                isCombatDamage = true,
                targetIsPlayer = true
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            triggers.filter {
                it.ability.trigger is GameEvent.OneOrMoreDealCombatDamageToPlayerEvent
            }.shouldBeEmpty()
        }

        test("non-combat damage to a player does not trigger the batch ability") {
            val driver = createDriver(KastralTheWindcrested, birdCreature)
            driver.putCreatureOnBattlefield(driver.player1, "Kastral, the Windcrested")
            val bird = driver.putCreatureOnBattlefield(driver.player1, "Test Sparrow")

            val event = DamageDealtEvent(
                sourceId = bird,
                targetId = driver.player2,
                amount = 1,
                isCombatDamage = false,
                targetIsPlayer = true
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            triggers.filter {
                it.ability.trigger is GameEvent.OneOrMoreDealCombatDamageToPlayerEvent
            }.shouldBeEmpty()
        }

        test("combat damage to a creature (not a player) does not fire the batch") {
            val driver = createDriver(KastralTheWindcrested, birdCreature)
            driver.putCreatureOnBattlefield(driver.player1, "Kastral, the Windcrested")
            val bird = driver.putCreatureOnBattlefield(driver.player1, "Test Sparrow")
            val victim = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

            val event = DamageDealtEvent(
                sourceId = bird,
                targetId = victim,
                amount = 1,
                isCombatDamage = true,
                targetIsPlayer = false
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            triggers.filter {
                it.ability.trigger is GameEvent.OneOrMoreDealCombatDamageToPlayerEvent
            }.shouldBeEmpty()
        }
    }

    // --- detectPermanentsEnteredBatchTriggers (+ class-level gating) -----------

    context("permanents-entered batch (Builder's Talent at level 2)") {

        test("fires once when a noncreature nonland permanent enters under your control") {
            val driver = createDriver(BuildersTalent, plainArtifact)
            val talent = driver.putPermanentOnBattlefield(driver.player1, "Builder's Talent")
            // Advance the Class to level 2 to unlock the batch-entered trigger
            driver.replaceState(
                driver.state.updateEntity(talent) { it.with(ClassLevelComponent(currentLevel = 2)) }
            )

            val entered = driver.putPermanentOnBattlefield(driver.player1, "Chrome Mox")
            val event = ZoneChangeEvent(
                entityId = entered,
                entityName = "Chrome Mox",
                fromZone = Zone.HAND,
                toZone = Zone.BATTLEFIELD,
                ownerId = driver.player1
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            val batch = triggers.filter {
                it.ability.trigger is GameEvent.PermanentsEnteredEvent
            }
            batch shouldHaveSize 1
            batch[0].controllerId shouldBe driver.player1
        }

        test("creature entering does not match the Noncreature filter") {
            val driver = createDriver(BuildersTalent)
            val talent = driver.putPermanentOnBattlefield(driver.player1, "Builder's Talent")
            driver.replaceState(
                driver.state.updateEntity(talent) { it.with(ClassLevelComponent(currentLevel = 2)) }
            )

            val entered = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
            val event = ZoneChangeEvent(
                entityId = entered,
                entityName = "Grizzly Bears",
                fromZone = Zone.HAND,
                toZone = Zone.BATTLEFIELD,
                ownerId = driver.player1
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            triggers.filter {
                it.ability.trigger is GameEvent.PermanentsEnteredEvent
            }.shouldBeEmpty()
        }

        test("level-1 Builder's Talent has no permanents-entered trigger yet") {
            val driver = createDriver(BuildersTalent, plainArtifact)
            driver.putPermanentOnBattlefield(driver.player1, "Builder's Talent")
            // No ClassLevelComponent: effectiveTriggeredAbilities(null) excludes level-2 abilities

            val entered = driver.putPermanentOnBattlefield(driver.player1, "Chrome Mox")
            val event = ZoneChangeEvent(
                entityId = entered,
                entityName = "Chrome Mox",
                fromZone = Zone.HAND,
                toZone = Zone.BATTLEFIELD,
                ownerId = driver.player1
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            triggers.filter {
                it.ability.trigger is GameEvent.PermanentsEnteredEvent
            }.shouldBeEmpty()
        }
    }

    // --- detectControlChangeTriggers sanity -------------------------------------

    context("control-change trigger sanity") {
        test("no control change when old == new controller") {
            val driver = createDriver()
            val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")

            val event = ControlChangedEvent(
                permanentId = bears,
                permanentName = "Grizzly Bears",
                oldControllerId = driver.player1,
                newControllerId = driver.player1
            )

            val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

            // Grizzly Bears has no GainControlOfSelf trigger either way; this just
            // exercises the early-exit branch in detectControlChangeTriggers.
            triggers.shouldBeEmpty()
        }
    }
})
