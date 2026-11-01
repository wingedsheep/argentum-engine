package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Unsummon
import com.wingedsheep.mtg.sets.definitions.mir.cards.Polymorph
import com.wingedsheep.mtg.sets.definitions.tmp.cards.MoggFanatic
import com.wingedsheep.mtg.sets.definitions.ody.cards.Zombify
import com.wingedsheep.mtg.sets.definitions.ons.cards.FutureSight
import com.wingedsheep.mtg.sets.definitions.war.cards.FblthpTheLost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Fblthp, the Lost (WAR #50) {1}{U} — Legendary Creature — Homunculus 1/1.
 * "When Fblthp enters, draw a card. If it entered from your library or was cast from your
 *  library, draw two cards instead.
 *  When Fblthp becomes the target of a spell, shuffle Fblthp into its owner's library."
 *
 * Covers both library halves of `Conditions.TriggeringEntityEnteredOrWasCastFrom(Zone.LIBRARY)` (cast
 * from the top with Future Sight, put onto the battlefield by Polymorph), the one-card cases (cast
 * from hand, reanimated from a graveyard), last-known provenance when Fblthp is shuffled away before
 * its enters trigger resolves (CR 608.2h), and the shuffle-away when a spell — not an ability —
 * targets it.
 */
class FblthpTheLostScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FblthpTheLost, FutureSight, Polymorph, Zombify, Unsummon, MoggFanatic))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        repeat(6) { driver.putLandOnBattlefield(me, "Island") }
        return driver to me
    }

    fun drainStack(driver: GameTestDriver) {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
    }

    fun cast(driver: GameTestDriver, me: EntityId, card: EntityId, targets: List<ChosenTarget> = emptyList()) {
        driver.submit(
            CastSpell(playerId = me, cardId = card, targets = targets, paymentStrategy = PaymentStrategy.AutoPay)
        ).outcome shouldBe Outcome.Done
        drainStack(driver)
    }

    test("cast from hand, Fblthp draws one card") {
        val (driver, me) = setup()
        val fblthp = driver.putCardInHand(me, "Fblthp, the Lost")
        val handBefore = driver.getHandSize(me)

        cast(driver, me, fblthp)

        driver.state.getBattlefield() shouldContain fblthp
        driver.getHandSize(me) shouldBe handBefore - 1 + 1
    }

    test("cast from the top of your library, Fblthp draws two cards") {
        val (driver, me) = setup()
        driver.putPermanentOnBattlefield(me, "Future Sight")
        val fblthp = driver.putCardOnTopOfLibrary(me, "Fblthp, the Lost")
        val handBefore = driver.getHandSize(me)

        cast(driver, me, fblthp)

        driver.state.getBattlefield() shouldContain fblthp
        driver.getHandSize(me) shouldBe handBefore + 2
    }

    test("put onto the battlefield from your library, Fblthp draws two cards") {
        val (driver, me) = setup()
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val fblthp = driver.putCardOnTopOfLibrary(me, "Fblthp, the Lost")
        val polymorph = driver.putCardInHand(me, "Polymorph")
        val handBefore = driver.getHandSize(me)

        cast(driver, me, polymorph, listOf(ChosenTarget.Permanent(bears)))

        driver.state.getBattlefield() shouldContain fblthp
        driver.getHandSize(me) shouldBe handBefore - 1 + 2
    }

    test("put onto the battlefield from a graveyard, Fblthp draws only one card") {
        val (driver, me) = setup()
        driver.putLandOnBattlefield(me, "Swamp")
        val fblthp = driver.putCardInGraveyard(me, "Fblthp, the Lost")
        val zombify = driver.putCardInHand(me, "Zombify")
        val handBefore = driver.getHandSize(me)

        cast(driver, me, zombify, listOf(ChosenTarget.Card(fblthp, me, Zone.GRAVEYARD)))

        driver.state.getBattlefield() shouldContain fblthp
        driver.getHandSize(me) shouldBe handBefore - 1 + 1
    }

    test("when a spell targets Fblthp, it shuffles into its owner's library and the spell fizzles") {
        val (driver, me) = setup()
        val fblthp = driver.putCreatureOnBattlefield(me, "Fblthp, the Lost")
        val unsummon = driver.putCardInHand(me, "Unsummon")

        cast(driver, me, unsummon, listOf(ChosenTarget.Permanent(fblthp)))

        driver.state.getBattlefield() shouldNotContain fblthp
        driver.state.getZone(ZoneKey(me, Zone.LIBRARY)) shouldContain fblthp
        driver.state.getZone(ZoneKey(me, Zone.HAND)) shouldNotContain fblthp
        driver.state.getZone(ZoneKey(me, Zone.GRAVEYARD)) shouldContain unsummon
    }

    test("shuffled away in response to its enters trigger, Fblthp still draws two (last-known information)") {
        val (driver, me) = setup()
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val fblthp = driver.putCardOnTopOfLibrary(me, "Fblthp, the Lost")
        val polymorph = driver.putCardInHand(me, "Polymorph")
        val unsummon = driver.putCardInHand(me, "Unsummon")
        val handBefore = driver.getHandSize(me)

        driver.submit(
            CastSpell(
                playerId = me, cardId = polymorph, targets = listOf(ChosenTarget.Permanent(bears)),
                paymentStrategy = PaymentStrategy.AutoPay
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass() // Polymorph resolves; Fblthp's enters trigger goes on the stack
        driver.state.getBattlefield() shouldContain fblthp
        driver.state.stack.size shouldBe 1

        // In response, a spell targets Fblthp: its shuffle trigger resolves first and it leaves.
        cast(driver, me, unsummon, listOf(ChosenTarget.Permanent(fblthp)))

        driver.state.getZone(ZoneKey(me, Zone.LIBRARY)) shouldContain fblthp
        driver.getHandSize(me) shouldBe handBefore - 2 + 2
    }

    test("an ability targeting Fblthp doesn't shuffle it away") {
        val (driver, me) = setup()
        val fblthp = driver.putCreatureOnBattlefield(me, "Fblthp, the Lost")
        val fanatic = driver.putCreatureOnBattlefield(me, "Mogg Fanatic")
        val abilityId = driver.cardRegistry.getCard("Mogg Fanatic")!!.activatedAbilities.first().id

        driver.submit(
            ActivateAbility(
                playerId = me, sourceId = fanatic, abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(fblthp)),
            )
        ).error shouldBe null
        drainStack(driver)

        // The 1 damage kills it: it's in the graveyard, not shuffled into the library.
        driver.state.getZone(ZoneKey(me, Zone.GRAVEYARD)) shouldContain fblthp
        driver.state.getZone(ZoneKey(me, Zone.LIBRARY)) shouldNotContain fblthp
    }
})
