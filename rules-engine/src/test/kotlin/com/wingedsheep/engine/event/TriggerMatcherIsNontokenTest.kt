package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize

/**
 * Regression for the latent bug where [TriggerMatcher.matchesCardPredicate] had no branch
 * for [CardPredicate.IsNontoken] and fell through to `else -> true`, silently treating the
 * predicate as a no-op so triggers gated on "nontoken X" fired for tokens too.
 *
 * The defect was masked on death triggers by CR 704.5s state-based actions sweeping the
 * token off the battlefield before the matcher ran. Bounce/exile/return paths are not
 * masked — the token's ZoneChangeEvent reaches the matcher with the token entity still
 * referenceable (or with the LKI flag set) and the predicate must actually evaluate.
 *
 * These tests assert the matcher behaviour through a tiny synthetic observer card with
 * `from = BATTLEFIELD, to = HAND` + `IsCreature + IsNontoken`, which is the simplest
 * way to provoke the previously-broken path on main.
 */
class TriggerMatcherIsNontokenTest : FunSpec({

    // Observer: "Whenever a nontoken creature you control leaves the battlefield to a hand,
    // draw a card." This is a synthetic trigger shape that hits matchesCardPredicate for
    // IsNontoken on a non-death zone change.
    val nontokenBounceWatcher = card("Nontoken Bounce Watcher") {
        manaCost = "{2}"
        colorIdentity = ""
        typeLine = "Enchantment"
        oracleText = "Whenever a nontoken creature you control is returned to its owner's hand from the battlefield, draw a card."

        spell {}

        triggeredAbility {
            trigger = TriggerSpec(
                event = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter(
                        cardPredicates = listOf(
                            CardPredicate.IsCreature,
                            CardPredicate.IsNontoken,
                        ),
                        controllerPredicate = ControllerPredicate.ControlledByYou,
                    ),
                    from = Zone.BATTLEFIELD,
                    to = Zone.HAND,
                ),
                binding = TriggerBinding.OTHER,
            )
            effect = Effects.DrawCards(1)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + nontokenBounceWatcher)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        return driver
    }

    fun detectorFor(driver: GameTestDriver): TriggerDetector =
        TriggerDetector(driver.cardRegistry)

    fun GameTestDriver.createMouseTokenOnBattlefield(playerId: EntityId): EntityId {
        val tokenId = EntityId.generate()
        val tokenCard = CardComponent(
            cardDefinitionId = "token:Mouse",
            name = "Mouse Token",
            manaCost = ManaCost.ZERO,
            typeLine = TypeLine.parse("Creature - Mouse"),
            baseStats = CreatureStats(1, 1),
            colors = setOf(Color.WHITE),
            ownerId = playerId,
        )
        val container = ComponentContainer.of(
            tokenCard,
            TokenComponent,
            ControllerComponent(playerId),
            SummoningSicknessComponent,
        )
        replaceState(
            state
                .withEntity(tokenId, container)
                .addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), tokenId),
        )
        return tokenId
    }

    test("nontoken-gated bounce trigger fires for a real (nontoken) creature returning to hand") {
        // Sanity baseline: with the IsNontoken branch wired up, a nontoken creature returning
        // to hand still satisfies the filter and fires the trigger.
        val driver = createDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Nontoken Bounce Watcher")

        val bounced = driver.putCardInHand(driver.player1, "Grizzly Bears")
        val event = ZoneChangeEvent(
            entityId = bounced,
            entityName = "Grizzly Bears",
            fromZone = Zone.BATTLEFIELD,
            toZone = Zone.HAND,
            ownerId = driver.player1,
            lastKnownWasToken = false,
        )

        val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

        triggers.filter {
            it.ability.trigger is EventPattern.ZoneChangeEvent
        } shouldHaveSize 1
    }

    test("nontoken-gated bounce trigger does NOT fire when a token is returned to hand (LKI path)") {
        // Token leaves the battlefield; TokensInWrongZonesCheck will sweep it after the
        // zone change, but at trigger-detection time the matcher sees lastKnownWasToken=true.
        // On main this case incorrectly fired because matchesCardPredicate fell through
        // to `else -> true` for IsNontoken.
        val driver = createDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Nontoken Bounce Watcher")

        val token = driver.createMouseTokenOnBattlefield(driver.player1)
        val event = ZoneChangeEvent(
            entityId = token,
            entityName = "Mouse Token",
            fromZone = Zone.BATTLEFIELD,
            toZone = Zone.HAND,
            ownerId = driver.player1,
            lastKnownWasToken = true,
        )

        val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

        triggers.filter {
            it.ability.trigger is EventPattern.ZoneChangeEvent
        }.shouldBeEmpty()
    }

    test("Or(IsNontoken, ...) composite hitting the deep matcher rejects a token via base-state check") {
        // Exercises the matchesCardPredicate path directly (rather than the matchesZoneChangeTrigger
        // early-handled cases) by wrapping IsNontoken inside an Or. When the predicate is composite,
        // matchesZoneChangeTrigger routes it through matchesCardPredicate, which previously fell
        // through to `else -> true` for IsNontoken even inside Or. The base-state fallback in
        // matchesCardPredicate must read TokenComponent from the projected snapshot's base state.
        val orWatcher = card("Or-Nontoken Watcher") {
            manaCost = "{2}"
            colorIdentity = ""
            typeLine = "Enchantment"
            oracleText = "Whenever a creature you control that is (nontoken OR has flying) is returned to its owner's hand from the battlefield, draw a card."

            spell {}

            triggeredAbility {
                trigger = TriggerSpec(
                    event = EventPattern.ZoneChangeEvent(
                        filter = GameObjectFilter(
                            cardPredicates = listOf(
                                CardPredicate.IsCreature,
                                CardPredicate.Or(
                                    listOf(
                                        CardPredicate.IsNontoken,
                                        CardPredicate.HasKeyword(com.wingedsheep.sdk.core.Keyword.FLYING),
                                    ),
                                ),
                            ),
                            controllerPredicate = ControllerPredicate.ControlledByYou,
                        ),
                        from = Zone.BATTLEFIELD,
                        to = Zone.HAND,
                    ),
                    binding = TriggerBinding.OTHER,
                )
                effect = Effects.DrawCards(1)
            }
        }

        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + orWatcher)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        driver.putPermanentOnBattlefield(driver.player1, "Or-Nontoken Watcher")

        // The token has no flying, so the Or evaluates to (false OR false) — trigger must NOT fire.
        val token = driver.createMouseTokenOnBattlefield(driver.player1)
        val event = ZoneChangeEvent(
            entityId = token,
            entityName = "Mouse Token",
            fromZone = Zone.BATTLEFIELD,
            toZone = Zone.HAND,
            ownerId = driver.player1,
            lastKnownWasToken = true,
        )

        val triggers = detectorFor(driver).detectTriggers(driver.state, listOf(event))

        triggers.filter {
            it.ability.trigger is EventPattern.ZoneChangeEvent
        }.shouldBeEmpty()
    }
})
