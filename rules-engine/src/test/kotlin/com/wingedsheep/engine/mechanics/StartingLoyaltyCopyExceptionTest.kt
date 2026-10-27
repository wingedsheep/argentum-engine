package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `CopyExceptions.startingLoyalty` — "create a token that's a copy of Jace, except it's not legendary
 * and its starting loyalty is 1" (Jace, Mirror Mage). The token enters with the overridden number of
 * loyalty counters instead of the printed one (CR 306.5b), and because a copy exception is a copiable
 * value (CR 707.9b), a plain copy of that token — minted, or a cast clone entering as a copy — starts at
 * the overridden loyalty too.
 */
class StartingLoyaltyCopyExceptionTest : FunSpec({

    val walker = card("Mirror Walker") {
        manaCost = "{1}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 4
    }

    // "Create a token that's a copy of target planeswalker, except it's not legendary and its
    // starting loyalty is 1."
    val mirror = card("Mirror Walker Copy") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            val pw = target(TargetFilter.Planeswalker)
            effect = Effects.CreateTokenCopyOfTarget(
                pw,
                exceptions = CopyExceptions(removedSupertypes = setOf(Supertype.LEGENDARY), startingLoyalty = 1),
            )
        }
    }

    val replicate = card("Replicate Walker") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Create a token that's a copy of target planeswalker."
        spell {
            val pw = target(TargetFilter.Planeswalker)
            effect = Effects.CreateTokenCopyOfTarget(pw)
        }
    }

    val cloner = card("Walker Cloner") {
        manaCost = "{1}"
        typeLine = "Creature — Shapeshifter"
        power = 0
        toughness = 0
        oracleText = "You may have this creature enter as a copy of a planeswalker on the battlefield."
        replacementEffect(EntersAsCopy(copyFilter = GameObjectFilter.Planeswalker))
    }

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(walker, mirror, replicate, cloner))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val caster = driver.activePlayer!!
        driver.putLandOnBattlefield(caster, "Swamp")
        driver.putLandOnBattlefield(caster, "Swamp")
        driver.putLandOnBattlefield(caster, "Swamp")
        return driver to caster
    }

    fun resolveAll(driver: GameTestDriver) {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard < 20) {
            driver.bothPass()
            guard++
        }
    }

    fun walkers(driver: GameTestDriver): List<EntityId> = driver.state.getBattlefield().filter {
        driver.state.getEntity(it)?.get<CardComponent>()?.name == "Mirror Walker"
    }

    fun loyalty(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    fun isToken(driver: GameTestDriver, id: EntityId) = driver.state.getEntity(id)?.has<TokenComponent>() == true

    test("the token copy enters with the overridden starting loyalty; the original keeps its printed one") {
        val (driver, caster) = setup()
        val spell = driver.putCardInHand(caster, "Mirror Walker")
        driver.castSpell(caster, spell).outcome shouldBe Outcome.Done
        resolveAll(driver)
        val original = walkers(driver).single()
        val mirrorSpell = driver.putCardInHand(caster, "Mirror Walker Copy")
        driver.castSpell(caster, mirrorSpell, listOf(original)).outcome shouldBe Outcome.Done
        resolveAll(driver)

        val onBattlefield = walkers(driver)
        withClue("the token isn't legendary, so the legend rule keeps both") { onBattlefield.size shouldBe 2 }
        val token = onBattlefield.single { isToken(driver, it) }
        loyalty(driver, token) shouldBe 1
        loyalty(driver, original) shouldBe 4
    }

    test("a plain copy of the token inherits the overridden starting loyalty as a copiable value") {
        val (driver, caster) = setup()
        val spell = driver.putCardInHand(caster, "Mirror Walker")
        driver.castSpell(caster, spell).outcome shouldBe Outcome.Done
        resolveAll(driver)
        val original = walkers(driver).single()
        val mirrorSpell = driver.putCardInHand(caster, "Mirror Walker Copy")
        driver.castSpell(caster, mirrorSpell, listOf(original)).outcome shouldBe Outcome.Done
        resolveAll(driver)
        val token = walkers(driver).single { isToken(driver, it) }

        val copySpell = driver.putCardInHand(caster, "Replicate Walker")
        driver.castSpell(caster, copySpell, listOf(token)).outcome shouldBe Outcome.Done
        resolveAll(driver)

        val tokens = walkers(driver).filter { isToken(driver, it) }
        tokens.size shouldBe 2
        val copyOfCopy = tokens.single { it != token }
        withClue("the copy of the copy starts at 1, not the printed 4") { loyalty(driver, copyOfCopy) shouldBe 1 }
    }

    test("a cast clone entering as a copy of the token starts at the overridden loyalty too") {
        val (driver, caster) = setup()
        val spell = driver.putCardInHand(caster, "Mirror Walker")
        driver.castSpell(caster, spell).outcome shouldBe Outcome.Done
        resolveAll(driver)
        val original = walkers(driver).single()
        val mirrorSpell = driver.putCardInHand(caster, "Mirror Walker Copy")
        driver.castSpell(caster, mirrorSpell, listOf(original)).outcome shouldBe Outcome.Done
        resolveAll(driver)
        val token = walkers(driver).single { isToken(driver, it) }

        val clone = driver.putCardInHand(caster, "Walker Cloner")
        driver.castSpell(caster, clone).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.submitCardSelection(caster, listOf(token)).error shouldBe null
        resolveAll(driver)

        withClue("the clone resolved as a nonlegendary copy of the token") {
            walkers(driver).size shouldBe 3
        }
        withClue("the clone took the token's copiable starting loyalty of 1, not the printed 4") {
            loyalty(driver, clone) shouldBe 1
        }
    }
})
