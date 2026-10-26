package com.wingedsheep.engine.mana

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.mana.ManaStaticsIndex
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blb.BloomburrowSet
import com.wingedsheep.mtg.sets.definitions.inv.InvasionSet
import com.wingedsheep.mtg.sets.definitions.khm.KaldheimSet
import com.wingedsheep.mtg.sets.definitions.vow.InnistradCrimsonVowSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * [ManaStaticsIndex] replaced five per-source battlefield scans inside `ManaSolver` with one walk.
 * These pin what each bucket collects, so a later edit cannot quietly drop one — the symptom would
 * be a mana source silently losing a granted ability or a bonus, which no compiler catches.
 *
 * The behavioural consequences (Clement's granted mana, Fertile Ground's extra mana, Shimmerwilds
 * Growth's colour override) already have their own scenario tests; these assert the *collection*.
 */
class ManaStaticsIndexTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(BloomburrowSet.cards)
        driver.registerCards(InvasionSet.cards)
        driver.registerCards(KaldheimSet.cards)
        driver.registerCards(InnistradCrimsonVowSet.cards)
        return driver
    }

    fun conditions(driver: GameTestDriver): ConditionEvaluator = PredicateEvaluator(driver.cardRegistry).conditions

    fun mirrorMatch(): GameTestDriver {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("a board with no mana-relevant static indexes to EMPTY") {
        val driver = mirrorMatch()
        driver.putPermanentOnBattlefield(driver.activePlayer!!, "Forest")
        driver.putPermanentOnBattlefield(driver.activePlayer!!, "Forest")

        val index = ManaStaticsIndex.build(driver.state, driver.cardRegistry, conditions(driver))

        index.isEmpty shouldBe true
        index shouldBe ManaStaticsIndex.EMPTY
    }

    test("a battlefield-scope mana grant is collected with its granter and controller") {
        val driver = mirrorMatch()
        val player = driver.activePlayer!!
        val clement = driver.putPermanentOnBattlefield(player, "Clement, the Worrywort")

        val index = ManaStaticsIndex.build(driver.state, driver.cardRegistry, conditions(driver))

        index.manaAbilityGrantors shouldHaveSize 1
        val grantor = index.manaAbilityGrantors.single()
        grantor.granterId shouldBe clement
        grantor.granterControllerId shouldBe player
        grantor.grant.ability.isManaAbility shouldBe true
    }

    test("an AdditionalManaOnTap aura is keyed by the permanent it enchants") {
        val driver = mirrorMatch()
        val player = driver.activePlayer!!

        val forest = driver.putPermanentOnBattlefield(player, "Forest")
        val fertileGround = driver.putCardInHand(player, "Fertile Ground")
        driver.giveMana(player, Color.GREEN, 2)
        driver.castSpell(player, fertileGround, listOf(forest))
        driver.bothPass()

        // Sanity: the engine attached it where we asked.
        driver.state.getEntity(fertileGround)?.get<AttachedToComponent>()?.targetId shouldBe forest

        val index = ManaStaticsIndex.build(driver.state, driver.cardRegistry, conditions(driver))

        val bonuses = index.auraBonusManaByTarget[forest].orEmpty()
        bonuses shouldHaveSize 1
        bonuses.single().auraId shouldBe fertileGround
        // Nothing is keyed against the aura itself.
        index.auraBonusManaByTarget[fertileGround] shouldBe null
    }

    test("a conditional grant is collected only while its condition holds") {
        val driver = mirrorMatch()
        val player = driver.activePlayer!!
        val tree = driver.putPermanentOnBattlefield(player, "The World Tree")
        repeat(4) { driver.putPermanentOnBattlefield(player, "Forest") }

        withClue("five lands: The World Tree's grant is off") {
            ManaStaticsIndex.build(driver.state, driver.cardRegistry, conditions(driver))
                .manaAbilityGrantors shouldHaveSize 0
        }

        driver.putPermanentOnBattlefield(player, "Forest")
        val grantor = ManaStaticsIndex.build(driver.state, driver.cardRegistry, conditions(driver))
            .manaAbilityGrantors.single()
        grantor.granterId shouldBe tree
        grantor.granterControllerId shouldBe player
    }

    test("an enchanted-permanent mana grant is keyed to the permanent its Aura enchants") {
        val driver = mirrorMatch()
        val player = driver.activePlayer!!
        val forest = driver.putPermanentOnBattlefield(player, "Forest")
        val embrace = driver.putCardInHand(player, "Nature's Embrace")
        driver.giveMana(player, Color.GREEN, 3)
        driver.castSpell(player, embrace, listOf(forest))
        driver.bothPass()

        val grantor = ManaStaticsIndex.build(driver.state, driver.cardRegistry, conditions(driver))
            .manaAbilityGrantors.single()
        grantor.granterId shouldBe embrace
        grantor.attachedToId shouldBe forest
    }
})
