package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.CounselOfTheSoratami
import com.wingedsheep.mtg.sets.definitions.ltc.cards.SummonsOfSaruman
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Summons of Saruman (LTC #70).
 *
 *   Amass Orcs X. Mill X cards. You may cast an instant or sorcery spell with mana value X or less
 *   from among them without paying its mana cost.
 *   Flashback—{3}{U}{R}, Exile X cards from your graveyard.
 *
 * Pins the flashback half: the number of cards exiled is the spell's X (CR 107.3a), and the spell
 * can't exile itself to pay (it's on the stack by then, CR 601.2a).
 */
class SummonsOfSarumanScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(SummonsOfSaruman, CounselOfTheSoratami))
        initMirrorMatch(deck = Deck.of("Mountain" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.armyCounters(player: EntityId): Int {
        val projected = state.projectedState
        val army = projected.getBattlefieldControlledBy(player)
            .single { projected.isCreature(it) && projected.hasSubtype(it, "Army") }
        projected.hasSubtype(army, "Orc") shouldBe true
        return state.getEntity(army)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    fun GameTestDriver.librarySize(player: EntityId): Int = state.getZone(ZoneKey(player, Zone.LIBRARY)).size

    fun GameTestDriver.giveFlashbackMana(player: EntityId) {
        giveMana(player, Color.BLUE, 1)
        giveMana(player, Color.RED, 4)
    }

    fun flashback(player: EntityId, card: EntityId, exiled: List<EntityId>) = CastSpell(
        playerId = player,
        cardId = card,
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.FLASHBACK,
        additionalCostPayment = AdditionalCostPayment(exiledCards = exiled),
        paymentStrategy = PaymentStrategy.FromPool,
    )

    test("flashback exiling two cards resolves with X = 2: amass Orcs 2, mill two, then exile itself") {
        val d = driver()
        val me = d.activePlayer!!
        val summons = d.putCardInGraveyard(me, "Summons of Saruman")
        val bears = d.putCardInGraveyard(me, "Grizzly Bears")
        val lions = d.putCardInGraveyard(me, "Savannah Lions")
        val libraryBefore = d.librarySize(me)

        d.giveFlashbackMana(me)
        val cast = d.submit(flashback(me, summons, listOf(bears, lions)))
        withClue("flashback with two exiled cards: ${cast.error}") { cast.error shouldBe null }
        d.getExile(me) shouldContain bears
        d.getExile(me) shouldContain lions

        d.bothPass()
        while (d.state.pendingDecision != null) d.autoResolveDecision()

        d.armyCounters(me) shouldBe 2
        d.librarySize(me) shouldBe libraryBefore - 2
        withClue("a flashback spell is exiled as it leaves the stack") { d.getExile(me) shouldContain summons }
    }

    test("flashback exiling no cards resolves with X = 0") {
        val d = driver()
        val me = d.activePlayer!!
        val summons = d.putCardInGraveyard(me, "Summons of Saruman")
        val libraryBefore = d.librarySize(me)

        d.giveFlashbackMana(me)
        d.submit(flashback(me, summons, emptyList())).error shouldBe null
        d.bothPass()
        while (d.state.pendingDecision != null) d.autoResolveDecision()

        withClue("amass 0 creates a 0/0 Army that dies to state-based actions") {
            d.state.projectedState.getBattlefieldControlledBy(me)
                .none { d.state.projectedState.hasSubtype(it, "Army") } shouldBe true
        }
        d.librarySize(me) shouldBe libraryBefore
        d.getExile(me) shouldContain summons
    }

    test("the flashback exile picker leaves out the spell being cast, and naming it is rejected") {
        val d = driver()
        val me = d.activePlayer!!
        val summons = d.putCardInGraveyard(me, "Summons of Saruman")
        val bears = d.putCardInGraveyard(me, "Grizzly Bears")
        d.giveFlashbackMana(me)

        val action = d.legalActions(me).firstOrNull { it.actionType == "CastWithFlashback" }.shouldNotBeNull()
        val info = action.additionalCostInfo.shouldNotBeNull()
        info.costType shouldBe "ExileFromGraveyard"
        info.validExileTargets shouldContainExactlyInAnyOrder listOf(bears)
        info.exileMinCount shouldBe 0

        val selfExile = d.submit(flashback(me, summons, listOf(summons, bears)))
        selfExile.error shouldNotBe null
        d.getGraveyard(me) shouldContain summons
        d.getGraveyard(me) shouldContain bears
    }

    test("cast from hand for X = 3, it casts a milled sorcery with mana value 3 for free") {
        val d = driver()
        val me = d.activePlayer!!
        val summons = d.putCardInHand(me, "Summons of Saruman")
        val counsel = d.putCardOnTopOfLibrary(me, "Counsel of the Soratami")
        val handBefore = d.getHand(me).size

        d.giveMana(me, Color.BLUE, 1)
        d.giveMana(me, Color.RED, 4)
        d.submit(
            CastSpell(playerId = me, cardId = summons, xValue = 3, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        d.bothPass()

        val choice = d.state.pendingDecision
        choice as SelectCardsDecision
        choice.options shouldContain counsel
        d.submitCardSelection(me, listOf(counsel)).error shouldBe null
        var guard = 0
        while ((d.state.pendingDecision != null || d.state.stack.isNotEmpty()) && guard++ < 20) {
            if (d.state.pendingDecision != null) d.autoResolveDecision() else d.bothPass()
        }

        d.armyCounters(me) shouldBe 3
        withClue("Counsel of the Soratami drew two (Summons left the hand)") {
            d.getHand(me).size shouldBe handBefore - 1 + 2
        }
        d.getGraveyard(me) shouldContain counsel
        d.getGraveyard(me) shouldContain summons
        d.getExile(me) shouldNotContain summons
    }
})
