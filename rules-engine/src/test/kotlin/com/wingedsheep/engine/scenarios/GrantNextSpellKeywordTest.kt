package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.GrantNextSpellKeywordEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Engine tests for [GrantNextSpellKeywordEffect] — "the next spell you cast this turn has
 * improvise" (Archway of Innovation), a one-shot rider granting a cost-payment keyword.
 *
 * Rules pinned here:
 *  1. The next spell the controller casts has improvise (CR 702.126a): the cast is enumerated as
 *     affordable when artifacts cover the generic shortfall, and each tapped artifact pays {1}.
 *  2. Only the *next* matching spell — the cast consumes the rider, so a second spell has no
 *     improvise.
 *  3. The rider is consumed by the cast whether or not improvise was used.
 *  4. A filtered rider waits: a non-matching spell neither gets the keyword nor spends it.
 *  5. Only the controller's spells — the opponent's spells don't get the keyword.
 *  6. "This turn" — an unused rider is gone at the next turn.
 *  7. Improvise counts {X} in the total cost (CR 702.126a: "each generic mana in this spell's
 *     total cost"), and applies to a spell cast from the graveyard (flashback) as well as the hand.
 *  8. Only cost-payment keywords the engine reads are accepted at construction.
 */
class GrantNextSpellKeywordTest : ScenarioTestBase() {

    init {
        val primer = card("Improvise Primer") {
            manaCost = "{0}"
            colorIdentity = ""
            typeLine = "Sorcery"
            oracleText = "The next spell you cast this turn has improvise."
            spell { effect = Effects.GrantNextSpellKeyword(Keyword.IMPROVISE) }
        }
        val instantPrimer = card("Instant Improvise Primer") {
            manaCost = "{0}"
            colorIdentity = ""
            typeLine = "Sorcery"
            oracleText = "The next instant spell you cast this turn has improvise."
            spell { effect = Effects.GrantNextSpellKeyword(Keyword.IMPROVISE, GameObjectFilter.Instant) }
        }
        val blueprint = card("Rider Blueprint") {
            manaCost = "{4}{U}"
            colorIdentity = "U"
            typeLine = "Sorcery"
            oracleText = "You gain 5 life."
            spell { effect = Effects.GainLife(5) }
        }
        val instant = card("Rider Instant") {
            manaCost = "{3}{U}"
            colorIdentity = "U"
            typeLine = "Instant"
            oracleText = "You gain 4 life."
            spell { effect = Effects.GainLife(4) }
        }
        val xSpell = card("Rider X Spell") {
            manaCost = "{X}{U}"
            colorIdentity = "U"
            typeLine = "Sorcery"
            oracleText = "You gain X life."
            spell { effect = Effects.GainLife(DynamicAmount.XValue) }
        }
        val flashbacker = card("Rider Flashbacker") {
            manaCost = "{U}"
            colorIdentity = "U"
            typeLine = "Sorcery"
            oracleText = "You gain 3 life.\nFlashback {3}{U}"
            keywordAbility(KeywordAbility.flashback("{3}{U}"))
            spell { effect = Effects.GainLife(3) }
        }
        val trinket = card("Rider Trinket") {
            manaCost = "{1}"
            colorIdentity = ""
            typeLine = "Artifact"
            oracleText = ""
        }
        listOf(primer, instantPrimer, blueprint, instant, xSpell, flashbacker, trinket)
            .forEach { cardRegistry.register(it) }

        fun castAction(game: TestGame, player: Int, name: String, type: String = "CastSpell"): LegalActionInfo? =
            game.getLegalActions(player).firstOrNull {
                it.actionType == type && it.action is CastSpell && it.description.contains(name)
            }

        fun TestGame.castPrimer(name: String = "Improvise Primer") {
            castSpell(1, name).error shouldBe null
            resolveStack()
        }

        fun TestGame.improvise(action: LegalActionInfo, artifacts: Collection<com.wingedsheep.sdk.model.EntityId>, x: Int? = null) =
            execute(
                (action.action as CastSpell).copy(
                    xValue = x ?: (action.action as CastSpell).xValue,
                    alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = artifacts.toSet())
                )
            )

        fun board(vararg hand: String) = scenario()
            .withPlayers("P1", "P2")
            .apply { hand.forEach { withCardInHand(1, it) } }
            .withLandsOnBattlefield(1, "Island", 1)
            .withCardOnBattlefield(1, "Rider Trinket")
            .withCardOnBattlefield(1, "Rider Trinket")
            .withCardOnBattlefield(1, "Rider Trinket")
            .withCardOnBattlefield(1, "Rider Trinket")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("the next spell has improvise: artifacts pay its generic") {
            val game = board("Improvise Primer", "Rider Blueprint").build()
            withClue("without the rider one Island can't pay {4}{U}") {
                castAction(game, 1, "Rider Blueprint")?.isAffordable shouldNotBe true
            }
            game.castPrimer()
            game.state.pendingNextSpellKeywords shouldHaveSize 1

            val action = castAction(game, 1, "Rider Blueprint")!!
            action.isAffordable shouldBe true
            action.hasTapForGeneric shouldBe true

            val artifacts = game.findAllPermanents("Rider Trinket")
            val before = game.getLifeTotal(1)
            game.improvise(action, artifacts).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe before + 5
            artifacts.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
            withClue("the cast consumed the rider") { game.state.pendingNextSpellKeywords.shouldBeEmpty() }
        }

        test("only the next spell — a second spell has no improvise") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Improvise Primer")
                .withCardInHand(1, "Rider Instant")
                .withCardInHand(1, "Rider Blueprint")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardOnBattlefield(1, "Rider Trinket")
                .withCardOnBattlefield(1, "Rider Trinket")
                .withCardOnBattlefield(1, "Rider Trinket")
                .withCardOnBattlefield(1, "Rider Trinket")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castPrimer()
            // Rule 3: pay the instant with mana only — the rider is still spent.
            game.castSpell(1, "Rider Instant").error shouldBe null
            game.resolveStack()
            game.state.pendingNextSpellKeywords.shouldBeEmpty()

            val action = castAction(game, 1, "Rider Blueprint")
            withClue("one Island left; the second spell has no improvise to cover {4}") {
                action?.isAffordable shouldNotBe true
                action?.hasTapForGeneric shouldNotBe true
            }
        }

        test("a filtered rider waits for a matching spell and is not spent by others") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Instant Improvise Primer")
                .withCardInHand(1, "Rider X Spell")
                .withCardInHand(1, "Rider Instant")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardOnBattlefield(1, "Rider Trinket")
                .withCardOnBattlefield(1, "Rider Trinket")
                .withCardOnBattlefield(1, "Rider Trinket")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castPrimer("Instant Improvise Primer")

            withClue("a sorcery doesn't match 'instant' — no improvise metadata on it") {
                castAction(game, 1, "Rider X Spell")?.hasTapForGeneric shouldNotBe true
            }
            game.castXSpell(1, "Rider X Spell", 0).error shouldBe null
            game.resolveStack()
            game.state.pendingNextSpellKeywords shouldHaveSize 1

            val action = castAction(game, 1, "Rider Instant")!!
            action.isAffordable shouldBe true
            game.improvise(action, game.findAllPermanents("Rider Trinket")).error shouldBe null
            game.resolveStack()
            game.state.pendingNextSpellKeywords.shouldBeEmpty()
        }

        test("the opponent's spells don't get the keyword") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Improvise Primer")
                .withCardInHand(2, "Rider Instant")
                .withLandsOnBattlefield(2, "Island", 1)
                .withCardOnBattlefield(2, "Rider Trinket")
                .withCardOnBattlefield(2, "Rider Trinket")
                .withCardOnBattlefield(2, "Rider Trinket")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Improvise Primer").error shouldBe null
            game.passPriority() // P2 gets priority with the primer on the stack
            withClue("P1's rider never applies to P2's instant") {
                castAction(game, 2, "Rider Instant")?.hasTapForGeneric shouldNotBe true
            }
        }

        test("an unused rider ends with the turn") {
            val game = board("Improvise Primer").build()
            game.castPrimer()
            game.state.pendingNextSpellKeywords shouldHaveSize 1
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.pendingNextSpellKeywords.shouldBeEmpty()
        }

        test("improvise pays the {X} in the total cost") {
            val game = board("Improvise Primer", "Rider X Spell").build()
            game.castPrimer()
            val action = castAction(game, 1, "Rider X Spell")!!
            action.hasTapForGeneric shouldBe true
            withClue("the X ceiling counts the four artifacts on top of the Island's {U}") {
                action.maxAffordableX shouldBe 4
            }
            val artifacts = game.findAllPermanents("Rider Trinket")
            val before = game.getLifeTotal(1)
            withClue("X=4 is paid by four artifacts; the Island pays {U}") {
                game.improvise(action, artifacts, x = 4).error shouldBe null
            }
            game.resolveStack()
            game.getLifeTotal(1) shouldBe before + 4
            artifacts.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
        }

        test("a spell cast from the graveyard with flashback gets improvise") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Improvise Primer")
                .withCardInGraveyard(1, "Rider Flashbacker")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardOnBattlefield(1, "Rider Trinket")
                .withCardOnBattlefield(1, "Rider Trinket")
                .withCardOnBattlefield(1, "Rider Trinket")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castPrimer()
            val action = castAction(game, 1, "Rider Flashbacker", type = "CastWithFlashback")!!
            withClue("one Island can't pay {3}{U} alone — the artifacts make the flashback affordable") {
                action.hasTapForGeneric shouldBe true
                action.isAffordable shouldBe true
            }
            val artifacts = game.findAllPermanents("Rider Trinket")
            val before = game.getLifeTotal(1)
            game.improvise(action, artifacts).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe before + 3
            withClue("the three artifacts paid {3} and the Island paid {U}") {
                artifacts.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
            }
            game.state.pendingNextSpellKeywords.shouldBeEmpty()
        }

        test("only cost-payment keywords are accepted") {
            shouldThrow<IllegalArgumentException> { GrantNextSpellKeywordEffect(Keyword.FLYING) }
            GrantNextSpellKeywordEffect(Keyword.CONVOKE).keyword shouldBe Keyword.CONVOKE
        }
    }
}
