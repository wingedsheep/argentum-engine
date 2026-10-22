package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mid.cards.CurseOfShakenFaith
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Curse of Shaken Faith (MID #134) — "Whenever enchanted player casts a spell other than the first
 * spell they cast each turn or copies a spell, this Aura deals 2 damage to them."
 *
 * The first cast of the turn is free, every later cast costs 2 life, and a copy the enchanted
 * player puts on the stack costs 2 more (a copy isn't cast, so it is the trigger's other half).
 * The Curse's own controller is never hit.
 */
class CurseOfShakenFaithScenarioTest : ScenarioTestBase() {

    private val quiet = card("Quiet Instant") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }

    private val copier = card("Spell Copier") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val original = target(TargetFilter.SpellOnStack)
            effect = Effects.CopyTargetSpell(original)
        }
    }

    init {
        cardRegistry.register(listOf(CurseOfShakenFaith, quiet, copier))

        /** The Curse on player 1's battlefield, enchanting player 2. */
        fun cursedOpponent(activePlayer: Int): TestGame {
            val builder = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Curse of Shaken Faith")
                .withActivePlayer(activePlayer)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            for (p in 1..2) {
                builder.withCardsInHand(p, "Quiet Instant", 3).withCardInHand(p, "Spell Copier")
                repeat(6) { builder.withCardInLibrary(p, "Plains") }
            }
            val game = builder.build()
            val curse = game.findPermanent("Curse of Shaken Faith")!!
            game.state = game.state.updateEntity(curse) { it.with(AttachedToComponent(game.player2Id)) }
            return game
        }

        fun TestGame.castAndResolve(player: Int, name: String) {
            castSpell(player, name).error shouldBe null
            resolveStack()
        }

        test("the enchanted player's first spell each turn is free; each later one deals 2 to them") {
            val game = cursedOpponent(activePlayer = 2)

            game.castAndResolve(2, "Quiet Instant")
            withClue("first spell of the turn") { game.getLifeTotal(2) shouldBe 20 }

            game.castAndResolve(2, "Quiet Instant")
            withClue("second spell") { game.getLifeTotal(2) shouldBe 18 }

            game.castAndResolve(2, "Quiet Instant")
            withClue("third spell") { game.getLifeTotal(2) shouldBe 16 }
            game.getLifeTotal(1) shouldBe 20
        }

        test("copying a spell deals 2 on top of the cast that made the copy") {
            val game = cursedOpponent(activePlayer = 2)

            game.castSpell(2, "Quiet Instant").error shouldBe null
            game.castSpellTargetingStackSpell(2, "Spell Copier", "Quiet Instant").error shouldBe null
            game.resolveStack()

            withClue("Spell Copier is the second cast (2), its copy of Quiet Instant is a copy (2)") {
                game.getLifeTotal(2) shouldBe 16
            }
            game.getLifeTotal(1) shouldBe 20
        }

        test("the Curse's controller casting and copying spells takes no damage") {
            val game = cursedOpponent(activePlayer = 1)

            game.castAndResolve(1, "Quiet Instant")
            game.castSpell(1, "Quiet Instant").error shouldBe null
            game.castSpellTargetingStackSpell(1, "Spell Copier", "Quiet Instant").error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
