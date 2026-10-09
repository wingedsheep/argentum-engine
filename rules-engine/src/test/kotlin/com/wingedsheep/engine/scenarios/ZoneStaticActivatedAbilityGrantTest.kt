package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.matchers.shouldBe

class ZoneStaticActivatedAbilityGrantTest : ScenarioTestBase() {
    init {
        val graveAbility = ActivatedAbility(
            id = AbilityId("zone_grant_life"), cost = AbilityCost.Free,
            effect = Effects.GainLife(3), activateFromZone = Zone.GRAVEYARD,
            timing = TimingRule.SorcerySpeed, descriptionOverride = "Gain 3 life"
        )
        val handAbility = graveAbility.copy(id = AbilityId("hand_grant_life"), activateFromZone = Zone.HAND)
        fun granter(name: String, zone: Zone = Zone.GRAVEYARD, conditional: Boolean = false) = card(name) {
            typeLine = "Creature — Wizard"
            power = 2
            toughness = 2
            staticAbility {
                val grant = GrantActivatedAbility(
                    if (zone == Zone.HAND) handAbility else graveAbility,
                    GroupFilter(GameObjectFilter.Artifact.ownedByYou()), recipientZone = zone
                )
                ability = if (conditional) ConditionalStaticAbility(grant, Conditions.SourceIsTapped) else grant
            }
        }
        cardRegistry.register(granter("Graveyard Mentor"))
        cardRegistry.register(granter("Hand Mentor", Zone.HAND))
        cardRegistry.register(granter("Conditional Mentor", conditional = true))
        cardRegistry.register(card("Zone Grant Blanker") {
            typeLine = "Enchantment"
            staticAbility { ability = LoseAllAbilities(GroupFilter.AllCreatures) }
        })
        // A default battlefield grant with a graveyard activation is intentionally dormant:
        // grant location and activation location are independent axes.
        cardRegistry.register(card("Battlefield Mentor") {
            typeLine = "Enchantment"
            staticAbility { ability = GrantActivatedAbility(graveAbility, GroupFilter(GameObjectFilter.Artifact.ownedByYou())) }
        })

        fun board(source: String = "Graveyard Mentor") = scenario().withPlayers("A", "B")
            .withCardOnBattlefield(1, source)
            .withCardInGraveyard(1, "Ornithopter")
            .withCardInGraveyard(1, "Grizzly Bears")
            .withCardInGraveyard(2, "Ornithopter")
            .withCardInHand(1, "Ornithopter")
            .withCardOnBattlefield(1, "Ornithopter")
            .withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
        fun TestGame.actions() = getLegalActions(1).mapNotNull { it.action as? ActivateAbility }
            .filter { it.abilityId == graveAbility.id || it.abilityId == handAbility.id }
        fun TestGame.graveAction() = ActivateAbility(player1Id, findCardsInGraveyard(1, "Ornithopter").single(), graveAbility.id)

        test("grant filters recipients by zone type and owner and activates on the receiving card") {
            val game = board()
            game.actions().map { it.sourceId } shouldBe listOf(game.graveAction().sourceId)
            game.execute(game.graveAction()).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 23
            game.getLifeTotal(2) shouldBe 20
        }
        test("default battlefield grant never leaks into a graveyard") {
            val game = board("Battlefield Mentor")
            game.actions() shouldBe emptyList()
            (game.execute(game.graveAction()).error != null) shouldBe true
        }
        test("same axis supports a hand ability without granting it in graveyards") {
            val game = board("Hand Mentor")
            val action = game.actions().single()
            action.sourceId shouldBe game.findCardsInHand(1, "Ornithopter").single()
            action.abilityId shouldBe handAbility.id
            game.execute(action).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 23
        }
        test("source leaving removes the offer and rejects a stale direct activation") {
            val game = board()
            val action = game.graveAction()
            game.actions().size shouldBe 1
            game.state = game.state.removeEntity(game.findPermanent("Graveyard Mentor")!!)
            game.actions() shouldBe emptyList()
            (game.execute(action).error != null) shouldBe true
        }
        test("an activation already on the stack survives removal of its granter") {
            val game = board()
            game.execute(game.graveAction()).error shouldBe null
            game.state = game.state.removeEntity(game.findPermanent("Graveyard Mentor")!!)
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 23
        }
        test("changing the source controller switches which owner's cards receive the grant") {
            val game = board()
            val source = game.findPermanent("Graveyard Mentor")!!
            game.state = game.state.updateEntity(source) { it.with(ControllerComponent(game.player2Id)) }
            game.actions() shouldBe emptyList()
            (game.execute(game.graveAction()).error != null) shouldBe true
            game.state = game.state.copy(activePlayerId = game.player2Id, priorityPlayerId = game.player2Id)
            val action = game.getLegalActions(2).mapNotNull { it.action as? ActivateAbility }
                .single { it.abilityId == graveAbility.id }
            action.sourceId shouldBe game.findCardsInGraveyard(2, "Ornithopter").single()
            game.execute(action).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 23
        }
        test("source losing its printed abilities removes its zone grant") {
            val game = scenario().withPlayers("A", "B")
                .withCardOnBattlefield(1, "Graveyard Mentor")
                .withCardOnBattlefield(2, "Zone Grant Blanker")
                .withCardInGraveyard(1, "Ornithopter")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.actions() shouldBe emptyList()
            (game.execute(game.graveAction()).error != null) shouldBe true
        }
        test("a granted static confers the same zone ability as a printed static") {
            val game = board("Battlefield Mentor")
            val source = game.findPermanent("Battlefield Mentor")!!
            game.state = game.state.copy(grantedStaticAbilities = listOf(
                com.wingedsheep.engine.event.GrantedStaticAbility(
                    source, GrantActivatedAbility(graveAbility,
                        GroupFilter(GameObjectFilter.Artifact.ownedByYou()), recipientZone = Zone.GRAVEYARD),
                    Duration.EndOfTurn
                )
            ))
            game.actions().size shouldBe 1
            game.execute(game.graveAction()).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 23
        }
        test("a second granter keeps the grant alive when the first leaves") {
            val game = scenario().withPlayers("A", "B")
                .withCardOnBattlefield(1, "Graveyard Mentor")
                .withCardOnBattlefield(1, "Graveyard Mentor")
                .withCardInGraveyard(1, "Ornithopter")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.actions().size shouldBe 1
            game.state = game.state.removeEntity(game.findPermanents("Graveyard Mentor").first())
            game.actions().size shouldBe 1
            game.execute(game.graveAction()).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 23
        }
        test("conditional zone grants use the source context") {
            val game = board("Conditional Mentor")
            game.actions() shouldBe emptyList()
            val source = game.findPermanent("Conditional Mentor")!!
            game.state = game.state.updateEntity(source) { it.with(com.wingedsheep.engine.state.components.battlefield.TappedComponent) }
            game.actions().size shouldBe 1
            game.execute(game.graveAction()).error shouldBe null
        }
        test("zone grants respect sorcery timing in offers and direct activation") {
            val game = board()
            game.state = game.state.copy(activePlayerId = game.player2Id)
            game.actions() shouldBe emptyList()
            (game.execute(game.graveAction()).error != null) shouldBe true
        }
        test("a moved recipient loses the grant immediately") {
            val game = board()
            val action = game.graveAction()
            val oldZone = game.state.logicalZone(action.sourceId)!!
            game.state = game.state.moveToZone(action.sourceId, oldZone, oldZone.copy(zoneType = Zone.EXILE))
            game.actions() shouldBe emptyList()
            (game.execute(action).error != null) shouldBe true
        }
    }
}
