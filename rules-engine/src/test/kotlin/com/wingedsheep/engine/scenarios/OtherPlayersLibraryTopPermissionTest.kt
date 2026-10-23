package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayFromTopWithAlternativeCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The `libraryOwner` axis of [LookAtTopOfLibrary] and [PlayFromTopWithAlternativeCost]: a
 * library-top permission that opens *another* player's library (Xanathar, Guild Kingpin).
 *
 * Pins that the target is fixed as the grant resolves, that a scoped grant opens only the named
 * library (and a printed permission only its holder's own), and that the "as though it were mana
 * of any color" rider relaxes colored pips but not a `{C}` pip (CR 609.4b).
 */
class OtherPlayersLibraryTopPermissionTest : FunSpec({

    val peek = card("Test Library Peek") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val opponent = target(Targets.Opponent)
            effect = Effects.GrantStaticAbility(LookAtTopOfLibrary(libraryOwner = opponent), EffectTarget.Controller) then
                Effects.GrantStaticAbility(
                    PlayFromTopWithAlternativeCost(libraryOwner = opponent, withAnyManaColor = true),
                    EffectTarget.Controller,
                )
        }
    }

    val lens = card("Test Lens") {
        manaCost = "{0}"
        typeLine = "Artifact"
        staticAbility { ability = LookAtTopOfLibrary() }
    }

    val colorlessPipBeast = CardDefinition(
        name = "Colorless Pip Beast",
        manaCost = ManaCost.parse("{C}{G}"),
        typeLine = TypeLine.parse("Creature — Beast"),
        oracleText = "",
        creatureStats = CreatureStats(2, 2),
    )

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(peek, lens, colorlessPipBeast))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        return Triple(driver, you, driver.getOpponent(you))
    }

    fun castPeek(driver: GameTestDriver, you: EntityId, opponent: EntityId) {
        val spell = driver.putCardInHand(you, "Test Library Peek")
        driver.castSpell(you, spell, listOf(opponent)).error shouldBe null
        driver.bothPass()
    }

    fun visibleTo(driver: GameTestDriver, viewer: EntityId, cardId: EntityId): Boolean =
        cardId in ClientStateTransformer(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
            .transform(driver.state, viewingPlayerId = viewer).cards

    fun libraryCasts(driver: GameTestDriver, playerId: EntityId) =
        driver.legalActions(playerId).filter { it.sourceZone == "LIBRARY" }.mapNotNull { (it.action as? CastSpell)?.cardId }

    test("the granted permission is pinned to the targeted player as it resolves") {
        val (driver, you, opponent) = newGame()
        castPeek(driver, you, opponent)

        val owners = driver.state.grantedStaticAbilities.map {
            when (val ability = it.ability) {
                is LookAtTopOfLibrary -> ability.libraryOwner
                is PlayFromTopWithAlternativeCost -> ability.libraryOwner
                else -> null
            }
        }
        owners shouldBe listOf(EffectTarget.SpecificEntity(opponent), EffectTarget.SpecificEntity(opponent))
        driver.state.grantedStaticAbilities.map { it.entityId } shouldBe listOf(you, you)
    }

    test("a scoped grant opens only the named library") {
        val (driver, you, opponent) = newGame()
        castPeek(driver, you, opponent)
        val theirs = driver.putCardOnTopOfLibrary(opponent, "Lightning Bolt")
        val yours = driver.putCardOnTopOfLibrary(you, "Lightning Bolt")
        driver.giveMana(you, Color.BLACK, 1)

        visibleTo(driver, you, theirs) shouldBe true
        visibleTo(driver, you, yours) shouldBe false
        (theirs in libraryCasts(driver, you)) shouldBe true
        (yours in libraryCasts(driver, you)) shouldBe false
    }

    test("a printed look-at-the-top permission opens only its holder's own library") {
        val (driver, you, opponent) = newGame()
        driver.putPermanentOnBattlefield(you, "Test Lens")
        val theirs = driver.putCardOnTopOfLibrary(opponent, "Lightning Bolt")
        val yours = driver.putCardOnTopOfLibrary(you, "Lightning Bolt")

        visibleTo(driver, you, yours) shouldBe true
        visibleTo(driver, you, theirs) shouldBe false
    }

    test("any-color mana pays colored pips but not a {C} pip") {
        val (driver, you, opponent) = newGame()
        castPeek(driver, you, opponent)
        val beast = driver.putCardOnTopOfLibrary(opponent, "Colorless Pip Beast")

        driver.giveMana(you, Color.BLACK, 2)
        driver.submitExpectFailure(CastSpell(you, beast, paymentStrategy = PaymentStrategy.FromPool))

        driver.giveColorlessMana(you, 1)
        driver.submit(CastSpell(you, beast, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        driver.bothPass()
        driver.getController(beast) shouldBe you
    }
})
