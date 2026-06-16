package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.ARealmReborn
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A Realm Reborn — {4}{G}{G} Enchantment
 * "Other permanents you control have '{T}: Add one mana of any color.'"
 *
 * Verifies the grant lands on another permanent you control (it can tap for any color) and
 * that the enchantment itself does NOT receive the ability (excludeSelf = true).
 */
class ARealmRebornTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ARealmReborn)
        return driver
    }

    test("another permanent you control gains tap-for-any-color; the enchantment itself does not") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val realm = driver.putPermanentOnBattlefield(p1, "A Realm Reborn")
        // Another permanent under p1's control. A vanilla creature works; clear sickness so {T} is usable.
        val bears = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(bears)

        val grantedAbility = ARealmReborn.staticAbilities
            .filterIsInstance<GrantActivatedAbility>()
            .first()
            .ability
            .id

        // The grant goes to OTHER permanents you control — the enchantment is excluded.
        driver.submit(
            ActivateAbility(playerId = p1, sourceId = realm, abilityId = grantedAbility)
        ).isSuccess shouldBe false

        // Activate the granted "{T}: Add one mana of any color." on Grizzly Bears, choosing red.
        val result = driver.submit(
            ActivateAbility(playerId = p1, sourceId = bears, abilityId = grantedAbility)
        )
        withClue("error=${result.error} isPaused=${result.isPaused}") {
            result.isPaused shouldBe true
        }

        // "Add one mana of any color" prompts for the color.
        val decision = result.pendingDecision
        if (decision is ChooseColorDecision) {
            driver.submitDecision(p1, ColorChosenResponse(decision.id, Color.RED))
        }

        val pool = driver.state.getEntity(p1)?.get<ManaPoolComponent>()!!
        pool.red shouldBe 1
    }
})
