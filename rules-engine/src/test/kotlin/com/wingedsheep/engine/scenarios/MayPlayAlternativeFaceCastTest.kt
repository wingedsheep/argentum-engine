package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.blc.cards.BrightcapBadger
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * A "you may cast that card" permission covers every face the card can be cast as: an Adventurer
 * card may be cast as its Adventure (CR 715.3) and a modal double-faced card as either face
 * (CR 712.11b) — including a modal DFC whose back is a *permanent*, cast transformed for the back's
 * own mana cost. The exceptions pin a face: the Adventure's own exile grants the creature alone
 * (CR 715.3d), and a permission carrying `castFaceIndex` grants only that face.
 *
 * Engine-level: exercises the from-exile enumerator and the authoritative cast validator across an
 * Adventure card (Brightcap Badger // Fungus Frolic) and a permanent-back modal DFC (Jennifer
 * Walters // The Sensational She-Hulk).
 */
class MayPlayAlternativeFaceCastTest : ScenarioTestBase() {

    private fun TestGame.grantCast(card: EntityId, primaryFaceOnly: Boolean = false) {
        val (id, withId) = state.newEntity()
        state = withId.addMayPlayPermission(
            MayPlayPermission(id, setOf(card), player1Id, primaryFaceOnly = primaryFaceOnly, timestamp = withId.timestamp)
        )
    }

    private fun TestGame.exiled(name: String): EntityId =
        state.getExile(player1Id).single { state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == name }

    private fun TestGame.castOffers(cardId: EntityId): List<CastSpell> =
        getLegalActions(1).mapNotNull { it.action as? CastSpell }.filter { it.cardId == cardId }

    private fun CastSpell.isModalBack() = useAlternativeCost && alternativeCostType == AlternativeCostType.MODAL_BACK_FACE

    init {
        cardRegistry.register(BrightcapBadger)
        // A prepare card's spell face is cast only from a prepared copy (Secrets of Strixhaven), so an
        // ordinary grant on the card itself must not reach it.
        cardRegistry.register(card("Test Prepare Adept") {
            manaCost = "{1}{G}"
            typeLine = "Creature — Human Wizard"
            power = 2
            toughness = 2
            prepare("Test Prepared Insight") {
                manaCost = "{G}"
                typeLine = "Sorcery"
                spell { effect = Effects.GainLife(1) }
            }
        })

        fun exiledWith(card: String, vararg lands: Pair<String, Int>) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInExile(1, card)
            .apply { lands.forEach { (land, n) -> withLandsOnBattlefield(1, land, n) } }
            .withCardInLibrary(1, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("an exiled modal DFC's permanent back face is castable under a may-play permission") {
            val game = exiledWith("Jennifer Walters", "Forest" to 3, "Plains" to 3)
            val card = game.exiled("Jennifer Walters")
            game.grantCast(card)

            val offers = game.castOffers(card)
            withClue("both faces are offered (CR 712.11b)") {
                offers.count { it.isModalBack() } shouldBe 1
                offers.count { !it.isModalBack() && it.faceIndex == null } shouldBe 1
            }

            game.execute(offers.single { it.isModalBack() }).error shouldBe null
            game.resolveStack()

            withClue("it resolves back face up (CR 712.13)") {
                val hulk = game.findPermanent("The Sensational She-Hulk").shouldNotBeNull()
                game.state.getEntity(hulk)!!.get<DoubleFacedComponent>()?.currentFace shouldBe DoubleFacedComponent.Face.BACK
            }
        }

        test("an exiled Adventurer card is castable as its Adventure, which then exiles it for the creature only") {
            val game = exiledWith("Brightcap Badger", "Forest" to 4)
            val card = game.exiled("Brightcap Badger")
            game.grantCast(card)

            game.castOffers(card).map { it.faceIndex }.toSet() shouldBe setOf(null, 0)

            game.execute(CastSpell(game.player1Id, card, faceIndex = 0)).error shouldBe null
            game.resolveStack()

            withClue("Fungus Frolic made two Saprolings and exiled the card (CR 715.3d)") {
                game.findAllPermanents("Saproling Token").size shouldBe 2
                game.state.getExile(game.player1Id) shouldBe listOf(card)
            }
            withClue("on an adventure, only the creature is castable") {
                game.castOffers(card).map { it.faceIndex } shouldBe listOf(null)
                game.execute(CastSpell(game.player1Id, card, faceIndex = 0)).error shouldNotBe null
            }
        }

        test("a plain permission does not reach a face of a layout cast only another way (prepare)") {
            val game = exiledWith("Test Prepare Adept", "Forest" to 2)
            val card = game.exiled("Test Prepare Adept")
            game.grantCast(card)

            game.castOffers(card).map { it.faceIndex } shouldBe listOf(null)
            game.execute(CastSpell(game.player1Id, card, faceIndex = 0)).error shouldNotBe null
        }

        test("a primary-only permission rejects a client-supplied alternative face") {
            val game = exiledWith("Jennifer Walters", "Forest" to 3, "Plains" to 3)
            val card = game.exiled("Jennifer Walters")
            game.grantCast(card, primaryFaceOnly = true)

            game.castOffers(card).none { it.isModalBack() } shouldBe true
            game.execute(
                CastSpell(game.player1Id, card, useAlternativeCost = true, alternativeCostType = AlternativeCostType.MODAL_BACK_FACE)
            ).error shouldNotBe null
        }
    }
}
