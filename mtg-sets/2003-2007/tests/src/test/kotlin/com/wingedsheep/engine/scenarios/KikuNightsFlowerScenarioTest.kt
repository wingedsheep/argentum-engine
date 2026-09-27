package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.KikuNightsFlower
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Kiku, Night's Flower (CHK #121) — "{2}{B}{B}, {T}: Target creature deals damage to itself
 * equal to its power."
 */
class KikuNightsFlowerScenarioTest : FunSpec({

    val kikuAbility = KikuNightsFlower.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + KikuNightsFlower)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.kikuWithMana(): EntityId {
        repeat(4) { putLandOnBattlefield(player1, "Swamp") }
        return putCreatureOnBattlefield(player1, "Kiku, Night's Flower").also { removeSummoningSickness(it) }
    }

    fun GameTestDriver.fire(source: EntityId, target: EntityId) {
        submit(
            ActivateAbility(player1, source, kikuAbility, targets = listOf(ChosenTarget.Permanent(target)))
        ).outcome shouldBe Outcome.Done
        passPriority(player2)
        bothPass()
    }

    test("the target deals damage equal to its power to itself and dies") {
        val d = driver()
        val kiku = d.kikuWithMana()
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser") // 3/3

        d.fire(kiku, courser)

        withClue("a 3/3 dealt 3 damage to itself") {
            d.findPermanent(d.player2, "Centaur Courser").shouldBeNull()
            d.getGraveyardCardNames(d.player2) shouldBe listOf("Centaur Courser")
        }
        withClue("the tap cost was paid") {
            d.isTapped(kiku) shouldBe true
        }
        withClue("Kiku herself took no damage") {
            (d.state.getEntity(kiku)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
            d.findPermanent(d.player1, "Kiku, Night's Flower").shouldNotBeNull()
        }
    }

    test("a 5/5 deals 5 to itself; the damage is the target's power, not Kiku's") {
        val d = driver()
        val kiku = d.kikuWithMana()
        d.putCreatureOnBattlefield(d.player2, "Force of Nature") // 5/5
        val force = d.findPermanent(d.player2, "Force of Nature")!!

        d.fire(kiku, force)

        d.findPermanent(d.player2, "Force of Nature").shouldBeNull()
    }

    test("Kiku can target herself and dies to her own 1 power") {
        val d = driver()
        val kiku = d.kikuWithMana()

        d.fire(kiku, kiku)

        d.findPermanent(d.player1, "Kiku, Night's Flower").shouldBeNull()
        d.getGraveyardCardNames(d.player1) shouldBe listOf("Kiku, Night's Flower")
    }
})
