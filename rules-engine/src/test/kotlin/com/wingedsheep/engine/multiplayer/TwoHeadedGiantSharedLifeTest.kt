package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.life.GainLifeExecutor
import com.wingedsheep.engine.handlers.effects.life.LoseLifeExecutor
import com.wingedsheep.engine.handlers.effects.life.SetLifeTotalExecutor
import com.wingedsheep.engine.mechanics.sba.player.PlayerLifeLossCheck
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.SetLifeTotalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Two-Headed Giant — Phase 2: shared team life total (CR 810.4 / 810.9).
 *
 * A team has one life total. Damage, life loss, and life gain happen to a player individually but
 * apply to the team's shared total (CR 810.9); a cost or effect that reads an individual player's
 * life uses the team's total (CR 810.9a). We verify the resolver + every routed read/write site:
 * both teammates report the same total, changes to either move the one shared pool, "each opponent"
 * drains a whole opposing team, and a 0-life team's members are all marked lost.
 *
 * Teams are [[0,1],[2,3]] with turn order pinned to player order.
 */
class TwoHeadedGiantSharedLifeTest : FunSpec({

    fun registry(): CardRegistry = CardRegistry().also { it.register(TestCards.all) }

    /** Boot a 2HG game; returns state + the four player ids (p0,p1 = team 0; p2,p3 = team 1). */
    fun boot(): Pair<GameState, List<EntityId>> {
        val result = GameInitializer(registry()).initializeGame(
            GameConfig(
                format = Format.TwoHeadedGiant(),
                players = (1..4).map { PlayerConfig("Player $it", Deck.of("Forest" to 40)) },
                teams = listOf(listOf(0, 1), listOf(2, 3)),
                startingPlayerIndex = 0,
                skipMulligans = true,
            )
        )
        return result.state to result.playerIds
    }

    fun ctx(controller: EntityId) = EffectContext(sourceId = null, controllerId = controller)

    test("both teammates report the same shared life total, starting at 30") {
        val (state, p) = boot()
        state.lifeTotal(p[0]) shouldBe 30
        state.lifeTotal(p[1]) shouldBe 30
        state.lifeTotal(p[2]) shouldBe 30
        state.lifeTotal(p[3]) shouldBe 30
        // The canonical owner of each team is its first member.
        state.teamLifeOwnerOf(p[1]) shouldBe p[0]
        state.teamLifeOwnerOf(p[3]) shouldBe p[2]
    }

    test("life loss by one teammate moves the single shared pool both teammates read") {
        val (state, p) = boot()
        val (after, _) = DamageUtils.loseLife(state, p[1], 5, LifeChangeReason.LIFE_LOSS)

        after.lifeTotal(p[0]) shouldBe 25
        after.lifeTotal(p[1]) shouldBe 25
        // The other team is untouched.
        after.lifeTotal(p[2]) shouldBe 30
        // Only the canonical owner's raw component changed; the teammate's is never read.
        after.getEntity(p[0])!!.get<LifeTotalComponent>()!!.life shouldBe 25
    }

    test("life gain by one teammate raises the shared total") {
        val (state, p) = boot()
        val (after, _) = DamageUtils.gainLife(state, p[1], 4)
        after.lifeTotal(p[0]) shouldBe 34
        after.lifeTotal(p[1]) shouldBe 34
    }

    test("damage to a teammate reduces the shared team total") {
        val (state, p) = boot()
        val result = DamageUtils.dealDamageToTarget(state, p[0], 6, sourceId = null)
        result.state.lifeTotal(p[0]) shouldBe 24
        result.state.lifeTotal(p[1]) shouldBe 24
        result.state.lifeTotal(p[2]) shouldBe 30
    }

    test("an EachOpponent loss drains BOTH players of the opposing team from one shared pool") {
        val (state, p) = boot()
        // Controller is p0 (team 0); EachOpponent = p2 and p3 (team 1). Each loses 3 → team 1
        // shares one pool, so the two individual 3-life losses stack to a 6-life drop.
        val effect = LoseLifeEffect(
            amount = DynamicAmount.Fixed(3),
            target = EffectTarget.PlayerRef(Player.EachOpponent)
        )
        val result = LoseLifeExecutor().execute(state, effect, ctx(p[0]))

        result.state.lifeTotal(p[0]) shouldBe 30 // controller's team untouched
        result.state.lifeTotal(p[2]) shouldBe 24 // 30 - 3 - 3
        result.state.lifeTotal(p[3]) shouldBe 24
    }

    test("an EachOpponent gain via GainLife also resolves per-opposing-player onto their shared pool") {
        val (state, p) = boot()
        val effect = GainLifeEffect(
            amount = DynamicAmount.Fixed(2),
            target = EffectTarget.PlayerRef(Player.EachOpponent)
        )
        val result = GainLifeExecutor().execute(state, effect, ctx(p[0]))
        result.state.lifeTotal(p[2]) shouldBe 34 // 30 + 2 + 2
        result.state.lifeTotal(p[0]) shouldBe 30
    }

    test("SetLifeTotal sets the team's shared total (CR 810.9c)") {
        val (state, p) = boot()
        val effect = SetLifeTotalEffect(
            amount = DynamicAmount.Fixed(10),
            target = EffectTarget.PlayerRef(Player.You)
        )
        val result = SetLifeTotalExecutor().execute(state, effect, ctx(p[3]))
        result.state.lifeTotal(p[2]) shouldBe 10
        result.state.lifeTotal(p[3]) shouldBe 10
        result.state.lifeTotal(p[0]) shouldBe 30
    }

    test("adjustLife / withLifeTotal resolver helpers target the team pool") {
        val (state, p) = boot()
        state.adjustLife(p[1], -7).lifeTotal(p[0]) shouldBe 23
        state.withLifeTotal(p[3], 5).lifeTotal(p[2]) shouldBe 5
    }

    test("when a team's shared total hits 0, both teammates are marked lost in one SBA pass") {
        val (state, p) = boot()
        // Drop team 0 to exactly 0 via a single teammate's loss.
        val (zeroed, _) = DamageUtils.loseLife(state, p[0], 30, LifeChangeReason.LIFE_LOSS)
        zeroed.lifeTotal(p[1]) shouldBe 0

        val result = PlayerLifeLossCheck().check(zeroed)
        result.newState.getEntity(p[0])!!.has<PlayerLostComponent>() shouldBe true
        result.newState.getEntity(p[1])!!.has<PlayerLostComponent>() shouldBe true
        // The opposing team is unaffected.
        result.newState.getEntity(p[2])!!.has<PlayerLostComponent>() shouldBe false
    }

    test("a non-team game is a pure pass-through: lifeTotal equals the player's own life") {
        val result = GameInitializer(registry()).initializeGame(
            GameConfig(
                players = (1..2).map { PlayerConfig("Player $it", Deck.of("Forest" to 40)) },
                startingPlayerIndex = 0,
                skipMulligans = true,
            )
        )
        val p = result.playerIds
        val state = result.state
        state.teamLifeOwnerOf(p[0]) shouldBe p[0]
        val (after, _) = DamageUtils.loseLife(state, p[0], 4, LifeChangeReason.LIFE_LOSS)
        after.lifeTotal(p[0]) shouldBe 16
        after.lifeTotal(p[1]) shouldBe 20 // opponent untouched — they are their own team
        after.getEntity(p[0])!!.get<LifeTotalComponent>()!!.life shouldBe 16
    }
})
