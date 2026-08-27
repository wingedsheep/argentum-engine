package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.replacement.PendingGameEvent
import com.wingedsheep.engine.replacement.ReplacementEffectProcessor
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyDrawAmount
import com.wingedsheep.sdk.scripting.PreventDraw
import com.wingedsheep.sdk.scripting.PreventLifeGain
import com.wingedsheep.sdk.scripting.ModifyLifeGain
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `Player.EachOpponent` on a replacement effect's [EventPattern] must mean "a player not on my
 * team" — CR 102.3: "In a multiplayer game between teams, a player's teammates are the other
 * players on their team, and the player's opponents are all players not on their team" — not
 * "anyone who isn't the source's controller".
 *
 * [GameState.getOpponents] is the single team-aware read surface for this — it excludes the
 * player's whole team, and degrades to "everyone but me" in a non-team game, so routing
 * through it costs nothing in Standard. `PendingGameEvent`'s own `matchesPlayerFilter`
 * helpers instead compare `affectedPlayerId != sourceControllerId` directly, which makes a
 * *teammate's* draw match `EachOpponent` in Two-Headed Giant.
 *
 * Asserted at the [ReplacementEffectProcessor.gatherReplacements] level, because there is no
 * shipped card using `DrawEvent(EachOpponent)` yet — the defect is in the matcher, and each
 * `PendingGameEvent` subtype carries its own copy of the helper, so both are pinned here.
 */
