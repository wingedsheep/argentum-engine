package com.wingedsheep.engine.mechanics.sba

import com.wingedsheep.engine.mechanics.sba.player.CommanderDamageLossCheck
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class CommanderDamageLossCheckTest : FunSpec({

    val p1 = EntityId.generate()
    val p2 = EntityId.generate()
    val cmdrA = EntityId.generate()
    val cmdrB = EntityId.generate()

    fun baseState(format: Format): GameState {
        return GameState(format = format)
            .withEntity(p1, ComponentContainer.of(PlayerComponent("P1", 40), LifeTotalComponent(40)))
            .withEntity(p2, ComponentContainer.of(PlayerComponent("P2", 40), LifeTotalComponent(40)))
            .copy(turnOrder = listOf(p1, p2))
    }

    test("21+ damage from a single commander makes the defender lose") {
        val state = baseState(Format.Commander())
            .recordCommanderDamage(cmdrA, p2, 21)
        val result = CommanderDamageLossCheck().check(state)
        val updatedP2 = result.newState.getEntity(p2)
        updatedP2!!.get<PlayerLostComponent>() shouldNotBe null
        updatedP2.get<PlayerLostComponent>()!!.reason shouldBe LossReason.COMMANDER_DAMAGE
    }

    test("20 damage is not enough; defender stays alive") {
        val state = baseState(Format.Commander())
            .recordCommanderDamage(cmdrA, p2, 20)
        val result = CommanderDamageLossCheck().check(state)
        result.newState.getEntity(p2)!!.get<PlayerLostComponent>() shouldBe null
    }

    test("damage from two different commanders does not aggregate") {
        // 11 + 11 from two different commanders → still alive
        val state = baseState(Format.Commander())
            .recordCommanderDamage(cmdrA, p2, 11)
            .recordCommanderDamage(cmdrB, p2, 11)
        val result = CommanderDamageLossCheck().check(state)
        result.newState.getEntity(p2)!!.get<PlayerLostComponent>() shouldBe null
    }

    test("Standard format: SBA never fires regardless of commanderDamage tally") {
        val state = baseState(Format.Standard)
            .recordCommanderDamage(cmdrA, p2, 99)
        val result = CommanderDamageLossCheck().check(state)
        result.newState.getEntity(p2)!!.get<PlayerLostComponent>() shouldBe null
    }

    test("threshold respects format config") {
        // Pretend Brawl-shaped variant with a hypothetical 30-damage threshold
        val state = baseState(Format.Commander(commanderDamageThreshold = 30))
            .recordCommanderDamage(cmdrA, p2, 25)
        CommanderDamageLossCheck().check(state).newState
            .getEntity(p2)!!.get<PlayerLostComponent>() shouldBe null

        val state2 = baseState(Format.Commander(commanderDamageThreshold = 30))
            .recordCommanderDamage(cmdrA, p2, 30)
        CommanderDamageLossCheck().check(state2).newState
            .getEntity(p2)!!.get<PlayerLostComponent>() shouldNotBe null
    }

    test("Team vs Team applies commander damage when Commander rules are enabled") {
        val format = Format.TeamVsTeam(
            startingLife = 40,
            commanderDamageThreshold = 21,
            deckSize = 100,
        )
        val state = baseState(format).recordCommanderDamage(cmdrA, p2, 21)

        CommanderDamageLossCheck().check(state).newState
            .getEntity(p2)!!.get<PlayerLostComponent>() shouldNotBe null
    }
})
