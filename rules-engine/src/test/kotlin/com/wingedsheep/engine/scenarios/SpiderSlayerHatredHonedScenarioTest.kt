package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.SpiderSlayerHatredHoned
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Spider-Slayer, Hatred Honed (SPM) — "Whenever Spider-Slayer deals damage to a Spider, destroy
 * that creature." Pins the `RecipientFilter.Matching` deals-damage trigger now wired in
 * `TriggerMatcher.matchesDealsDamageTrigger` (the same shape as East-Mark Cavalier / Mauhur).
 *
 * The blocker is a 1/3 so it survives the 2 combat damage on its own — proving it is the *trigger*
 * (not lethal combat damage) that destroys a Spider, and that a non-Spider is left alone.
 */
class SpiderSlayerHatredHonedScenarioTest : FunSpec({

    val slayerActivatedAbilityId = SpiderSlayerHatredHoned.activatedAbilities.single().id

    // 0 power so Spider-Slayer (a 2/1) survives the block — isolating the destroy *trigger* from
    // lethal combat damage in either direction.
    val testSpider = card("Test Spider Blocker") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Spider"
        power = 0
        toughness = 3
    }
    val testBear = card("Test Bear Blocker") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 1
        toughness = 3
    }

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(testSpider, testBear))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }
        return Triple(driver, you, opponent)
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    test("destroys a Spider it deals combat damage to (even though the Spider survives the damage)") {
        val (driver, you, opponent) = newGame()
        val slayer = driver.putCreatureOnBattlefield(you, "Spider-Slayer, Hatred Honed")
        driver.removeSummoningSickness(slayer)
        val spider = driver.putCreatureOnBattlefield(opponent, "Test Spider Blocker") // 1/3

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(slayer), defendingPlayer = opponent).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(spider to listOf(slayer)))
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        resolveStack(driver)

        // The Spider took only 2 (survives a 3-toughness body on its own), but the trigger destroys it.
        driver.state.getBattlefield().contains(spider) shouldBe false
        // Spider-Slayer took 1 and survives.
        driver.state.getBattlefield().contains(slayer) shouldBe true
    }

    test("does not destroy a non-Spider it deals combat damage to") {
        val (driver, you, opponent) = newGame()
        val slayer = driver.putCreatureOnBattlefield(you, "Spider-Slayer, Hatred Honed")
        driver.removeSummoningSickness(slayer)
        val bear = driver.putCreatureOnBattlefield(opponent, "Test Bear Blocker") // 1/3, not a Spider

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(slayer), defendingPlayer = opponent).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(bear to listOf(slayer)))
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        resolveStack(driver)

        // Non-Spider: no destroy trigger; the 1/3 survives the 2 combat damage.
        driver.state.getBattlefield().contains(bear) shouldBe true
    }

    test("{6}, exile from graveyard: creates two tapped 1/1 flying Robot artifact tokens") {
        val (driver, you, _) = newGame()
        val slayer = driver.putCardInGraveyard(you, "Spider-Slayer, Hatred Honed")
        driver.giveColorlessMana(you, 6)

        driver.submit(
            ActivateAbility(playerId = you, sourceId = slayer, abilityId = slayerActivatedAbilityId),
        ).isSuccess shouldBe true
        driver.bothPass()
        resolveStack(driver)

        // Exile is part of the cost — the card leaves the graveyard for exile.
        driver.getExile(you).contains(slayer) shouldBe true
        driver.state.getBattlefield().contains(slayer) shouldBe false

        val robots = driver.state.getBattlefield().filter { id ->
            driver.state.getEntity(id)?.get<CardComponent>()?.name == "Robot Token" &&
                driver.getController(id) == you
        }
        robots.size shouldBe 2
        robots.forEach { r ->
            driver.state.getEntity(r)?.has<TappedComponent>() shouldBe true
            driver.state.projectedState.getPower(r) shouldBe 1
            driver.state.projectedState.getToughness(r) shouldBe 1
            driver.state.projectedState.hasKeyword(r, Keyword.FLYING) shouldBe true
        }
    }
})
