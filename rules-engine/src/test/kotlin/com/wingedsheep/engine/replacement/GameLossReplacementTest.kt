package com.wingedsheep.engine.replacement

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantCantLoseGame
import com.wingedsheep.sdk.scripting.ReplaceGameLossWith
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * [ReplaceGameLossWith] — "If you would lose the game, instead …" (CR 614.1a over every way to
 * lose, CR 104.3). Pins: each loss route is replaced (0 life, poison, empty-library draw, a
 * "loses the game" effect); "can't lose" wins over it; one event is replaced once, so two copies
 * spend one at a time; a reason that still holds afterwards (poison) loses at the next check; an
 * opponent's replacement doesn't protect you; a source that lost its abilities doesn't apply; and
 * a source that dies in the same state-based-action pass asks its controller exile-or-graveyard
 * (the Archangel's first ruling); in Two-Headed Giant a "you" replacement protects the team.
 */
class GameLossReplacementTest : FunSpec({

    val archangel = card("Lossward Archangel") {
        manaCost = "{0}"
        typeLine = "Creature — Angel"
        power = 5
        toughness = 5
        keywords(Keyword.FLYING)
        replacementEffect(
            ReplaceGameLossWith(
                Effects.Exile(EffectTarget.Self, fromZone = Zone.BATTLEFIELD) then
                    Effects.SetLifeTotal(DynamicAmounts.startingLifeTotal())
            )
        )
    }
    val platinum = card("Lossward Platinum") {
        manaCost = "{0}"
        typeLine = "Artifact Creature — Angel"
        power = 2
        toughness = 2
        staticAbility { ability = GrantCantLoseGame }
    }
    val loseThirty = card("Lossward Lifeloss") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.LoseLife(30, EffectTarget.Controller) }
    }
    val poison = card("Lossward Poison") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.AddCounters(CounterType.POISON, 10, EffectTarget.Controller) }
    }
    val drawAll = card("Lossward Draw") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.DrawCards(60) }
    }
    val forfeit = card("Lossward Forfeit") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.LoseGame(EffectTarget.Controller) }
    }
    val victory = card("Lossward Victory") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.WinGame(EffectTarget.Controller) }
    }
    // Both halves land before state-based actions: the Archangel's lethal damage and its
    // controller's 0 life are one SBA pass.
    val inferno = card("Lossward Inferno") {
        manaCost = "{0}"; typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.DealDamage(5, t) then Effects.LoseLife(30, EffectTarget.Controller)
        }
    }
    val blank = card("Lossward Blank") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.RemoveAllAbilities(target(TargetFilter.Creature)) }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(archangel, platinum, loseThirty, poison, drawAll, forfeit, victory, inferno, blank))
        initMirrorMatch(deck = Deck.of("Plains" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.cast(player: EntityId, name: String, targets: List<EntityId> = emptyList()) {
        val holder = state.priorityPlayerId
        if (holder != null && holder != player) passPriority(holder)
        castSpell(player, putCardInHand(player, name), targets).outcome shouldBe Outcome.Done
        var guard = 0
        while (!state.gameOver && state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    test("0 or less life: the Archangel is exiled and life becomes the starting total") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Lossward Archangel")
        d.cast(d.player1, "Lossward Lifeloss")

        d.state.gameOver.shouldBeFalse()
        d.getLifeTotal(d.player1) shouldBe 20
        d.getExileCardNames(d.player1) shouldContain "Lossward Archangel"
        d.findPermanent(d.player1, "Lossward Archangel") shouldBe null
    }

    test("a 'loses the game' effect is replaced") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Lossward Archangel")
        d.cast(d.player1, "Lossward Forfeit")

        d.state.gameOver.shouldBeFalse()
        d.getExileCardNames(d.player1) shouldContain "Lossward Archangel"
    }

    test("an empty-library draw is replaced, and the attempt isn't held over to the next check") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Lossward Archangel")
        d.cast(d.player1, "Lossward Draw")

        d.state.gameOver.shouldBeFalse()
        d.getExileCardNames(d.player1) shouldContain "Lossward Archangel"
        // The replaced pass re-runs state-based actions, so a held-over attempt would already have
        // lost here; the player is still in the game.
        d.state.getEntity(d.player1)?.has<PlayerLostComponent>() shouldBe false
    }

    test("poison: the loss is replaced once, but ten counters still lose at the next check") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Lossward Archangel")
        d.cast(d.player1, "Lossward Poison")

        d.getExileCardNames(d.player1) shouldContain "Lossward Archangel"
        d.state.gameOver.shouldBeTrue()
        d.state.winnerId shouldBe d.player2
    }

    test("can't lose the game wins: there is no loss to replace") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Lossward Archangel")
        d.putCreatureOnBattlefield(d.player1, "Lossward Platinum")
        d.cast(d.player1, "Lossward Lifeloss")

        d.state.gameOver.shouldBeFalse()
        d.getLifeTotal(d.player1) shouldBe -10
        (d.findPermanent(d.player1, "Lossward Archangel") != null).shouldBeTrue()
    }

    test("two Archangels: one loss spends one of them") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Lossward Archangel")
        d.putCreatureOnBattlefield(d.player1, "Lossward Archangel")
        d.cast(d.player1, "Lossward Lifeloss")

        d.state.gameOver.shouldBeFalse()
        d.getLifeTotal(d.player1) shouldBe 20
        d.getExileCardNames(d.player1).count { it == "Lossward Archangel" } shouldBe 1
        (d.findPermanent(d.player1, "Lossward Archangel") != null).shouldBeTrue()
    }

    test("an opponent's Archangel doesn't protect you") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player2, "Lossward Archangel")
        d.cast(d.player1, "Lossward Lifeloss")

        d.state.gameOver.shouldBeTrue()
        d.state.winnerId shouldBe d.player2
        (d.findPermanent(d.player2, "Lossward Archangel") != null).shouldBeTrue()
    }

    test("an opponent winning the game isn't a loss it can replace") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Lossward Archangel")
        d.cast(d.player2, "Lossward Victory")

        d.state.gameOver.shouldBeTrue()
        d.state.winnerId shouldBe d.player2
    }

    test("an Archangel that lost its abilities doesn't apply") {
        val d = driver()
        val angel = d.putCreatureOnBattlefield(d.player1, "Lossward Archangel")
        d.cast(d.player1, "Lossward Blank", listOf(angel))
        d.cast(d.player1, "Lossward Lifeloss")

        d.state.gameOver.shouldBeTrue()
        d.state.winnerId shouldBe d.player2
    }

    test("dealt lethal damage in the same pass, its controller chooses: exile") {
        val d = driver()
        val angel = d.putCreatureOnBattlefield(d.player1, "Lossward Archangel")
        d.castSpell(d.player1, d.putCardInHand(d.player1, "Lossward Inferno"), listOf(angel))
        d.bothPass()

        val decision = d.pendingDecision as YesNoDecision
        decision.playerId shouldBe d.player1
        d.submitYesNo(d.player1, true)

        d.state.gameOver.shouldBeFalse()
        d.getLifeTotal(d.player1) shouldBe 20
        d.getExileCardNames(d.player1) shouldContain "Lossward Archangel"
        d.getGraveyardCardNames(d.player1) shouldNotContain "Lossward Archangel"
    }

    test("dealt lethal damage in the same pass, its controller chooses: graveyard") {
        val d = driver()
        val angel = d.putCreatureOnBattlefield(d.player1, "Lossward Archangel")
        d.castSpell(d.player1, d.putCardInHand(d.player1, "Lossward Inferno"), listOf(angel))
        d.bothPass()

        (d.pendingDecision is YesNoDecision).shouldBeTrue()
        d.submitYesNo(d.player1, false)

        d.state.gameOver.shouldBeFalse()
        d.getLifeTotal(d.player1) shouldBe 20
        d.getGraveyardCardNames(d.player1) shouldContain "Lossward Archangel"
        d.getExileCardNames(d.player1) shouldNotContain "Lossward Archangel"
    }

    test("a source that survives the pass isn't asked about") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Lossward Archangel")
        d.cast(d.player1, "Lossward Lifeloss")
        d.pendingDecision shouldBe null
    }

    test("Two-Headed Giant: my Archangel replaces my teammate's loss, and the team keeps playing") {
        val d = GameTestDriver().apply { registerCards(TestCards.all + listOf(archangel, drawAll)) }
        val players = d.initMultiplayer(
            decks = List(4) { Deck.of("Plains" to 40) },
            format = Format.TwoHeadedGiant(),
            teams = listOf(listOf(0, 1), listOf(2, 3)),
        )
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (me, teammate) = players[0] to players[1]
        d.putCreatureOnBattlefield(me, "Lossward Archangel")
        d.cast(teammate, "Lossward Draw")

        d.state.gameOver.shouldBeFalse()
        d.getExileCardNames(me) shouldContain "Lossward Archangel"
        d.state.getEntity(teammate)?.has<PlayerLostComponent>() shouldBe false
    }
})
