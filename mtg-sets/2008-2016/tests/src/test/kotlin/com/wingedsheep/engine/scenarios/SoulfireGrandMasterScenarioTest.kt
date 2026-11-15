package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.SoulfireGrandMaster
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Soulfire Grand Master (FRF) — {1}{W} 2/2 Human Monk. "Lifelink. Instant and sorcery spells you
 * control have lifelink. {2}{U/R}{U/R}: The next time you cast an instant or sorcery spell from your
 * hand this turn, put that card into your hand instead of into your graveyard as it resolves."
 *
 * The rider's rules (countered, flashback, self-exile, turn end) are pinned by the engine's
 * `GrantNextSpellResolveDestinationTest`; this covers the card's wiring: the spell lifelink grant,
 * the hybrid activation cost, and a burn spell that comes back to hand.
 */
class SoulfireGrandMasterScenarioTest : FunSpec({

    fun setUp(): Triple<GameTestDriver, EntityId, EntityId> {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + SoulfireGrandMaster)
            initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        }
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val monk = d.putCreatureOnBattlefield(me, "Soulfire Grand Master")
        return Triple(d, me, monk)
    }

    fun resolveAll(d: GameTestDriver) {
        var guard = 0
        while (d.stackSize > 0 && guard++ < 10) d.bothPass()
    }

    test("Lightning Bolt you control has lifelink") {
        val (d, me, _) = setUp()
        val opponent = d.getOpponent(me)
        val bolt = d.putCardInHand(me, "Lightning Bolt")
        d.giveMana(me, Color.RED, 1)
        d.castSpell(me, bolt, listOf(opponent)).error shouldBe null
        resolveAll(d)

        d.getLifeTotal(opponent) shouldBe 17
        withClue("lifelink on the spell") { d.getLifeTotal(me) shouldBe 23 }
        withClue("no ability activated, so it goes to the graveyard") {
            d.getGraveyard(me).contains(bolt) shouldBe true
        }
    }

    test("the activated ability returns the next instant cast from hand to hand as it resolves") {
        val (d, me, monk) = setUp()
        val opponent = d.getOpponent(me)
        val abilityId = SoulfireGrandMaster.activatedAbilities.first().id

        d.giveMana(me, Color.RED, 3)
        withClue("{2}{U/R}{U/R} is not payable with three mana") {
            d.submit(ActivateAbility(me, monk, abilityId)).error shouldNotBe null
        }
        d.giveMana(me, Color.BLUE, 1)
        d.submitSuccess(ActivateAbility(me, monk, abilityId))
        resolveAll(d)

        val bolt = d.putCardInHand(me, "Lightning Bolt")
        d.giveMana(me, Color.RED, 1)
        d.castSpell(me, bolt, listOf(opponent)).error shouldBe null
        resolveAll(d)

        d.getLifeTotal(opponent) shouldBe 17
        withClue("back in hand") {
            d.getHand(me).contains(bolt) shouldBe true
            d.getGraveyard(me).contains(bolt) shouldBe false
        }

        d.giveMana(me, Color.RED, 1)
        d.castSpell(me, bolt, listOf(opponent)).error shouldBe null
        resolveAll(d)
        withClue("the rider was one-shot") { d.getGraveyard(me).contains(bolt) shouldBe true }
    }
})
