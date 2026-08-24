package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for the exile batch trigger — `EventPattern.CardsPutIntoExileEvent`, detected by
 * `TriggerDetector.detectCardsPutIntoExileBatchTriggers`.
 *
 * Two observers are built from the same pattern so every case asserts both readings side by side:
 *
 *  - the **unscoped** one, Ketramose, the New Dawn's "whenever one or more **cards** are put into
 *    exile from graveyards and/or the battlefield" — anyone's cards, tokens excluded (CR 111.6:
 *    a token isn't a card);
 *  - the **scoped, token-inclusive** one, Kaya, Spirits' Justice's "whenever one or more
 *    **creatures you control** and/or **creature cards in your graveyard** are put into exile" —
 *    where the battlefield noun is *creatures*, so a token counts, and both arms are narrowed to
 *    one player.
 *
 * CR 603.2c: both fire at most once per batch, however many objects moved.
 *
 * The detector is driven directly with synthesized [ZoneChangeEvent]s, which is the shape
 * `ZoneTransitionService` emits — a battlefield exit carries the `lastKnown` snapshot with the
 * controller frozen at the moment it left (CR 603.10), and every exit carries `ownerId`.
 */
class CardsPutIntoExileBatchTriggerTest : FunSpec({

    // Ketramose's reading: any card, from any graveyard or anyone's battlefield.
    val anyCardObserver = card("Any Card Exile Observer") {
        manaCost = "{0}"
        typeLine = "Creature — Human Cleric"
        power = 0
        toughness = 1
        triggeredAbility {
            trigger = Triggers.CardsPutIntoExile()
            effect = Effects.DrawCards(1)
        }
    }

    // Kaya's reading: creatures *you control* and/or creature cards in *your* graveyard, tokens included.
    val yourCreaturesObserver = card("Your Creatures Exile Observer") {
        manaCost = "{0}"
        typeLine = "Creature — Human Cleric"
        power = 0
        toughness = 1
        triggeredAbility {
            trigger = Triggers.CardsPutIntoExile(
                fromZones = setOf(Zone.BATTLEFIELD, Zone.GRAVEYARD),
                filter = GameObjectFilter.Creature.youControl(),
                includeTokens = true,
            )
            effect = Effects.DrawCards(1)
        }
    }

    val bear = card("Exile Batch Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    val relic = card("Exile Batch Relic") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(anyCardObserver, yourCreaturesObserver, bear, relic)
        )
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        return driver
    }

    /** A battlefield → exile move, carrying the last-known controller the way a real exit does. */
    fun exiledFromBattlefield(entityId: EntityId, controllerId: EntityId, ownerId: EntityId) =
        ZoneChangeEvent(
            entityId = entityId,
            entityName = "",
            fromZone = Zone.BATTLEFIELD,
            toZone = Zone.EXILE,
            ownerId = ownerId,
            lastKnown = EntitySnapshot(entityId = entityId, controllerId = controllerId),
        )

    /** A graveyard → exile move. Graveyard cards have no controller — ownership is the whole scope. */
    fun exiledFromGraveyard(entityId: EntityId, ownerId: EntityId) =
        ZoneChangeEvent(
            entityId = entityId,
            entityName = "",
            fromZone = Zone.GRAVEYARD,
            toZone = Zone.EXILE,
            ownerId = ownerId,
        )

    fun exileTriggersOf(driver: GameTestDriver, events: List<ZoneChangeEvent>, sourceId: EntityId) =
        TriggerDetector(driver.cardRegistry)
            .detectTriggers(driver.state, events)
            .filter { it.ability.trigger is EventPattern.CardsPutIntoExileEvent && it.sourceId == sourceId }

    context("controller and owner scoping") {

        test("a creature you control leaving the battlefield fires the scoped trigger") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Your Creatures Exile Observer")
            val mine = driver.putCreatureOnBattlefield(driver.player1, "Exile Batch Bear")

            val triggers = exileTriggersOf(
                driver,
                listOf(exiledFromBattlefield(mine, driver.player1, driver.player1)),
                observer,
            )
            triggers shouldHaveSize 1
        }

        test("a creature an OPPONENT controls does not fire the scoped trigger, but does fire the unscoped one") {
            val driver = createDriver()
            val scoped = driver.putCreatureOnBattlefield(driver.player1, "Your Creatures Exile Observer")
            val unscoped = driver.putCreatureOnBattlefield(driver.player1, "Any Card Exile Observer")
            val theirs = driver.putCreatureOnBattlefield(driver.player2, "Exile Batch Bear")

            val events = listOf(exiledFromBattlefield(theirs, driver.player2, driver.player2))

            withClue("\"creatures you control\" is not satisfied by the opponent's creature") {
                exileTriggersOf(driver, events, scoped) shouldHaveSize 0
            }
            withClue("Ketramose's wording watches every battlefield") {
                exileTriggersOf(driver, events, unscoped) shouldHaveSize 1
            }
        }

        test("a creature card in YOUR graveyard fires; one in an opponent's graveyard does not") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Your Creatures Exile Observer")
            val mine = driver.putCardInGraveyard(driver.player1, "Exile Batch Bear")
            val theirs = driver.putCardInGraveyard(driver.player2, "Exile Batch Bear")

            withClue("ownership is what \"in your graveyard\" means") {
                exileTriggersOf(driver, listOf(exiledFromGraveyard(mine, driver.player1)), observer) shouldHaveSize 1
                exileTriggersOf(driver, listOf(exiledFromGraveyard(theirs, driver.player2)), observer) shouldHaveSize 0
            }
        }

        test("a noncreature card you own does not fire a creature-scoped trigger") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Your Creatures Exile Observer")
            val artifact = driver.putCardInGraveyard(driver.player1, "Exile Batch Relic")

            exileTriggersOf(driver, listOf(exiledFromGraveyard(artifact, driver.player1)), observer) shouldHaveSize 0
        }
    }

    context("tokens (CR 111.6 / 111.7)") {

        test("a token creature you control fires the token-inclusive trigger but never the card one") {
            val driver = createDriver()
            val scoped = driver.putCreatureOnBattlefield(driver.player1, "Your Creatures Exile Observer")
            val unscoped = driver.putCreatureOnBattlefield(driver.player1, "Any Card Exile Observer")
            val token = driver.putCreatureOnBattlefield(driver.player1, "Exile Batch Bear")
            driver.replaceState(driver.state.updateEntity(token) { it.with(TokenComponent) })

            val events = listOf(exiledFromBattlefield(token, driver.player1, driver.player1))

            withClue("\"creatures you control\" counts a token creature like any other creature") {
                exileTriggersOf(driver, events, scoped) shouldHaveSize 1
            }
            withClue("a token is not a card (CR 111.6), so the \"cards\" wording ignores it") {
                exileTriggersOf(driver, events, unscoped) shouldHaveSize 0
            }
        }
    }

    context("batching and capture (CR 603.2c)") {

        test("several matching objects in one batch fire the trigger once and are all captured") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Your Creatures Exile Observer")
            val onBoard = driver.putCreatureOnBattlefield(driver.player1, "Exile Batch Bear")
            val inYard = driver.putCardInGraveyard(driver.player1, "Exile Batch Bear")

            val triggers = exileTriggersOf(
                driver,
                listOf(
                    exiledFromBattlefield(onBoard, driver.player1, driver.player1),
                    exiledFromGraveyard(inYard, driver.player1),
                ),
                observer,
            )

            triggers shouldHaveSize 1
            withClue("\"from among them\" is the captured batch — both arms of the and/or") {
                triggers.single().triggerContext.capturedEntityIds
                    .shouldContainExactlyInAnyOrder(listOf(onBoard, inYard))
            }
        }

        test("the capture holds only the matching objects, not the whole batch") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Your Creatures Exile Observer")
            val mine = driver.putCreatureOnBattlefield(driver.player1, "Exile Batch Bear")
            val theirs = driver.putCreatureOnBattlefield(driver.player2, "Exile Batch Bear")
            val myRelic = driver.putCardInGraveyard(driver.player1, "Exile Batch Relic")

            val triggers = exileTriggersOf(
                driver,
                listOf(
                    exiledFromBattlefield(mine, driver.player1, driver.player1),
                    exiledFromBattlefield(theirs, driver.player2, driver.player2),
                    exiledFromGraveyard(myRelic, driver.player1),
                ),
                observer,
            )

            triggers shouldHaveSize 1
            withClue("only your creature is choosable from among them") {
                triggers.single().triggerContext.capturedEntityIds shouldBe listOf(mine)
            }
        }

        test("a move out of an unwatched zone does not fire it") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Your Creatures Exile Observer")
            val inHand = driver.putCardInHand(driver.player1, "Exile Batch Bear")

            val fromHand = ZoneChangeEvent(
                entityId = inHand,
                entityName = "",
                fromZone = Zone.HAND,
                toZone = Zone.EXILE,
                ownerId = driver.player1,
            )
            exileTriggersOf(driver, listOf(fromHand), observer) shouldHaveSize 0
        }
    }
})
