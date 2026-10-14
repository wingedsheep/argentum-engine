package com.wingedsheep.engine.view

import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith

/**
 * A badge says when its effect ends. Without that, a creature pumped until end of turn looks the
 * same as one changed for good, and a trigger watching for the rest of the game reads as if it
 * lapsed tonight.
 */
class EffectDurationBadgeTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        return driver
    }

    fun transformer(d: GameTestDriver): ClientStateTransformer =
        ClientStateTransformer(cardRegistry = d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))

    fun floating(
        target: EntityId,
        controller: EntityId,
        modification: SerializableModification,
        duration: Duration,
        id: String = "test-effect",
        sourceId: EntityId? = null,
        sourceName: String = "Giant Growth",
        condition: Condition? = null,
    ) = ActiveFloatingEffect(
        id = EntityId(id),
        effect = FloatingEffectData(
            Layer.POWER_TOUGHNESS, Sublayer.MODIFICATIONS, modification, setOf(target), sourceCondition = condition,
        ),
        duration = duration,
        sourceId = sourceId,
        sourceName = sourceName,
        controllerId = controller,
        timestamp = 1,
    )

    fun GameTestDriver.badges(state: GameState, card: EntityId): List<ClientCardEffect> =
        transformer(this).transform(state, player2).cards.getValue(card).activeEffects

    test("a pump until end of turn is badged with its source and its end; a lasting one is not badged") {
        val driver = createDriver()
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val pump = SerializableModification.ModifyPowerToughness(3, 3)
        val untilEndOfTurn = driver.state.copy(floatingEffects = listOf(floating(bears, driver.player1, pump, Duration.EndOfTurn)))
        val lasting = driver.state.copy(floatingEffects = listOf(floating(bears, driver.player1, pump, Duration.Permanent)))

        val badge = driver.badges(untilEndOfTurn, bears).single()
        badge.name shouldBe "Giant Growth"
        badge.duration shouldBe "until end of turn"
        badge.description shouldBe "+3/+3 (until end of turn)"
        driver.badges(lasting, bears).shouldBeEmpty()
    }

    test("a restriction's own badge carries its effect's end") {
        val driver = createDriver()
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        fun cantBlock(duration: Duration) = driver.badges(
            driver.state.copy(floatingEffects = listOf(floating(bears, driver.player1, SerializableModification.SetCantBlock, duration))),
            bears,
        ).single()

        cantBlock(Duration.EndOfTurn).duration shouldBe "until end of turn"
        cantBlock(Duration.EndOfTurn).description shouldBe "This creature can't block (until end of turn)"
        cantBlock(Duration.Permanent).duration shouldBe null
        cantBlock(Duration.Permanent).description shouldBe "This creature can't block"
    }

    test("an event trigger's badge states its real expiry instead of always saying end of turn") {
        val driver = createDriver()
        val controller = driver.player1
        fun trigger(expiry: DelayedTriggerExpiry, fireOnce: Boolean = false, id: String = "watch") = DelayedTriggeredAbility(
            id = id, effect = Effects.DrawCards(1), sourceId = controller, sourceName = "Watcher",
            controllerId = controller, trigger = Triggers.self.attacks(), expiry = expiry, fireOnce = fireOnce,
        )
        fun badges(vararg triggers: DelayedTriggeredAbility): List<ClientPlayerEffect> =
            transformer(driver).transform(driver.state.copy(delayedTriggers = triggers.toList()), driver.player2)
                .players.single { it.playerId == controller }.activeEffects.filter { it.name.startsWith("Watcher") }
        fun badge(expiry: DelayedTriggerExpiry) = badges(trigger(expiry)).single()

        badge(DelayedTriggerExpiry.EndOfTurn).duration shouldBe "until end of turn"
        badge(DelayedTriggerExpiry.EndOfCombat).duration shouldBe "until end of combat"
        // Both seats read this badge, so "your" names the controller.
        badge(DelayedTriggerExpiry.UntilControllersNextTurn).duration shouldBe "until Player 1's next turn"
        badge(DelayedTriggerExpiry.Never).duration shouldBe null
        badge(DelayedTriggerExpiry.Never).description!! shouldNotContain "end of turn"
        badges(trigger(DelayedTriggerExpiry.Never, fireOnce = true)).single().description!! shouldStartWith "The next time"
        // One source, two lifetimes: two badges, not one that claims the first's.
        badges(trigger(DelayedTriggerExpiry.EndOfTurn), trigger(DelayedTriggerExpiry.Never, id = "watch-2"))
            .map { it.duration } shouldContainExactlyInAnyOrder listOf("until end of turn", null)
    }

    test("a cast watcher waiting on a named player's next turn names that player twice") {
        val driver = createDriver()
        val controller = driver.player1
        val watched = driver.player2
        fun watcher(expiresAfterTurn: Int) = DelayedTriggeredAbility(
            id = "rads", effect = Effects.DrawCards(1), sourceId = controller, sourceName = "Launcher",
            controllerId = controller, trigger = Triggers.anyPlayer.casts(), watchedEntityId = watched,
            expiry = DelayedTriggerExpiry.EndOfPlayersNextTurn(), expiryPlayerId = watched,
            expiresAfterTurn = expiresAfterTurn,
        )
        fun badge(state: GameState) = transformer(driver).transform(state, controller)
            .players.single { it.playerId == controller }.activeEffects.single { it.name.startsWith("Launcher") }

        val pending = badge(driver.state.copy(delayedTriggers = listOf(watcher(driver.state.turnNumber + 1))))
        pending.duration shouldBe "until the end of Player 2's next turn"
        pending.description!! shouldStartWith "Whenever Player 2 casts a spell"
        // On that player's last turn the badge says the watcher ends tonight.
        val lastTurn = driver.state.copy(activePlayerId = watched)
        badge(lastTurn.copy(delayedTriggers = listOf(watcher(lastTurn.turnNumber)))).duration shouldBe "until end of turn"
    }

    test("repeated pumps add up, and a conditional grant says when it applies") {
        val driver = createDriver()
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val pump = SerializableModification.ModifyPowerToughness(3, 3)
        val twice = listOf(
            floating(bears, driver.player1, pump, Duration.EndOfTurn, id = "growth-1"),
            floating(bears, driver.player1, pump, Duration.EndOfTurn, id = "growth-2"),
        )
        driver.badges(driver.state.copy(floatingEffects = twice), bears).single().description shouldBe "+6/+6 (until end of turn)"
        driver.badges(driver.state.copy(floatingEffects = twice.take(1)), bears).single().description shouldBe "+3/+3 (until end of turn)"

        val conditional = floating(bears, driver.player1, SerializableModification.GrantKeyword("FIRST_STRIKE"),
            Duration.EndOfTurn, sourceName = "Restless Spire", condition = Conditions.IsYourTurn)
        driver.badges(driver.state.copy(floatingEffects = listOf(conditional)), bears).single().description!! shouldContain
            "first strike as long as"
    }

    test("the same restriction with two different ends is one badge, with its own id, listing both ends") {
        val driver = createDriver()
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val both = listOf(
            floating(bears, driver.player1, SerializableModification.SetCantBlock, Duration.EndOfTurn, id = "a"),
            floating(bears, driver.player1, SerializableModification.SetCantBlock, Duration.UntilYourNextTurn, id = "b"),
        )
        val badges = driver.badges(driver.state.copy(floatingEffects = both), bears)
        badges.map { it.effectId }.distinct().size shouldBe badges.size
        badges.single().description shouldBe "This creature can't block (until end of turn; until Player 1's next turn)"
    }

    test("a next-turn effect says whether it ends this turn or on its controller's next") {
        val driver = createDriver()
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val pump = floating(bears, driver.player1, SerializableModification.ModifyPowerToughness(3, 3), Duration.EndOfYourNextTurn)
        // On Player 1's turn 3: an effect made on turn 1 (floor 2) ends tonight; one made now (floor 4) doesn't.
        val turnThree = driver.state.copy(turnNumber = 3, activePlayerId = driver.player1)
        fun ending(floor: Int) = driver.badges(turnThree.copy(floatingEffects = listOf(pump.copy(expiresAfterTurn = floor))), bears)
            .single().duration

        ending(2) shouldBe "until end of turn"
        ending(4) shouldBe "until the end of Player 1's next turn"
    }

    test("damage shields say when they end, per shield") {
        val driver = createDriver()
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        fun shield(amount: Int, duration: Duration, id: String) =
            floating(bears, driver.player1, SerializableModification.PreventNextDamage(amount), duration, id = id)
        fun badge(vararg shields: ActiveFloatingEffect) =
            driver.badges(driver.state.copy(floatingEffects = shields.toList()), bears).single { it.effectId == "prevent_damage" }

        badge(shield(3, Duration.EndOfTurn, "a")).duration shouldBe "until end of turn"
        badge(shield(3, Duration.Permanent, "a")).duration shouldBe null
        badge(shield(3, Duration.EndOfTurn, "a"), shield(2, Duration.Permanent, "b")).duration shouldBe
            "3 until end of turn; 2 indefinitely"
    }

    test("a player's damage shield says when it ends") {
        val driver = createDriver()
        val player = driver.player1
        val shield = floating(player, player, SerializableModification.PreventNextDamage(3), Duration.EndOfTurn)
        transformer(driver).transform(driver.state.copy(floatingEffects = listOf(shield)), driver.player2).players
            .single { it.playerId == player }.activeEffects.single { it.effectId == "prevent_damage" }
            .duration shouldBe "until end of turn"
    }

    test("a face-down source is named by its face-down name, not its face") {
        val driver = createDriver()
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val morph = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val state = driver.state.updateEntity(morph) { it.with(FaceDownComponent) }
        val pump = floating(bears, driver.player1, SerializableModification.ModifyPowerToughness(1, 1), Duration.EndOfTurn,
            sourceId = morph, sourceName = "Secret Morph")
        driver.badges(state.copy(floatingEffects = listOf(pump)), bears).single().name shouldNotBe "Secret Morph"

        val trigger = DelayedTriggeredAbility(
            id = "watch", effect = Effects.DrawCards(1), sourceId = morph, sourceName = "Secret Morph",
            controllerId = driver.player1, trigger = Triggers.self.attacks(), expiry = DelayedTriggerExpiry.EndOfTurn,
        )
        transformer(driver).transform(state.copy(delayedTriggers = listOf(trigger)), driver.player2).players
            .single { it.playerId == driver.player1 }.activeEffects.none { "Secret Morph" in it.name } shouldBe true
    }
})
