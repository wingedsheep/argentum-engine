package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CategoryPickPurpose
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `ChooseOnePerCategoryEffect.distinctPicks` — the overlapping-category edict ("each player
 * sacrifices an artifact, a creature, …", Release): a permanent answers one category only, and every
 * prompt offers just the candidates that keep the largest number of distinct picks reachable, so a
 * chooser can never strand a later category by spending its only candidate early.
 */
class ChooseOnePerCategoryDistinctPicksTest : FunSpec({

    val artifactCreature = card("Distinct Test Golem") {
        manaCost = "{2}"; typeLine = "Artifact Creature — Golem"; power = 2; toughness = 2
    }
    val bear = card("Distinct Test Bear") {
        manaCost = "{1}{G}"; typeLine = "Creature — Bear"; power = 2; toughness = 2
    }
    val trinket = card("Distinct Test Trinket") { manaCost = "{1}"; typeLine = "Artifact" }

    fun edict(distinct: Boolean) = card("Distinct Test Edict ${if (distinct) "On" else "Off"}") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val pool = gather(GameObjectFilter.Permanent.opponentControls())
                val doomed = chooseOnePerCategory(
                    pool,
                    listOf(GameObjectFilter.Artifact, GameObjectFilter.Creature),
                    purpose = CategoryPickPurpose.SACRIFICE,
                    distinctPicks = distinct,
                )
                sacrifice(doomed)
            }
        }
    }
    val distinctEdict = edict(true)
    val plainEdict = edict(false)

    /** A game where the opponent controls [names]; returns their ids in order. */
    fun setUp(driver: GameTestDriver, spell: String, vararg names: String): List<EntityId> {
        driver.registerCards(TestCards.all + listOf(artifactCreature, bear, trinket, distinctEdict, plainEdict))
        driver.initMirrorMatch(Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val ids = names.map { driver.putPermanentOnBattlefield(driver.player2, it) }
        val card = driver.putCardInHand(driver.player1, spell)
        driver.giveMana(driver.player1, Color.BLACK)
        driver.castSpell(driver.player1, card).error.shouldBeNull()
        driver.passPriority(driver.player1)
        driver.passPriority(driver.player2)
        return ids
    }

    test("an artifact creature beside a nonartifact creature: both are sacrificed, no question asked") {
        val driver = GameTestDriver()
        val (golem, bearId) = setUp(driver, distinctEdict.name, artifactCreature.name, bear.name)
        driver.state.pendingDecision.shouldBeNull()
        driver.getGraveyard(driver.player2) shouldContainExactlyInAnyOrder listOf(golem, bearId)
    }

    test("a lone artifact creature answers only one category") {
        val driver = GameTestDriver()
        val (golem) = setUp(driver, distinctEdict.name, artifactCreature.name)
        driver.state.pendingDecision.shouldBeNull()
        driver.getGraveyard(driver.player2) shouldContainExactlyInAnyOrder listOf(golem)
    }

    test("the only creature is never offered as the artifact, so both picks land") {
        val driver = GameTestDriver()
        val (golem, trinketId) = setUp(driver, distinctEdict.name, artifactCreature.name, trinket.name)
        driver.state.pendingDecision.shouldBeNull()
        driver.getGraveyard(driver.player2) shouldContainExactlyInAnyOrder listOf(golem, trinketId)
    }

    test("without distinctPicks the same board offers both artifacts and one permanent may cover both") {
        val driver = GameTestDriver()
        val (golem, trinketId) = setUp(driver, plainEdict.name, artifactCreature.name, trinket.name)
        val decision = driver.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContainExactlyInAnyOrder listOf(golem, trinketId)
        driver.submitCardSelection(driver.player2, listOf(golem)).error.shouldBeNull()
        driver.state.pendingDecision.shouldBeNull()
        driver.getGraveyard(driver.player2) shouldContainExactlyInAnyOrder listOf(golem)
    }

    test("two artifact creatures: a real choice for the artifact, and the other one becomes the creature") {
        val driver = GameTestDriver()
        val golems = setUp(driver, distinctEdict.name, artifactCreature.name, artifactCreature.name)
        val decision = driver.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe driver.player2
        decision.options shouldContainExactlyInAnyOrder golems
        driver.submitCardSelection(driver.player2, listOf(golems.first())).error.shouldBeNull()
        driver.state.pendingDecision.shouldBeNull()
        driver.getGraveyard(driver.player2) shouldContainExactlyInAnyOrder golems
    }
})
