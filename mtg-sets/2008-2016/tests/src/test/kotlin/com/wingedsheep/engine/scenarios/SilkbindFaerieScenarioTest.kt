package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.shm.cards.SilkbindFaerie
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Silkbind Faerie (SHM) — {2}{W/U} 1/3 Creature — Faerie Rogue
 *
 * "Flying
 *  {1}{W/U}, {Q}: Tap target creature. ({Q} is the untap symbol.)"
 *
 * Shadowmoor's untap-symbol cycle is the first *shipped* use of `Costs.Untap` (CR 107.6 / 702).
 * Until now the only coverage of the `AbilityCost.Untap` branch was the synthetic "Test Untapper"
 * in `ActivateAbilitiesAsThoughHastyTest`, so these tests are what a real card's `{Q}` rests on.
 *
 * Two properties are load-bearing and easy to get wrong:
 * - `{Q}` means "untap this permanent", so the source must already be **tapped** to pay it.
 * - CR 302.6 gates `{Q}` behind summoning sickness exactly as it gates `{T}`.
 *
 * Note what "not offered" means for a *composite* cost like this one. A bare `Costs.Untap` that
 * can't be paid is dropped from the legal-action list outright, but `ActivatedAbilityEnumerator`
 * surfaces an unpayable **composite** as a greyed-out `affordable = false` entry so the client can
 * still show the player the ability exists. The contract these tests pin is therefore the pair that
 * actually matters: no *affordable* activation, and `ActivateAbilityHandler` refusing the action.
 */
class SilkbindFaerieScenarioTest : FunSpec({

    fun List<LegalAction>.activationsOf(sourceId: EntityId): List<LegalAction> =
        filter { (it.action as? ActivateAbility)?.sourceId == sourceId }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SilkbindFaerie))
        return driver
    }

    test("an untapped Silkbind Faerie cannot pay {Q}, so the ability is unaffordable and refused") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val faerie = driver.putCreatureOnBattlefield(you, "Silkbind Faerie")
        val victim = driver.putCreatureOnBattlefield(driver.getOpponent(you), "Centaur Courser")
        repeat(2) { driver.putLandOnBattlefield(you, "Plains") }
        driver.removeSummoningSickness(faerie)

        withClue("{Q} untaps the source, so an untapped source can't pay it") {
            driver.legalActions(you).activationsOf(faerie).count { it.affordable } shouldBe 0
        }
        driver.submit(
            ActivateAbility(
                playerId = you,
                sourceId = faerie,
                abilityId = SilkbindFaerie.script.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Permanent(victim))
            )
        ).isSuccess shouldBe false
    }

    test("a tapped, non-sick Silkbind Faerie untaps itself and taps the target") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        val faerie = driver.putCreatureOnBattlefield(you, "Silkbind Faerie")
        val victim = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        repeat(2) { driver.putLandOnBattlefield(you, "Plains") }
        driver.removeSummoningSickness(faerie)
        driver.tapPermanent(faerie)

        val offered = driver.legalActions(you).activationsOf(faerie)
        withClue("a tapped, non-sick source should have its {Q} ability enumerated") {
            offered.size shouldBe 1
        }

        driver.submit(
            ActivateAbility(
                playerId = you,
                sourceId = faerie,
                abilityId = SilkbindFaerie.script.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Permanent(victim))
            )
        ).isSuccess shouldBe true

        withClue("paying {Q} untaps the source as part of the cost") {
            driver.state.getEntity(faerie)!!.has<TappedComponent>() shouldBe false
        }

        driver.bothPass()
        withClue("the ability resolves and taps its target") {
            driver.state.getEntity(victim)!!.has<TappedComponent>() shouldBe true
        }
    }

    test("a summoning-sick Silkbind Faerie's {Q} ability is gated like {T} (CR 302.6)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val faerie = driver.putCreatureOnBattlefield(you, "Silkbind Faerie")
        val victim = driver.putCreatureOnBattlefield(driver.getOpponent(you), "Centaur Courser")
        repeat(2) { driver.putLandOnBattlefield(you, "Plains") }
        driver.tapPermanent(faerie) // tapped, so the only thing left blocking it is sickness

        withClue("a summoning-sick creature's {Q} ability must not be affordable") {
            driver.legalActions(you).activationsOf(faerie).count { it.affordable } shouldBe 0
        }
        driver.submitExpectFailure(
            ActivateAbility(
                playerId = you,
                sourceId = faerie,
                abilityId = SilkbindFaerie.script.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Permanent(victim))
            )
        ).error shouldBe "This creature has summoning sickness"
    }
})