class ReplacementTeamAwarenessTest : FunSpec({

    fun registry(): CardRegistry = CardRegistry().apply { register(TestCards.all) }

    /** Four seats in two teams of two, per CR 810.1: [[p1, p2], [p3, p4]]. */
    fun boot2hg() = GameInitializer(registry()).initializeGame(
        GameConfig(
            format = Format.TwoHeadedGiant(),
            players = (1..4).map { PlayerConfig("Player $it", Deck.of("Forest" to 40)) },
            teams = listOf(listOf(0, 1), listOf(2, 3)),
            startingPlayerIndex = 0,
            skipMulligans = true,
        )
    )

    fun GameState.withReplacementPermanent(
        controllerId: EntityId,
        name: String,
        effect: ReplacementEffect
    ): GameState {
        val permanentId = EntityId.generate()
        val container = ComponentContainer.of(
            CardComponent(
                cardDefinitionId = name,
                name = name,
                manaCost = ManaCost.ZERO,
                typeLine = TypeLine.parse("Enchantment"),
                oracleText = effect.description,
                colors = emptySet(),
                ownerId = controllerId,
            ),
            OwnerComponent(controllerId),
            ControllerComponent(controllerId),
            ReplacementEffectSourceComponent(listOf(effect))
        )
        return withEntity(permanentId, container)
            .addToZone(ZoneKey(controllerId, Zone.BATTLEFIELD), permanentId)
    }

    test("DrawEvent(EachOpponent) matches only opposing-team draws, not a teammate's") {
        val booted = boot2hg()
        val players = booted.playerIds
        val (source, teammate) = players[0] to players[1]
        val opponent = players[2]

        // PreventDraw rather than ModifyDrawAmount: the per-card DrawEvent is the pattern under
        // test here, and ModifyDrawAmount is announcement-only (its appliesTo is typed as
        // DrawCardsEvent). The player filter is shared, so either carrier exercises it.
        val state = booted.state.withReplacementPermanent(
            source, "Opposing Draw Preventer",
            PreventDraw(appliesTo = EventPattern.DrawEvent(player = Player.EachOpponent))
        )
        val processor = ReplacementEffectProcessor()

        withClue("Sanity: the pattern does fire for a player on the opposing team") {
            processor.gatherReplacements(state, PendingGameEvent.DrawPending(opponent, 1)).size shouldBe 1
        }
        withClue("The source's controller is not their own opponent") {
            processor.gatherReplacements(state, PendingGameEvent.DrawPending(source, 1)).size shouldBe 0
        }
        withClue(
            "CR 102.3 — a teammate is on the SAME team, so they are not an opponent. Matching " +
                "here means the filter used `affectedPlayerId != sourceControllerId` instead of " +
                "GameState.getOpponents(sourceControllerId)."
        ) {
            processor.gatherReplacements(state, PendingGameEvent.DrawPending(teammate, 1)).size shouldBe 0
        }
    }

    test("DrawCardsEvent(EachOpponent) matches only opposing-team draws, not a teammate's") {
        val booted = boot2hg()
        val players = booted.playerIds
        val (source, teammate) = players[0] to players[1]
        val opponent = players[2]

        val state = booted.state.withReplacementPermanent(
            source, "Opposing Draw Taxer",
            ModifyDrawAmount(
                modifier = 1,
                appliesTo = EventPattern.DrawCardsEvent(player = Player.EachOpponent)
            )
        )
        val processor = ReplacementEffectProcessor()

        withClue("Sanity: the pattern does fire for a player on the opposing team") {
            processor.gatherReplacements(state, PendingGameEvent.DrawAmountPending(opponent, 1)).size shouldBe 1
        }
        withClue("The source's controller is not their own opponent") {
            processor.gatherReplacements(state, PendingGameEvent.DrawAmountPending(source, 1)).size shouldBe 0
        }
        withClue(
            "CR 102.3 — a teammate is on the SAME team, so they are not an opponent. Matching " +
                "here means the announcement-site filter used " +
                "`affectedPlayerId != sourceControllerId` instead of GameState.getOpponents."
        ) {
            processor.gatherReplacements(state, PendingGameEvent.DrawAmountPending(teammate, 1)).size shouldBe 0
        }
    }

    test("PreventLifeGain(EachOpponent) locks the opposing team, never the controller's teammate (CR 810.9g)") {
        val booted = boot2hg()
        val players = booted.playerIds
        val (source, teammate) = players[0] to players[1]
        val opponent = players[2]
        val state = booted.state.withReplacementPermanent(
            source, "Opposing Lifegain Lock",
            PreventLifeGain(appliesTo = EventPattern.LifeGainEvent(player = Player.EachOpponent))
        )
        com.wingedsheep.engine.handlers.effects.DamageUtils.isLifeGainPrevented(state, opponent) shouldBe true
        com.wingedsheep.engine.handlers.effects.DamageUtils.isLifeGainPrevented(state, source) shouldBe false
        withClue("CR 102.3 — the teammate is not an opponent, so Gríma-style locks must not reach them") {
            com.wingedsheep.engine.handlers.effects.DamageUtils.isLifeGainPrevented(state, teammate) shouldBe false
        }
    }

    test("ModifyLifeGain(EachOpponent) scales the opposing team's gains, not the teammate's") {
        val booted = boot2hg()
        val players = booted.playerIds
        val (source, teammate) = players[0] to players[1]
        val opponent = players[2]
        val state = booted.state.withReplacementPermanent(
            source, "Opposing Lifegain Doubler",
            ModifyLifeGain(multiplier = 2, appliesTo = EventPattern.LifeGainEvent(player = Player.EachOpponent))
        )
        com.wingedsheep.engine.handlers.effects.LifeGainModifiers.apply(state, opponent, 3) shouldBe 6
        com.wingedsheep.engine.handlers.effects.LifeGainModifiers.apply(state, teammate, 3) shouldBe 3
        com.wingedsheep.engine.handlers.effects.LifeGainModifiers.apply(state, source, 3) shouldBe 3
    }

    test("'whenever an opponent gains life' does not trigger off a teammate's gain") {
        val booted = boot2hg()
        val players = booted.playerIds
        val (source, teammate) = players[0] to players[1]
        val opponent = players[2]
        val matcher = com.wingedsheep.engine.event.TriggerMatcher(
            com.wingedsheep.engine.handlers.PredicateEvaluator(),
            com.wingedsheep.engine.handlers.ConditionEvaluator()
        )
        fun gainBy(player: EntityId) = com.wingedsheep.engine.core.LifeChangedEvent(
            player, 30, 32, com.wingedsheep.engine.core.LifeChangeReason.LIFE_GAIN
        )
        val pattern = EventPattern.LifeGainEvent(player = Player.EachOpponent)
        val binding = com.wingedsheep.sdk.scripting.TriggerBinding.ANY
        matcher.matchesTrigger(pattern, binding, gainBy(opponent), source, source, booted.state) shouldBe true
        matcher.matchesTrigger(pattern, binding, gainBy(teammate), source, source, booted.state) shouldBe false
        matcher.matchesTrigger(pattern, binding, gainBy(source), source, source, booted.state) shouldBe false
    }

    test("EachOpponent still matches every other seat in a non-team game") {
        val booted = GameInitializer(registry()).initializeGame(
            GameConfig(
                players = (1..3).map { PlayerConfig("Player $it", Deck.of("Forest" to 40)) },
                startingPlayerIndex = 0,
                skipMulligans = true,
            )
        )
        val players = booted.playerIds
        val source = players[0]

        val state = booted.state.withReplacementPermanent(
            source, "Opposing Draw Preventer",
            PreventDraw(appliesTo = EventPattern.DrawEvent(player = Player.EachOpponent))
        )
        val processor = ReplacementEffectProcessor()

        withClue("No TeamComponent means every other seat is an opponent") {
            processor.gatherReplacements(state, PendingGameEvent.DrawPending(players[1], 1)).size shouldBe 1
            processor.gatherReplacements(state, PendingGameEvent.DrawPending(players[2], 1)).size shouldBe 1
        }
        withClue("A player is never their own opponent") {
            processor.gatherReplacements(state, PendingGameEvent.DrawPending(source, 1)).size shouldBe 0
        }
    }
})
