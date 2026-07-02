package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.ZhaoTheMoonSlayer
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.dsl.basicLand
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Zhao, the Moon Slayer ({1}{R} Legendary Creature — Human Soldier, 2/2):
 *   - Menace (printed keyword).
 *   - "Nonbasic lands enter tapped." — a global [com.wingedsheep.sdk.scripting.PermanentsEnterTapped]
 *     replacement effect (affects every player's nonbasic lands; basics are unaffected).
 *   - "{7}: Put a conqueror counter on Zhao." — accumulates generic conqueror counters.
 *   - "As long as Zhao has a conqueror counter on him, nonbasic lands are Mountains." — a
 *     [com.wingedsheep.sdk.scripting.SetLandTypesForGroup] over all nonbasic lands, gated by the
 *     conqueror counter (CR 305.7: lose other land types + abilities, gain the Mountain mana
 *     ability). Basics stay themselves.
 */
class ZhaoTheMoonSlayerScenarioTest : FunSpec({

    // A nonbasic dual land (Island + Forest, no "basic" supertype). Normally enters untapped and
    // taps for {U}/{G} via its intrinsic subtype mana abilities.
    val TropicalIsland = CardDefinition(
        name = "Tropical Island",
        manaCost = ManaCost.ZERO,
        typeLine = TypeLine(
            cardTypes = setOf(CardType.LAND),
            subtypes = setOf(Subtype("Island"), Subtype("Forest")),
        ),
        script = CardScript(),
    )
    val TestForest = basicLand("Forest") {}

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ZhaoTheMoonSlayer, TropicalIsland, TestForest))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun GameTestDriver.pool(playerId: EntityId): ManaPoolComponent =
        state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 50) {
            val pd = state.pendingDecision
            if (pd != null) autoResolveDecision() else bothPass()
        }
    }

    fun GameTestDriver.conquerorCounters(zhao: EntityId): Int =
        state.getEntity(zhao)?.get<CountersComponent>()?.getCount(CounterType.CONQUEROR) ?: 0

    // ── Menace ─────────────────────────────────────────────────────────────────────────

    test("Zhao has menace") {
        val (driver, me) = newGame()
        val zhao = driver.putPermanentOnBattlefield(me, "Zhao, the Moon Slayer")
        driver.state.projectedState.hasKeyword(zhao, Keyword.MENACE).shouldBeTrue()
    }

    // ── "Nonbasic lands enter tapped" ────────────────────────────────────────────────────

    test("a nonbasic land played from hand enters tapped while Zhao is in play") {
        val (driver, me) = newGame()
        driver.putPermanentOnBattlefield(me, "Zhao, the Moon Slayer")

        val land = driver.putCardInHand(me, "Tropical Island")
        driver.playLand(me, land).isSuccess shouldBe true
        driver.state.getEntity(land)?.has<TappedComponent>() shouldBe true
    }

    test("a BASIC land played from hand still enters untapped while Zhao is in play") {
        val (driver, me) = newGame()
        driver.putPermanentOnBattlefield(me, "Zhao, the Moon Slayer")

        val forest = driver.putCardInHand(me, "Forest")
        driver.playLand(me, forest).isSuccess shouldBe true
        driver.state.getEntity(forest)?.has<TappedComponent>() shouldBe false
    }

    test("baseline: a nonbasic land enters untapped without Zhao") {
        val (driver, me) = newGame()
        val land = driver.putCardInHand(me, "Tropical Island")
        driver.playLand(me, land).isSuccess shouldBe true
        driver.state.getEntity(land)?.has<TappedComponent>() shouldBe false
    }

    // ── "{7}: Put a conqueror counter on Zhao" ──────────────────────────────────────────

    test("{7} activated ability adds a conqueror counter") {
        val (driver, me) = newGame()
        val zhao = driver.putPermanentOnBattlefield(me, "Zhao, the Moon Slayer")
        driver.conquerorCounters(zhao) shouldBe 0

        driver.giveColorlessMana(me, 7)
        val abilityId = ZhaoTheMoonSlayer.activatedAbilities.first().id
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = zhao, abilityId = abilityId))
        driver.resolveAll()

        driver.conquerorCounters(zhao) shouldBe 1
    }

    // ── "Nonbasic lands are Mountains" while Zhao has a conqueror counter ────────────────

    test("with a conqueror counter, a nonbasic land is a Mountain (only) that taps for {R}; a basic Forest is unaffected") {
        val (driver, me) = newGame()
        val zhao = driver.putPermanentOnBattlefield(me, "Zhao, the Moon Slayer")
        val island = driver.putLandOnBattlefield(me, "Tropical Island")
        val forest = driver.putLandOnBattlefield(me, "Forest")

        // No counter yet: the nonbasic land keeps its own identity.
        driver.state.projectedState.hasSubtype(island, "Island").shouldBeTrue()
        driver.state.projectedState.hasSubtype(island, "Mountain") shouldBe false

        // {7}: add a conqueror counter → the group land-type override switches on.
        driver.giveColorlessMana(me, 7)
        val abilityId = ZhaoTheMoonSlayer.activatedAbilities.first().id
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = zhao, abilityId = abilityId))
        driver.resolveAll()
        driver.conquerorCounters(zhao) shouldBe 1

        // The nonbasic land is now a Mountain, having lost its other land subtypes.
        driver.state.projectedState.hasSubtype(island, "Mountain").shouldBeTrue()
        driver.state.projectedState.hasSubtype(island, "Island") shouldBe false
        driver.state.projectedState.hasSubtype(island, "Forest") shouldBe false

        // The basic Forest is untouched.
        driver.state.projectedState.hasSubtype(forest, "Forest").shouldBeTrue()
        driver.state.projectedState.hasSubtype(forest, "Mountain") shouldBe false

        // The Mountain-ified land taps for {R} (intrinsic Mountain mana ability).
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = island, abilityId = AbilityId.intrinsicMana('R')))
        driver.pool(me).red shouldBe 1
    }

    test("regression: the Mountain-ified land's {R} intrinsic ability is OFFERED in legal actions (client can tap it)") {
        // The bug: SetLandTypesForGroup marks the land lost-all-abilities (Layer 6), which
        // suppressed the intrinsic Mountain mana ability in the legal-action enumerator — so
        // the client never showed a tap option even though the handler would accept it. The
        // effect-SET basic land type must keep its intrinsic mana ability (CR 305.7).
        val (driver, me) = newGame()
        val zhao = driver.putPermanentOnBattlefield(me, "Zhao, the Moon Slayer")
        val island = driver.putLandOnBattlefield(me, "Tropical Island")

        driver.giveColorlessMana(me, 7)
        val abilityId = ZhaoTheMoonSlayer.activatedAbilities.first().id
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = zhao, abilityId = abilityId))
        driver.resolveAll()
        driver.conquerorCounters(zhao) shouldBe 1

        val landActions = driver.legalActions(me).filter {
            val a = it.action
            a is ActivateAbility && a.sourceId == island
        }
        // Exactly the Mountain {R} intrinsic ability is offered — its old Island/Forest
        // abilities were stripped, and it is not silently dropped.
        landActions.map { (it.action as ActivateAbility).abilityId }
            .shouldContainExactly(AbilityId.intrinsicMana('R'))
    }

    test("without a conqueror counter, the nonbasic land keeps its identity and taps for its own color") {
        val (driver, me) = newGame()
        driver.putPermanentOnBattlefield(me, "Zhao, the Moon Slayer")
        val island = driver.putLandOnBattlefield(me, "Tropical Island")

        driver.state.projectedState.hasSubtype(island, "Mountain") shouldBe false
        driver.state.projectedState.hasSubtype(island, "Forest").shouldBeTrue()

        // Taps for {G} (Forest subtype), not {R}.
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = island, abilityId = AbilityId.intrinsicMana('G')))
        driver.pool(me).green shouldBe 1
        driver.pool(me).red shouldBe 0
    }
})
