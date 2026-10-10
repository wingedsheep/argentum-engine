package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.player.TheRingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltc.cards.SauronLordOfTheRings
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Sauron, Lord of the Rings (LTC #4).
 *
 *   When you cast this spell, amass Orcs 5, mill five cards, then return a creature card from
 *   your graveyard to the battlefield.
 *   Whenever a commander an opponent controls dies, the Ring tempts you.
 *
 * Pins the cast-trigger pipeline (resolving above Sauron on the stack, the returned card chosen on
 * resolution from the whole graveyard) and the first trigger keyed on the commander designation.
 */
class SauronLordOfTheRingsScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(SauronLordOfTheRings))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 40)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.tagCommander(entity: EntityId, owner: EntityId) =
        replaceState(state.updateEntity(entity) { it.with(CommanderComponent(ownerId = owner)) })

    fun GameTestDriver.armiesOf(player: EntityId): List<EntityId> {
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(player)
            .filter { projected.isCreature(it) && projected.hasSubtype(it, "Army") }
    }

    fun GameTestDriver.temptCount(player: EntityId): Int =
        state.getEntity(player)?.get<TheRingComponent>()?.temptCount ?: 0

    fun GameTestDriver.librarySize(player: EntityId): Int =
        state.getZone(ZoneKey(player, Zone.LIBRARY)).size

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 50) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
        }
    }

    test("cast trigger amasses Orcs 5, mills five, and returns a chosen creature card before Sauron resolves") {
        val d = driver()
        val me = d.activePlayer!!
        val bears = d.putCardInGraveyard(me, "Grizzly Bears")
        val lions = d.putCardInGraveyard(me, "Savannah Lions")
        val sauron = d.putCardInHand(me, "Sauron, Lord of the Rings")
        val libraryBefore = d.librarySize(me)

        d.giveMana(me, Color.BLUE, 1)
        d.giveMana(me, Color.BLACK, 1)
        d.giveMana(me, Color.RED, 6)
        d.castSpell(me, sauron).error shouldBe null
        d.stackSize shouldBe 2 // Sauron + its cast trigger above it

        d.bothPass()
        var guard = 0
        while (d.state.pendingDecision != null && guard++ < 5) {
            val decision = d.state.pendingDecision
            if (decision is SelectCardsDecision && bears in decision.options) {
                // An already-present card is a legal pick, not only one just milled.
                decision.options shouldContain lions
                d.submitCardSelection(me, listOf(bears)).error shouldBe null
            } else {
                d.autoResolveDecision()
            }
        }

        // The trigger has resolved; Sauron itself is still on the stack.
        d.stackSize shouldBe 1
        d.state.getBattlefield() shouldContain bears
        d.getGraveyard(me) shouldContain lions
        d.getGraveyard(me) shouldNotContain bears
        d.librarySize(me) shouldBe libraryBefore - 5
        d.getGraveyardCardNames(me).count { it == "Mountain" } shouldBe 5

        val army = d.armiesOf(me).single()
        d.state.projectedState.hasSubtype(army, "Orc") shouldBe true
        d.state.getEntity(army)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 5

        d.resolveStack()
        d.state.getBattlefield() shouldContain sauron
    }

    test("an opponent's commander dying tempts you; a non-commander or your own commander does not") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, "Sauron, Lord of the Rings")

        val theirLions = d.putCreatureOnBattlefield(opp, "Savannah Lions")
        val myCommander = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.tagCommander(myCommander, me)
        val theirCommander = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.tagCommander(theirCommander, opp)

        fun bolt(target: EntityId) {
            val spell = d.putCardInHand(me, "Lightning Bolt")
            d.giveMana(me, Color.RED, 1)
            d.castSpell(me, spell, listOf(target)).error shouldBe null
            d.resolveStack()
        }

        bolt(theirLions)
        d.getGraveyard(opp) shouldContain theirLions
        d.temptCount(me) shouldBe 0

        bolt(myCommander)
        d.getGraveyard(me) shouldContain myCommander
        d.temptCount(me) shouldBe 0

        bolt(theirCommander)
        d.getGraveyard(opp) shouldContain theirCommander
        d.temptCount(me) shouldBe 1
    }
})
