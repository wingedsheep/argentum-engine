package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.KeepHand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.nph.cards.KarnLiberated
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Karn Liberated (NPH #1, {7}, Loyalty 6).
 *
 *   +4: Target player exiles a card from their hand.
 *   −3: Exile target permanent.
 *   −14: Restart the game, leaving in exile all non-Aura permanent cards exiled with Karn. Then put
 *        those cards onto the battlefield under your control.
 */
class KarnLiberatedScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(KarnLiberated))

        test("+4: the targeted player chooses the card they exile, and it's exiled with Karn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Karn Liberated")
                .withCardInHand(2, "Savannah Lions")
                .withCardInHand(2, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val karn = game.findPermanent("Karn Liberated")!!
            setLoyalty(game, karn, 6)

            activate(game, karn, index = 0, targets = listOf(ChosenTarget.Player(game.player2Id)))
            game.resolveStack()

            val choice = game.state.pendingDecision as SelectCardsDecision
            withClue("the targeted player picks from their own hand") { choice.playerId shouldBe game.player2Id }
            val bolt = game.findCardsInHand(2, "Lightning Bolt").single()
            game.selectCards(listOf(bolt))
            game.resolveStack()

            game.isInExile(2, "Lightning Bolt") shouldBe true
            game.isInHand(2, "Savannah Lions") shouldBe true
            linkedExile(game, karn) shouldContain bolt
            loyalty(game, karn) shouldBe 10
        }

        test("−3: exiles target permanent, linked to Karn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Karn Liberated")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val karn = game.findPermanent("Karn Liberated")!!
            setLoyalty(game, karn, 6)
            val courser = game.findPermanent("Centaur Courser")!!

            activate(game, karn, index = 1, targets = listOf(ChosenTarget.Permanent(courser)))
            game.resolveStack()

            game.isInExile(2, "Centaur Courser") shouldBe true
            linkedExile(game, karn) shouldContain courser
            loyalty(game, karn) shouldBe 3
        }

        test("−14: restarts the game; Karn's non-Aura permanent cards return under your control after mulligans") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Karn Liberated")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardInExile(2, "Centaur Courser")
                .withCardInExile(2, "Pacifism")
                .withCardInExile(1, "Lightning Bolt")
                .withCardsInHand(1, "Island", 10)
                .withCardsInHand(2, "Forest", 10)
                .withLifeTotal(1, 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val karn = game.findPermanent("Karn Liberated")!!
            // Exactly 14: paying −14 puts Karn in the graveyard before the ability resolves, and the
            // cards it exiled must still be found.
            setLoyalty(game, karn, 14)
            val pile = listOf("Centaur Courser", "Pacifism").map { name -> exiled(game, 2, name) } +
                exiled(game, 1, "Lightning Bolt")
            game.state = game.state.updateEntity(karn) { it.with(LinkedExileComponent(pile)) }

            activate(game, karn, index = 2)
            game.resolveStack()

            withClue("the game restarted, with Karn's controller as the starting player") {
                game.state.turnNumber shouldBe 1
                game.state.activePlayerId shouldBe game.player1Id
                game.state.step shouldBe Step.UNTAP
                game.getLifeTotal(1) shouldBe 20
                game.handSize(1) shouldBe 7
                game.handSize(2) shouldBe 7
            }
            withClue("only the non-Aura permanent card stays in exile") {
                game.isInExile(2, "Centaur Courser") shouldBe true
                game.isInExile(2, "Pacifism") shouldBe false
                game.isInExile(1, "Lightning Bolt") shouldBe false
                game.state.getBattlefield().size shouldBe 0
            }

            game.execute(KeepHand(game.player1Id)).error shouldBe null
            game.execute(KeepHand(game.player2Id)).error shouldBe null

            val courser = withClue("Centaur Courser entered the battlefield before the first turn") {
                game.findPermanent("Centaur Courser")!!
            }
            withClue("…under Karn's controller's control, still owned by its owner") {
                game.state.projectedState.getController(courser) shouldBe game.player1Id
                game.state.getEntity(courser)?.get<OwnerComponent>()?.playerId shouldBe game.player2Id
            }
            withClue("the new game is under way at the first upkeep") {
                game.state.step shouldBe Step.UPKEEP
                game.state.activePlayerId shouldBe game.player1Id
            }
            withClue("Karn itself is a card of its owner's new deck") {
                val karnCards = game.state.entities.values.filter { it.get<CardComponent>()?.name == "Karn Liberated" }
                karnCards.size shouldBe 1
                val karnId = game.state.entities.entries.single { it.value.get<CardComponent>()?.name == "Karn Liberated" }.key
                (karnId in game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY)) ||
                    karnId in game.state.getHand(game.player1Id)) shouldBe true
            }
        }
    }

    private fun exiled(game: TestGame, playerNumber: Int, name: String): EntityId {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getExile(playerId).single { game.state.getEntity(it)?.get<CardComponent>()?.name == name }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int, targets: List<ChosenTarget> = emptyList()) {
        val ability = cardRegistry.getCard("Karn Liberated")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = source, abilityId = ability.id, targets = targets)
        ).error shouldBe null
    }

    private fun linkedExile(game: TestGame, karn: EntityId): List<EntityId> =
        game.state.getEntity(karn)?.get<LinkedExileComponent>()?.exiledIds.orEmpty()

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
