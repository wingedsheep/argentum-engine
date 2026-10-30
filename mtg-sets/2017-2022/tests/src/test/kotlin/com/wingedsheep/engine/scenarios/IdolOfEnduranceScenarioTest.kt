package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m21.cards.IdolOfEndurance
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Idol of Endurance (M21 #23).
 *
 *   When this artifact enters, exile all creature cards with mana value 3 or less from your
 *   graveyard until this artifact leaves the battlefield.
 *   {1}{W}, {T}: Until end of turn, you may cast a creature spell from among cards exiled with this
 *   artifact without paying its mana cost.
 *
 * Pins the mana-value cut, one free cast per activation (and two for two), and the return to the
 * graveyard of whatever is still exiled when the Idol leaves (2020-06-23 rulings).
 */
class IdolOfEnduranceScenarioTest : ScenarioTestBase() {

    init {
        fun setup(vararg graveyard: String, withShatter: Boolean = false): TestGame {
            val builder = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Idol of Endurance")
                .withLandsOnBattlefield(1, "Plains", 7)
            if (withShatter) builder.withCardInHand(1, "Shatter").withLandsOnBattlefield(1, "Mountain", 2)
            builder
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            graveyard.forEach { builder.withCardInGraveyard(1, it) }
            val game = builder.build()
            game.castSpell(1, "Idol of Endurance").error shouldBe null
            game.resolveStack()
            return game
        }

        fun activate(game: TestGame) {
            val idol = game.findPermanent("Idol of Endurance")!!
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = idol,
                    abilityId = IdolOfEndurance.activatedAbilities[0].id,
                )
            ).error shouldBe null
            game.resolveStack()
        }

        fun untappedPlains(game: TestGame) =
            game.findPermanents("Plains").count { game.state.getEntity(it)?.has<TappedComponent>() != true }

        test("enters: exiles only creature cards with mana value 3 or less from your graveyard") {
            val game = setup("Grizzly Bears", "Hill Giant", "Craw Wurm", "Lightning Bolt")

            withClue("the MV 2 creature is exiled") {
                game.isInExile(1, "Grizzly Bears") shouldBe true
            }
            withClue("MV 4 and MV 6 creatures and a noncreature stay in the graveyard") {
                game.isInGraveyard(1, "Hill Giant") shouldBe true
                game.isInGraveyard(1, "Craw Wurm") shouldBe true
                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            }
        }

        test("one activation lets you cast one exiled creature for free, not two") {
            val game = setup("Grizzly Bears", "Grizzly Bears")
            withClue("before activating, the exiled cards can't be cast") {
                game.castSpellFromExile(1, "Grizzly Bears").error shouldNotBe null
            }

            activate(game)
            val untappedBefore = untappedPlains(game)
            game.castSpellFromExile(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            withClue("the Bears resolved without spending mana") {
                game.findPermanents("Grizzly Bears").size shouldBe 1
                untappedPlains(game) shouldBe untappedBefore
            }
            withClue("the grant covered one spell: the second Bears stays exiled") {
                game.castSpellFromExile(1, "Grizzly Bears").error shouldNotBe null
            }
        }

        test("two activations in a turn are two free casts") {
            val game = setup("Grizzly Bears", "Grizzly Bears")
            activate(game)
            game.state = game.state.updateEntity(game.findPermanent("Idol of Endurance")!!) { it.without<TappedComponent>() }
            activate(game)

            game.castSpellFromExile(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.castSpellFromExile(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.findPermanents("Grizzly Bears").size shouldBe 2
        }

        test("when the Idol leaves, the cards still exiled return to the graveyard; a cast one stays put") {
            val game = setup("Grizzly Bears", "Savannah Lions", withShatter = true)
            activate(game)
            game.castSpellFromExile(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.castSpell(1, "Shatter", game.findPermanent("Idol of Endurance")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Idol of Endurance") shouldBe true

            withClue("the uncast card is back in the graveyard") {
                game.isInGraveyard(1, "Savannah Lions") shouldBe true
                game.isInExile(1, "Savannah Lions") shouldBe false
            }
            withClue("the creature cast from the pile stays on the battlefield") {
                game.findPermanents("Grizzly Bears").size shouldBe 1
            }
        }
    }
}
