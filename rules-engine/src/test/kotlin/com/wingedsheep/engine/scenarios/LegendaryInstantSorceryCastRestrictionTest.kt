package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * CR 205.4e — "Any instant or sorcery spell with the supertype 'legendary' is subject to a casting
 * restriction. A player can't cast a legendary instant or sorcery spell unless that player controls
 * a legendary creature or a legendary planeswalker."
 *
 * The restriction is intrinsic to the card's type line (not a per-card opt-in), so it's enforced in
 * the engine's two cast chokepoints: legal-action enumeration ([com.wingedsheep.engine.legalactions
 * .EnumerationContext.cantCastSpell]) and the authoritative [com.wingedsheep.engine.handlers.actions
 * .spell.CastSpellHandler] (via `reasonCannotCast`). Both are asserted below.
 */
class LegendaryInstantSorceryCastRestrictionTest : ScenarioTestBase() {

    init {
        // Argument-free spells so the restriction is tested in isolation (no target/mana noise).
        val legendaryInstant = card("Test Legendary Instant") {
            manaCost = "{0}"; typeLine = "Legendary Instant"
            spell { effect = Effects.DrawCards(1) }
        }
        val legendarySorcery = card("Test Legendary Sorcery") {
            manaCost = "{0}"; typeLine = "Legendary Sorcery"
            spell { effect = Effects.DrawCards(1) }
        }
        val plainInstant = card("Test Plain Instant") {
            manaCost = "{0}"; typeLine = "Instant"
            spell { effect = Effects.DrawCards(1) }
        }
        val legendaryCreature = card("Test Legend Bear") {
            manaCost = "{0}"; typeLine = "Legendary Creature — Bear"; power = 2; toughness = 2
        }
        val plainCreature = card("Test Plain Bear") {
            manaCost = "{0}"; typeLine = "Creature — Bear"; power = 2; toughness = 2
        }
        val legendaryArtifact = card("Test Legend Relic") {
            manaCost = "{0}"; typeLine = "Legendary Artifact"
        }
        val legendaryPlaneswalker = card("Test Legend Walker") {
            manaCost = "{0}"; typeLine = "Legendary Planeswalker — Tester"; startingLoyalty = 3
        }
        cardRegistry.register(
            listOf(
                legendaryInstant, legendarySorcery, plainInstant,
                legendaryCreature, plainCreature, legendaryArtifact, legendaryPlaneswalker
            )
        )

        val RESTRICTION_MSG = "legendary creature or planeswalker"

        fun TestGame.castOfferedFor(spellName: String): Boolean =
            getLegalActions(1).any { info ->
                val a = info.action
                a is CastSpell && state.getEntity(a.cardId)?.get<CardComponent>()?.name == spellName
            }

        test("legendary instant is NOT castable with no legendary permanents (enumeration + handler)") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Test Legendary Instant")
                .withCardInLibrary(1, "Test Plain Bear")
                .build()

            game.castOfferedFor("Test Legendary Instant").shouldBeFalse()

            val result = game.castSpell(1, "Test Legendary Instant")
            result.isSuccess.shouldBeFalse()
            result.error!! shouldContain RESTRICTION_MSG
        }

        test("legendary instant IS castable while controlling a legendary creature") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Test Legendary Instant")
                .withCardOnBattlefield(1, "Test Legend Bear")
                .withCardInLibrary(1, "Test Plain Bear")
                .build()

            game.castOfferedFor("Test Legendary Instant").shouldBeTrue()
            game.castSpell(1, "Test Legendary Instant").isSuccess.shouldBeTrue()
        }

        test("legendary instant IS castable while controlling a legendary planeswalker") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Test Legendary Instant")
                .withCardOnBattlefield(1, "Test Legend Walker")
                .withCardInLibrary(1, "Test Plain Bear")
                .build()

            game.castOfferedFor("Test Legendary Instant").shouldBeTrue()
            game.castSpell(1, "Test Legendary Instant").isSuccess.shouldBeTrue()
        }

        test("a NON-legendary creature does not satisfy the restriction") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Test Legendary Instant")
                .withCardOnBattlefield(1, "Test Plain Bear")
                .withCardInLibrary(1, "Test Plain Bear")
                .build()

            game.castOfferedFor("Test Legendary Instant").shouldBeFalse()
            game.castSpell(1, "Test Legendary Instant").error!! shouldContain RESTRICTION_MSG
        }

        test("a legendary NON-creature/non-planeswalker (artifact) does not satisfy the restriction") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Test Legendary Instant")
                .withCardOnBattlefield(1, "Test Legend Relic")
                .withCardInLibrary(1, "Test Plain Bear")
                .build()

            game.castOfferedFor("Test Legendary Instant").shouldBeFalse()
            game.castSpell(1, "Test Legendary Instant").error!! shouldContain RESTRICTION_MSG
        }

        test("the restriction applies to legendary SORCERIES too") {
            val without = scenario()
                .withPlayers()
                .withCardInHand(1, "Test Legendary Sorcery")
                .withCardInLibrary(1, "Test Plain Bear")
                .build()
            without.castOfferedFor("Test Legendary Sorcery").shouldBeFalse()
            without.castSpell(1, "Test Legendary Sorcery").error!! shouldContain RESTRICTION_MSG

            val with = scenario()
                .withPlayers()
                .withCardInHand(1, "Test Legendary Sorcery")
                .withCardOnBattlefield(1, "Test Legend Bear")
                .withCardInLibrary(1, "Test Plain Bear")
                .build()
            with.castOfferedFor("Test Legendary Sorcery").shouldBeTrue()
            with.castSpell(1, "Test Legendary Sorcery").isSuccess.shouldBeTrue()
        }

        test("a NON-legendary instant is unaffected (castable with no legendary permanents)") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Test Plain Instant")
                .withCardInLibrary(1, "Test Plain Bear")
                .build()

            game.castOfferedFor("Test Plain Instant").shouldBeTrue()
            game.castSpell(1, "Test Plain Instant").isSuccess.shouldBeTrue()
        }

        test("only the OPPONENT's legendary creature does not let you cast (control matters)") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Test Legendary Instant")
                .withCardOnBattlefield(2, "Test Legend Bear")
                .withCardInLibrary(1, "Test Plain Bear")
                .build()

            game.castOfferedFor("Test Legendary Instant").shouldBeFalse()
            game.castSpell(1, "Test Legendary Instant").error!! shouldContain RESTRICTION_MSG
        }

        test("the real Isildur's Fateful Strike is blocked without a legendary permanent") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Isildur's Fateful Strike")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardOnBattlefield(2, "Test Plain Bear")
                .withCardInLibrary(1, "Test Plain Bear")
                .build()

            game.castOfferedFor("Isildur's Fateful Strike").shouldBeFalse()

            val bearId = game.findPermanent("Test Plain Bear")!!
            val result = game.castSpell(1, "Isildur's Fateful Strike", bearId)
            result.isSuccess.shouldBeFalse()
            result.error!! shouldContain RESTRICTION_MSG
        }
    }
}
