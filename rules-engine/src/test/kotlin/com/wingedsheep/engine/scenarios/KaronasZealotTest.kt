package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.scg.cards.KaronasZealot
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Karona's Zealot's CONTINUOUS damage redirection.
 *
 * Karona's Zealot: {4}{W}, Morph {3}{W}{W}
 * When this creature is turned face up, ALL damage that would be dealt to it this turn is dealt
 * to target creature instead.
 *
 * Unlike Glarecaster's one-shot "next time" shield, this redirects *every* instance of damage for
 * the rest of the turn — the shield is never used up by a single redirection (RedirectScope.CONTINUOUS).
 */
class KaronasZealotTest : FunSpec({

    // A high-toughness sink so the redirect target survives several redirected bolts.
    val damageSink = CardDefinition.creature(
        name = "Damage Sink",
        manaCost = ManaCost.parse("{4}"),
        subtypes = setOf(Subtype("Wall")),
        power = 0,
        toughness = 10,
        oracleText = ""
    )

    val allCards = TestCards.all + listOf(damageSink)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    fun GameTestDriver.putFaceDownCreature(playerId: EntityId, cardName: String): EntityId {
        val creatureId = putCreatureOnBattlefield(playerId, cardName)
        val cardDef = allCards.first { it.name == cardName }
        val morphAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Morph>().firstOrNull()
        replaceState(state.updateEntity(creatureId) { container ->
            var c = container.with(FaceDownComponent)
            if (morphAbility != null) {
                c = c.with(MorphDataComponent(morphAbility.morphCost, cardDef.name))
            }
            c
        })
        removeSummoningSickness(creatureId)
        return creatureId
    }

    test("CONTINUOUS shield redirects every separate damage instance for the rest of the turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        // Karona's Zealot face down, plus a fat redirect target (the Damage Sink).
        val zealot = driver.putFaceDownCreature(p1, "Karona's Zealot")
        val sink = driver.putCreatureOnBattlefield(p2, "Damage Sink")

        // Turn face up by paying the {3}{W}{W} morph cost from the pool.
        driver.giveMana(p1, Color.WHITE, 5)
        driver.submit(TurnFaceUp(playerId = p1, sourceId = zealot)).error shouldBe null
        driver.state.getEntity(zealot)?.get<FaceDownComponent>() shouldBe null

        // The "when turned face up" trigger targets the Damage Sink, then resolves into the shield.
        driver.submitTargetSelection(p1, listOf(sink))
        driver.bothPass()

        driver.state.floatingEffects.any {
            it.effect.modification is SerializableModification.RedirectNextDamage
        } shouldBe true

        // First bolt at the Zealot — redirected to the Sink.
        driver.giveMana(p1, Color.RED, 1)
        val bolt1 = driver.putCardInHand(p1, "Lightning Bolt")
        driver.castSpellWithTargets(p1, bolt1, listOf(ChosenTarget.Permanent(zealot)))
        driver.bothPass()

        (driver.state.getEntity(zealot)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        (driver.state.getEntity(sink)?.get<DamageComponent>()?.amount ?: 0) shouldBe 3

        // Shield is CONTINUOUS — still active after the first redirection.
        driver.state.floatingEffects.any {
            it.effect.modification is SerializableModification.RedirectNextDamage
        } shouldBe true

        // Second, separate bolt at the Zealot — ALSO redirected (the old one-shot behavior would let
        // this one through).
        driver.giveMana(p1, Color.RED, 1)
        val bolt2 = driver.putCardInHand(p1, "Lightning Bolt")
        driver.castSpellWithTargets(p1, bolt2, listOf(ChosenTarget.Permanent(zealot)))
        driver.bothPass()

        (driver.state.getEntity(zealot)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        (driver.state.getEntity(sink)?.get<DamageComponent>()?.amount ?: 0) shouldBe 6
    }
})
