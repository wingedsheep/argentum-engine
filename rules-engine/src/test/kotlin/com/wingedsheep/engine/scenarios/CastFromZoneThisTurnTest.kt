package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.CastSpellRecord
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blb.cards.BonebindOrator
import com.wingedsheep.mtg.sets.definitions.blb.cards.OsteomancerAdept
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.PlayerCastSpellsThisTurn
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Cast-zone qualifier on the spell-cast tracker (OTJ engine gap #4).
 *
 * Each [CastSpellRecord] now captures the zone the spell was cast from (`castFromZone`), so a count
 * or intervening-if can ask specifically about spells cast *from your hand* — the Prairie Dog cycle's
 * "if you haven't cast a spell from your hand this turn" (Inventive Wingsmith, Prairie Dog, Canyon
 * Crab, Emergent Haunting, Wrangler of the Damned). A hand cast must be distinguished from a
 * flashback/forage (GRAVEYARD), plot/foretell (EXILE), or commander (COMMAND) cast.
 *
 * The first group drives the real cast pipeline to prove the engine stamps the origin zone correctly;
 * the second pins the condition's zone-aware counting (incl. the face-down/morph edge); the third
 * does the same for the symmetric [DynamicAmount.SpellsCastThisTurn] read surface.
 */
class CastFromZoneThisTurnTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(OsteomancerAdept, BonebindOrator, PredefinedTokens.Food))
        return driver
    }

    fun lastRecord(driver: GameTestDriver, player: EntityId): CastSpellRecord =
        driver.state.spellsCastThisTurnByPlayer[player]!!.last()

    // =========================================================================
    // Engine populates castFromZone from the real origin zone
    // =========================================================================

    test("a spell cast from hand records castFromZone = HAND") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val you = driver.activePlayer!!
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(you)).isSuccess shouldBe true

        lastRecord(driver, you).castFromZone shouldBe Zone.HAND
    }

    test("a spell cast from the graveyard (forage) records castFromZone = GRAVEYARD, not HAND") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20))
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val adept = driver.putCreatureOnBattlefield(you, "Osteomancer Adept")
        driver.removeSummoningSickness(adept)
        driver.putPermanentOnBattlefield(you, "Food") // forage cost
        val orator = driver.putCardInGraveyard(you, "Bonebind Orator")

        val abilityId = OsteomancerAdept.activatedAbilities.first().id
        driver.submitSuccess(ActivateAbility(playerId = you, sourceId = adept, abilityId = abilityId))
        driver.bothPass() // resolve the ability — grants forage-cast permission

        driver.giveMana(you, Color.BLACK, 2)
        driver.castSpell(you, orator).isSuccess shouldBe true

        lastRecord(driver, you).castFromZone shouldBe Zone.GRAVEYARD
    }

    // =========================================================================
    // Condition: PlayerCastSpellsThisTurn with fromZone (the Prairie Dog gate)
    // =========================================================================

    test("end-to-end: a hand cast satisfies fromZone=HAND; a graveyard cast does not") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20))
        val you = driver.activePlayer!!
        val opp = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val evaluator = ConditionEvaluator()
        val ctx = EffectContext(sourceId = null, controllerId = you)

        // Cast a creature from the graveyard via forage — the only cast this turn.
        val adept = driver.putCreatureOnBattlefield(you, "Osteomancer Adept")
        driver.removeSummoningSickness(adept)
        driver.putPermanentOnBattlefield(you, "Food")
        val orator = driver.putCardInGraveyard(you, "Bonebind Orator")
        val abilityId = OsteomancerAdept.activatedAbilities.first().id
        driver.submitSuccess(ActivateAbility(playerId = you, sourceId = adept, abilityId = abilityId))
        driver.bothPass()
        driver.giveMana(you, Color.BLACK, 2)
        driver.castSpell(you, orator).isSuccess shouldBe true

        // You *have* cast a spell, but not from your hand.
        evaluator.evaluate(driver.state, PlayerCastSpellsThisTurn(Player.You, atLeast = 1), ctx) shouldBe true
        evaluator.evaluate(driver.state, PlayerCastSpellsThisTurn(Player.You, atLeast = 1, fromZone = Zone.HAND), ctx) shouldBe false
        evaluator.evaluate(driver.state, PlayerCastSpellsThisTurn(Player.You, atLeast = 1, fromZone = Zone.GRAVEYARD), ctx) shouldBe true

        // Now cast a spell from hand — the hand qualifier flips true.
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(opp)).isSuccess shouldBe true
        evaluator.evaluate(driver.state, PlayerCastSpellsThisTurn(Player.You, atLeast = 1, fromZone = Zone.HAND), ctx) shouldBe true
    }

    test("condition: fromZone counts only matching-zone records, honoring atLeast and filter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.player1
        val opp = driver.player2
        val evaluator = ConditionEvaluator()
        val ctx = EffectContext(sourceId = null, controllerId = you)

        val instant = TypeLine.parse("Instant")
        val creature = TypeLine.parse("Creature")
        val state = driver.state.copy(
            spellsCastThisTurnByPlayer = mapOf(
                you to listOf(
                    CastSpellRecord(instant, 1, emptySet(), false, castFromZone = Zone.HAND),
                    CastSpellRecord(creature, 2, emptySet(), false, castFromZone = Zone.HAND),
                    CastSpellRecord(instant, 1, emptySet(), false, castFromZone = Zone.GRAVEYARD),
                )
            )
        )

        // 2 hand casts → atLeast 1 and 2 hold, 3 fails.
        evaluator.evaluate(state, PlayerCastSpellsThisTurn(Player.You, atLeast = 1, fromZone = Zone.HAND), ctx) shouldBe true
        evaluator.evaluate(state, PlayerCastSpellsThisTurn(Player.You, atLeast = 2, fromZone = Zone.HAND), ctx) shouldBe true
        evaluator.evaluate(state, PlayerCastSpellsThisTurn(Player.You, atLeast = 3, fromZone = Zone.HAND), ctx) shouldBe false
        // Only one GRAVEYARD cast.
        evaluator.evaluate(state, PlayerCastSpellsThisTurn(Player.You, atLeast = 2, fromZone = Zone.GRAVEYARD), ctx) shouldBe false
        // filter + zone combine: one instant cast from hand.
        evaluator.evaluate(
            state,
            PlayerCastSpellsThisTurn(Player.You, GameObjectFilter.Instant, atLeast = 1, fromZone = Zone.HAND),
            ctx
        ) shouldBe true
        evaluator.evaluate(
            state,
            PlayerCastSpellsThisTurn(Player.You, GameObjectFilter.Instant, atLeast = 2, fromZone = Zone.HAND),
            ctx
        ) shouldBe false
        // No zone → all three count.
        evaluator.evaluate(state, PlayerCastSpellsThisTurn(Player.You, atLeast = 3), ctx) shouldBe true
    }

    test("condition: a face-down (morph) spell cast from hand still counts toward fromZone=HAND") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.player1
        val opp = driver.player2
        val evaluator = ConditionEvaluator()
        val ctx = EffectContext(sourceId = null, controllerId = you)

        // A face-down spell has no known characteristics (CR 708.2), but it was still cast from hand.
        val state = driver.state.copy(
            spellsCastThisTurnByPlayer = mapOf(
                you to listOf(
                    CastSpellRecord(TypeLine.parse("Creature"), 0, emptySet(), isFaceDown = true, castFromZone = Zone.HAND),
                )
            )
        )
        // Any-filter zone gate counts it...
        evaluator.evaluate(state, PlayerCastSpellsThisTurn(Player.You, atLeast = 1, fromZone = Zone.HAND), ctx) shouldBe true
        // ...but a typed filter still can't match a face-down spell's hidden characteristics.
        evaluator.evaluate(
            state,
            PlayerCastSpellsThisTurn(Player.You, GameObjectFilter.Creature, atLeast = 1, fromZone = Zone.HAND),
            ctx
        ) shouldBe false
    }

    // =========================================================================
    // DynamicAmount: SpellsCastThisTurn with fromZone (symmetric read surface)
    // =========================================================================

    test("dynamicAmount: fromZone counts only matching-zone records") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.player1
        val opp = driver.player2
        val evaluator = DynamicAmountEvaluator()
        val ctx = EffectContext(sourceId = null, controllerId = you)

        val instant = TypeLine.parse("Instant")
        val state = driver.state.copy(
            spellsCastThisTurnByPlayer = mapOf(
                you to listOf(
                    CastSpellRecord(instant, 1, emptySet(), false, castFromZone = Zone.HAND),
                    CastSpellRecord(instant, 1, emptySet(), false, castFromZone = Zone.HAND),
                    CastSpellRecord(instant, 1, emptySet(), false, castFromZone = Zone.GRAVEYARD),
                    CastSpellRecord(instant, 1, emptySet(), false, castFromZone = null),
                )
            )
        )

        evaluator.evaluate(state, DynamicAmount.SpellsCastThisTurn(Player.You), ctx) shouldBe 4
        evaluator.evaluate(state, DynamicAmount.SpellsCastThisTurn(Player.You, fromZone = Zone.HAND), ctx) shouldBe 2
        evaluator.evaluate(state, DynamicAmount.SpellsCastThisTurn(Player.You, fromZone = Zone.GRAVEYARD), ctx) shouldBe 1
        evaluator.evaluate(state, DynamicAmount.SpellsCastThisTurn(Player.You, fromZone = Zone.EXILE), ctx) shouldBe 0
    }

    // =========================================================================
    // Descriptions
    // =========================================================================

    test("descriptions read with the zone qualifier") {
        PlayerCastSpellsThisTurn(Player.You, atLeast = 1, fromZone = Zone.HAND).description shouldBe
            "if you cast 1 or more spells from hand this turn"
        DynamicAmount.SpellsCastThisTurn(Player.You, fromZone = Zone.HAND).description shouldBe
            "the number of spells you've cast from hand this turn"
    }
})
