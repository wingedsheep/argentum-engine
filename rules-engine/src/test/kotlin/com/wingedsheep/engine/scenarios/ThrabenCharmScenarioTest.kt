package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Thraben Charm (MH3) — a `modal(chooseCount = 1)` charm whose third mode is the first use of
 * `TargetPlayer(unlimited = true)` *inside a modal mode*. Every prior user of an unlimited player
 * target (Riverchurn Monument, The Death of Gwen Stacy, Kaboom!) is a top-level ability or a saga
 * chapter, where the target list is the spell's whole target list; here it has to survive being
 * bound to one mode's scope and read back as `Player.ContextPlayer(0)` inside a
 * [com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect]. Both ends of "any number" are
 * covered: two players and none.
 *
 * Mode 1's amount is doubled, not fixed, so it is checked at two different creature counts — a
 * hard-coded 4 would pass the first case alone.
 */
class ThrabenCharmScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        return d
    }

    /**
     * Cast Thraben Charm choosing [modeIndex], binding [modeTargets] to that mode. Modal targets go
     * in `modeTargetsOrdered` (aligned 1:1 with `chosenModes`) as well as the flat `targets` list —
     * resolution reads `ContextTarget`/`ContextPlayer` out of the per-mode bindings.
     */
    fun castCharm(
        d: GameTestDriver,
        playerId: EntityId,
        cardId: EntityId,
        modeIndex: Int,
        modeTargets: List<ChosenTarget> = emptyList()
    ) = d.submit(
        CastSpell(
            playerId = playerId,
            cardId = cardId,
            targets = modeTargets,
            chosenModes = listOf(modeIndex),
            modeTargetsOrdered = listOf(modeTargets),
            paymentStrategy = PaymentStrategy.FromPool
        )
    )

    fun damageOn(d: GameTestDriver, entityId: EntityId): Int =
        d.state.getEntity(entityId)?.get<DamageComponent>()?.amount ?: 0

    fun payFor(d: GameTestDriver, playerId: EntityId) {
        d.giveMana(playerId, Color.WHITE, 1)
        d.giveColorlessMana(playerId, 1)
    }

    test("mode 1 deals twice your creature count — two creatures kill a 3/3") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Grizzly Bears")
        d.putCreatureOnBattlefield(active, "Grizzly Bears")
        val courser = d.putCreatureOnBattlefield(opp, "Centaur Courser")

        payFor(d, active)
        val charm = d.putCardInHand(active, "Thraben Charm")
        castCharm(d, active, charm, 0, listOf(ChosenTarget.Permanent(courser))).isSuccess shouldBe true
        d.bothPass()

        // 2 creatures × 2 = 4 damage, lethal to a 3/3.
        d.findPermanent(opp, "Centaur Courser") shouldBe null
        d.getGraveyardCardNames(opp) shouldContain "Centaur Courser"
    }

    test("mode 1 with one creature deals only 2 — the amount tracks the creature count") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Grizzly Bears")
        val courser = d.putCreatureOnBattlefield(opp, "Centaur Courser")

        payFor(d, active)
        val charm = d.putCardInHand(active, "Thraben Charm")
        castCharm(d, active, charm, 0, listOf(ChosenTarget.Permanent(courser))).isSuccess shouldBe true
        d.bothPass()

        damageOn(d, courser) shouldBe 2
        d.findPermanent(opp, "Centaur Courser") shouldBe courser
    }

    test("mode 2 destroys target enchantment") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val glade = d.putPermanentOnBattlefield(opp, "Centaur Glade")

        payFor(d, active)
        val charm = d.putCardInHand(active, "Thraben Charm")
        castCharm(d, active, charm, 1, listOf(ChosenTarget.Permanent(glade))).isSuccess shouldBe true
        d.bothPass()

        d.findPermanent(opp, "Centaur Glade") shouldBe null
        d.getGraveyardCardNames(opp) shouldContain "Centaur Glade"
    }

    test("mode 3 exiles the graveyards of every targeted player") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCardInGraveyard(active, "Grizzly Bears")
        d.putCardInGraveyard(opp, "Centaur Courser")
        d.putCardInGraveyard(opp, "Hill Giant")

        payFor(d, active)
        val charm = d.putCardInHand(active, "Thraben Charm")
        castCharm(
            d, active, charm, 2,
            listOf(ChosenTarget.Player(active), ChosenTarget.Player(opp))
        ).isSuccess shouldBe true
        d.bothPass()

        d.getExileCardNames(active) shouldContain "Grizzly Bears"
        d.getExileCardNames(opp) shouldContain "Centaur Courser"
        d.getExileCardNames(opp) shouldContain "Hill Giant"

        // Both graveyards are emptied, including the caster's own. What's left in the caster's is
        // Thraben Charm itself, put there as the last step of its own resolution (CR 608.2m) —
        // after the exile, so it isn't caught by it.
        d.getGraveyardCardNames(active) shouldBe listOf("Thraben Charm")
        d.getGraveyard(opp) shouldBe emptyList()
    }

    test("mode 3 with zero targets resolves and exiles nothing") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCardInGraveyard(active, "Grizzly Bears")
        d.putCardInGraveyard(opp, "Centaur Courser")

        payFor(d, active)
        val charm = d.putCardInHand(active, "Thraben Charm")
        castCharm(d, active, charm, 2).isSuccess shouldBe true
        d.bothPass()

        // "Any number" includes none: the spell resolves, nothing is exiled, and — the point of the
        // case — it doesn't fizzle for lack of a legal target or fall through to some default
        // "every player" reading.
        d.getGraveyardCardNames(active) shouldContain "Grizzly Bears"
        d.getGraveyardCardNames(opp) shouldContain "Centaur Courser"
        d.getExile(active) shouldBe emptyList()
        d.getExile(opp) shouldBe emptyList()
    }
})
