package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.blc.cards.BrightcapBadger
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * A may-play permission's spell-type rider (`castSpellTypes`, "you may cast it if it's a creature
 * spell" — Vivien, Champion of the Wilds) is checked against the face actually being cast, not the
 * card in exile: an Adventurer card (Brightcap Badger // Fungus Frolic) is a creature card, but its
 * Adventure is a sorcery spell (CR 715.3), so a creature-only grant covers the creature half alone
 * and an instant-or-sorcery grant the Adventure alone. A spell needs only *one* of the listed types.
 *
 * Engine-level: the from-exile enumerator and the authoritative cast validator (the action is
 * client-supplied) must agree.
 */
class MayPlaySpellTypeRiderTest : ScenarioTestBase() {

    private fun TestGame.grant(card: EntityId, types: Set<CardType>, nonLandOnly: Boolean = true) {
        val (id, withId) = state.newEntity()
        state = withId.addMayPlayPermission(
            MayPlayPermission(
                id, setOf(card), player1Id,
                nonLandOnly = nonLandOnly,
                castSpellTypes = types,
                timestamp = withId.timestamp,
            )
        )
    }

    private fun TestGame.exiled(name: String): EntityId =
        state.getExile(player1Id).single { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.castOffers(cardId: EntityId): List<CastSpell> =
        getLegalActions(1).mapNotNull { it.action as? CastSpell }.filter { it.cardId == cardId }

    init {
        cardRegistry.register(BrightcapBadger)

        fun exiledWith(card: String, land: String, count: Int) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInExile(1, card)
            .withLandsOnBattlefield(1, land, count)
            .withCardInLibrary(1, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("a creature-only grant offers an Adventurer card's creature half but not its sorcery Adventure") {
            val game = exiledWith("Brightcap Badger", "Forest", 4)
            val card = game.exiled("Brightcap Badger")
            game.grant(card, setOf(CardType.CREATURE))

            game.castOffers(card).map { it.faceIndex } shouldBe listOf(null)

            withClue("a hand-built Adventure cast is rejected by the validator") {
                val error = game.execute(CastSpell(game.player1Id, card, faceIndex = 0)).error
                error.shouldNotBeNull() shouldContain "creature spells"
            }

            game.execute(CastSpell(game.player1Id, card)).error shouldBe null
            game.resolveStack()
            game.findPermanent("Brightcap Badger").shouldNotBeNull()
        }

        test("an instant-or-sorcery grant offers the Adventure but not the creature half") {
            val game = exiledWith("Brightcap Badger", "Forest", 4)
            val card = game.exiled("Brightcap Badger")
            game.grant(card, setOf(CardType.INSTANT, CardType.SORCERY))

            game.castOffers(card).map { it.faceIndex } shouldBe listOf(0)
            game.execute(CastSpell(game.player1Id, card)).error shouldNotBe null

            game.execute(CastSpell(game.player1Id, card, faceIndex = 0)).error shouldBe null
            game.resolveStack()
            game.findAllPermanents("Saproling Token").size shouldBe 2
        }

        test("a creature-only grant offers nothing for a noncreature card, which stays exiled") {
            val game = exiledWith("Lightning Bolt", "Mountain", 1)
            val card = game.exiled("Lightning Bolt")
            game.grant(card, setOf(CardType.CREATURE))

            game.castOffers(card) shouldBe emptyList()
            game.execute(CastSpell(game.player1Id, card)).error shouldNotBe null
            game.state.getExile(game.player1Id) shouldBe listOf(card)
        }

        test("with nonLandOnly a creature-only grant never lets an exiled land be played") {
            val game = exiledWith("Forest", "Forest", 1)
            val card = game.exiled("Forest")
            game.grant(card, setOf(CardType.CREATURE))

            game.getLegalActions(1).none { (it.action as? PlayLand)?.cardId == card } shouldBe true
            game.execute(PlayLand(game.player1Id, card)).error shouldNotBe null
        }
    }
}
