package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.DeathknellBerserker
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Deathknell Berserker — the dies trigger's intervening-if reads the Berserker's power as it last
 * existed on the battlefield (CR 603.10, 608.2h): a +1/+1 counter it died with makes it 3 and
 * creates the Zombie Berserker; dying as a plain 2/2 creates nothing.
 */
class DeathknellBerserkerScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + DeathknellBerserker)
        initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.tokens(): List<EntityId> =
        getCreatures(player1).filter { state.getEntity(it)?.has<TokenComponent>() == true }

    fun GameTestDriver.boltAndResolve(target: EntityId) {
        val bolt = putCardInHand(player1, "Lightning Bolt")
        giveMana(player1, Color.RED, 1)
        castSpell(player1, bolt, listOf(target)).error shouldBe null
        var guard = 0
        while (pendingDecision == null && state.stack.isNotEmpty() && guard++ < 40)
            passPriority(state.priorityPlayerId!!).error shouldBe null
    }

    test("dying with power 3 from a +1/+1 counter creates a 2/2 black Zombie Berserker") {
        val d = setup()
        val berserker = d.putCreatureOnBattlefield(d.player1, DeathknellBerserker.name)
        d.addComponent(berserker, CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
        d.state.projectedState.getPower(berserker) shouldBe 3

        d.boltAndResolve(berserker)

        d.findPermanent(d.player1, DeathknellBerserker.name) shouldBe null
        val tokens = d.tokens()
        tokens shouldHaveSize 1
        val token = tokens.single()
        val projected = d.state.projectedState
        projected.getPower(token) shouldBe 2
        projected.getToughness(token) shouldBe 2
        projected.hasColor(token, Color.BLACK) shouldBe true
        projected.hasSubtype(token, "Zombie") shouldBe true
        projected.hasSubtype(token, "Berserker") shouldBe true
    }

    test("dying as a plain 2/2 creates no token") {
        val d = setup()
        val berserker = d.putCreatureOnBattlefield(d.player1, DeathknellBerserker.name)

        d.boltAndResolve(berserker)

        d.findPermanent(d.player1, DeathknellBerserker.name) shouldBe null
        d.tokens().shouldBeEmpty()
    }
})
