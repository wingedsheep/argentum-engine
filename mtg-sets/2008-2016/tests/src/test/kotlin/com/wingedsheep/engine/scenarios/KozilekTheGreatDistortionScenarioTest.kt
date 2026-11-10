package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ogw.cards.KozilekTheGreatDistortion
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Kozilek, the Great Distortion (OGW #4).
 *
 * When you cast this spell, if you have fewer than seven cards in hand, draw cards equal to the
 * difference. Menace. Discard a card with mana value X: Counter target spell with mana value X.
 *
 * The cast trigger checks the hand with Kozilek already on the stack (the first ruling) and draws
 * up to seven. The counter's discard announces X, so its controller picks a card whose mana value
 * matches a spell on the stack and then a spell of exactly that mana value.
 */
class KozilekTheGreatDistortionScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KozilekTheGreatDistortion))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.trimHandTo(player: EntityId, size: Int) {
        getHand(player).drop(size).forEach { moveToGraveyard(it) }
    }

    fun GameTestDriver.castKozilek(player: EntityId): EntityId {
        val kozilek = putCardInHand(player, "Kozilek, the Great Distortion")
        giveColorlessMana(player, 10)
        castSpell(player, kozilek).error shouldBe null
        return kozilek
    }

    test("casting it with a short hand draws up to seven before Kozilek resolves") {
        val d = createDriver()
        val you = d.activePlayer!!
        d.trimHandTo(you, 2)

        val kozilek = d.castKozilek(you)
        // Kozilek is on the stack, not in hand: two cards in hand, so the trigger draws five.
        d.getHandSize(you) shouldBe 2
        d.assertStackSize(2)
        d.bothPass()

        d.getHandSize(you) shouldBe 7
        d.getTopOfStack() shouldBe kozilek
        d.bothPass()
        d.state.getZone(ZoneKey(you, Zone.BATTLEFIELD)).contains(kozilek) shouldBe true
        d.state.projectedState.hasKeyword(kozilek, Keyword.MENACE) shouldBe true
    }

    test("with seven cards in hand after casting it, the ability doesn't trigger") {
        val d = createDriver()
        val you = d.activePlayer!!
        d.getHandSize(you) shouldBe 7

        d.castKozilek(you)
        d.getHandSize(you) shouldBe 7
        d.assertStackSize(1)
    }

    test("discarding a card counters an opponent's spell of the same mana value") {
        val d = createDriver()
        val caster = d.activePlayer!!
        val you = d.getOpponent(caster)
        val kozilek = d.putCreatureOnBattlefield(you, "Kozilek, the Great Distortion")
        val matching = d.putCardInHand(you, "Centaur Courser") // mana value 3
        val tooSmall = d.putCardInHand(you, "Savannah Lions") // mana value 1

        val spell = d.putCardInHand(caster, "Centaur Courser")
        d.giveMana(caster, Color.GREEN, 3)
        d.castSpell(caster, spell).error shouldBe null
        d.passPriority(caster)

        d.submit(ActivateAbility(you, kozilek, KozilekTheGreatDistortion.activatedAbilities.single().id))
        val discard = d.pendingDecision
        discard.shouldBeInstanceOf<SelectCardsDecision>()
        discard.options.contains(matching) shouldBe true
        discard.options.contains(tooSmall) shouldBe false
        d.submitDecision(you, CardsSelectedResponse(discard.id, listOf(matching)))

        val target = d.pendingDecision
        target.shouldBeInstanceOf<ChooseTargetsDecision>()
        target.legalTargets[0] shouldBe listOf(spell)
        d.submitDecision(you, TargetsResponse(target.id, mapOf(0 to listOf(spell))))
        d.state.getZone(ZoneKey(you, Zone.GRAVEYARD)).contains(matching) shouldBe true

        d.bothPass()
        d.state.getZone(ZoneKey(caster, Zone.GRAVEYARD)).contains(spell) shouldBe true
        d.state.getZone(ZoneKey(you, Zone.HAND)).contains(tooSmall) shouldBe true
    }
})
