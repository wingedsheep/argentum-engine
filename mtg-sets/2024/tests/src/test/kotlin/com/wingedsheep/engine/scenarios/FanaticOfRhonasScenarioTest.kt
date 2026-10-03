package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.FanaticOfRhonas
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fanatic of Rhonas (MH3 #152) — {1}{G} Creature — Snake Druid 1/4.
 *
 *   {T}: Add {G}.
 *   Ferocious — {T}: Add {G}{G}{G}{G}. Activate only if you control a creature with power 4 or greater.
 *   Eternalize {2}{G}{G}
 */
class FanaticOfRhonasScenarioTest : ScenarioTestBase() {

    private val name = "Fanatic of Rhonas"
    private val plainManaId = FanaticOfRhonas.activatedAbilities[0].id
    private val ferociousId = FanaticOfRhonas.activatedAbilities[1].id
    private val eternalizeId = FanaticOfRhonas.activatedAbilities[2].id

    private fun green(game: TestGame) =
        game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.green

    init {
        fun board() = scenario()
            .withPlayers("Player1", "Player2")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("taps for {G}") {
            val game = board().withCardOnBattlefield(1, name).build()
            val fanatic = game.findPermanent(name)!!

            game.execute(ActivateAbility(game.player1Id, fanatic, plainManaId)).error shouldBe null
            green(game) shouldBe 1
        }

        test("ferocious taps for {G}{G}{G}{G} when you control a creature with power 4 or greater") {
            val game = board()
                .withCardOnBattlefield(1, name)
                .withCardOnBattlefield(1, "Force of Nature")
                .build()
            val fanatic = game.findPermanent(name)!!

            game.execute(ActivateAbility(game.player1Id, fanatic, ferociousId)).error shouldBe null
            green(game) shouldBe 4
        }

        test("ferocious can't be activated without a creature with power 4 or greater") {
            val game = board()
                .withCardOnBattlefield(1, name)
                .withCardOnBattlefield(1, "Centaur Courser")
                .withCardOnBattlefield(2, "Force of Nature")
                .build()
            val fanatic = game.findPermanent(name)!!

            withClue("a 3/3 of yours and an opponent's 5/5 don't satisfy 'you control … power 4 or greater'") {
                game.execute(ActivateAbility(game.player1Id, fanatic, ferociousId)).error shouldNotBe null
                green(game) shouldBe 0
            }
        }

        test("eternalize exiles the card and makes a 4/4 black Zombie token copy with no mana cost") {
            val game = board()
                .withCardInGraveyard(1, name)
                .withLandsOnBattlefield(1, "Forest", 4)
                .build()
            val card = game.findCardsInGraveyard(1, name).single()

            game.execute(ActivateAbility(game.player1Id, card, eternalizeId)).error shouldBe null
            withClue("the card is exiled as part of the cost") {
                game.isInExile(1, name) shouldBe true
                game.isInGraveyard(1, name) shouldBe false
            }
            game.resolveStack()

            val token = game.findPermanent(name).shouldNotBeNull()
            game.state.getEntity(token)!!.has<TokenComponent>() shouldBe true
            val tokenCard = game.state.getEntity(token)!!.get<CardComponent>().shouldNotBeNull()
            tokenCard.colors shouldBe setOf(Color.BLACK)
            tokenCard.manaCost shouldBe ManaCost.ZERO
            tokenCard.typeLine.subtypes shouldContain Subtype.ZOMBIE
            tokenCard.typeLine.subtypes shouldContain Subtype("Snake")
            tokenCard.typeLine.subtypes shouldContain Subtype("Druid")
            game.state.projectedState.getPower(token) shouldBe 4
            game.state.projectedState.getToughness(token) shouldBe 4
        }

        test("eternalize is sorcery-speed only") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardInGraveyard(1, name)
                .withLandsOnBattlefield(1, "Forest", 4)
                .build()
            val card = game.findCardsInGraveyard(1, name).single()

            game.execute(ActivateAbility(game.player1Id, card, eternalizeId)).error shouldNotBe null
            game.isInGraveyard(1, name) shouldBe true
        }
    }
}
