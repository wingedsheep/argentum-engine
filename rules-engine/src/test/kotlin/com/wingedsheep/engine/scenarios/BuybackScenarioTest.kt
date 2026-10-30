package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Buyback (CR 702.27a): "You may pay an additional [cost] as you cast this spell" and "If the
 * buyback cost was paid, put this spell into its owner's hand instead of into that player's
 * graveyard as it resolves."
 *
 * Buyback rides the optional-additional-cost rail ([KeywordAbility.OptionalAdditionalCost]) under
 * its own [ChoiceSlot.BOUGHT_BACK]; the only thing paying it changes is where the resolving spell
 * goes. These tests pin both halves of the rule and that a countered spell is unaffected.
 */
class BuybackScenarioTest : ScenarioTestBase() {

    private val buybackShock = card("Buyback Balm") {
        manaCost = "{R}"
        typeLine = "Instant"
        oracleText = "Buyback {2}\nYou gain 1 life."
        keywordAbility(KeywordAbility.buyback("{2}"))
        spell {
            effect = Effects.GainLife(1)
        }
    }

    // Buyback plus flashback (as Snapcaster Mage or Mystic Sanctuary would grant).
    private val buybackFlare = card("Buyback Flare") {
        manaCost = "{R}"
        typeLine = "Instant"
        oracleText = "Buyback {2}\nYou gain 1 life.\nFlashback {R}"
        keywordAbility(KeywordAbility.buyback("{2}"))
        keywordAbility(KeywordAbility.flashback("{R}"))
        spell {
            effect = Effects.GainLife(1)
        }
    }

    private val testCounterspell = card("Test Counterspell") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            target(TargetFilter.SpellOnStack)
            effect = Effects.CounterSpell()
        }
    }

    // "Whenever you cast a kicked spell" — a bought-back spell is not kicked.
    private val kickedWatcher = card("Kicked Watcher") {
        manaCost = "{W}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.WasKicked))
            effect = Effects.GainLife(3)
        }
    }

    private fun TestGame.castBuyback(declare: Boolean): ExecutionResult {
        val cardId = findCardsInHand(1, "Buyback Balm").first()
        return execute(
            CastSpell(
                playerId = player1Id,
                cardId = cardId,
                declaredCostSlot = if (declare) ChoiceSlot.BOUGHT_BACK else null,
            )
        )
    }

    private fun buybackGame(mountains: Int) = scenario()
        .withPlayers("Caster", "Opponent")
        .withCardInHand(1, "Buyback Balm")
        .withLandsOnBattlefield(1, "Mountain", mountains)
        .withCardInHand(2, "Test Counterspell")
        .withLandsOnBattlefield(2, "Island", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        cardRegistry.register(buybackShock)
        cardRegistry.register(testCounterspell)
        cardRegistry.register(kickedWatcher)
        cardRegistry.register(buybackFlare)

        test("paying buyback returns the resolved spell to its owner's hand") {
            val game = buybackGame(mountains = 3)
            val life = game.getLifeTotal(1)

            game.castBuyback(declare = true).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe life + 1
            game.isInHand(1, "Buyback Balm") shouldBe true
            game.isInGraveyard(1, "Buyback Balm") shouldBe false
            // The buyback cost was paid on top of the mana cost: all three lands are tapped.
            game.findPermanents("Mountain").all { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
        }

        test("without buyback the spell goes to the graveyard as usual") {
            val game = buybackGame(mountains = 3)

            game.castBuyback(declare = false).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Buyback Balm") shouldBe true
            game.isInHand(1, "Buyback Balm") shouldBe false
        }

        test("the buyback cost must be paid in full to declare it") {
            val game = buybackGame(mountains = 1)

            game.castBuyback(declare = true).error shouldNotBe null
            game.isInHand(1, "Buyback Balm") shouldBe true
        }

        test("the cast is offered both with and without buyback") {
            val game = buybackGame(mountains = 3)

            val casts = game.getLegalActions(1)
                .mapNotNull { it.action as? CastSpell }
                .filter { it.cardId == game.findCardsInHand(1, "Buyback Balm").first() }
            casts.map { it.declaredCostSlot } shouldContain ChoiceSlot.BOUGHT_BACK
            casts.map { it.declaredCostSlot } shouldContain null
        }

        test("a bought-back spell that is countered goes to the graveyard") {
            val game = buybackGame(mountains = 3)

            game.castBuyback(declare = true).error shouldBe null
            val spell = game.state.stack.last()
            // The opponent responds with a counterspell.
            game.passPriority()
            val counterId = game.findCardsInHand(2, "Test Counterspell").first()
            game.execute(
                CastSpell(
                    playerId = game.player2Id,
                    cardId = counterId,
                    targets = listOf(ChosenTarget.Spell(spell)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Buyback Balm") shouldBe true
            game.isInHand(1, "Buyback Balm") shouldBe false
        }

        test("a flashed-back spell is exiled even with buyback paid") {
            // CR 702.34a's "exile it instead of putting it anywhere else" still applies after
            // buyback has replaced the graveyard with the hand.
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInGraveyard(1, "Buyback Flare")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val flare = game.state.getGraveyard(game.player1Id).single()

            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = flare,
                    declaredCostSlot = ChoiceSlot.BOUGHT_BACK,
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Buyback Flare") shouldBe false
            game.isInGraveyard(1, "Buyback Flare") shouldBe false
            game.state.getExile(game.player1Id) shouldContain flare
        }

        test("a bought-back spell is not a kicked spell") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Buyback Balm")
                .withCardOnBattlefield(1, "Kicked Watcher")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val life = game.getLifeTotal(1)

            game.castBuyback(declare = true).error shouldBe null
            game.resolveStack()

            // Only the spell's own 1 life — the kicked-spell payoff stayed silent.
            game.getLifeTotal(1) shouldBe life + 1
            game.findPermanent("Kicked Watcher").shouldNotBeNull()
        }
    }
}
