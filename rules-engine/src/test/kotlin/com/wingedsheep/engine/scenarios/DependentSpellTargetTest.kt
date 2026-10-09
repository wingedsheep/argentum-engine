package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.legalactions.utils.TargetEnumerationUtils
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DependentSpellTargetTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
    }
    val predicates = PredicateEvaluator(cardRegistry = null)
    val utils = TargetEnumerationUtils(predicates)
    val requirements = listOf(
        TargetPlayer(id = "player"),
        TargetObject(filter = TargetFilter(GameObjectFilter.Land.targetPlayerControls(EffectTarget.BoundVariable("player"))))
    )
    test("named player dependency offers a distinct legal branch per player") {
        val d = setup()
        val mine = d.putLandOnBattlefield(d.player1, "Forest")
        val theirs = d.putLandOnBattlefield(d.player2, "Mountain")
        val infos = utils.buildTargetInfos(d.state, d.player1, requirements, targetingSourceType = TargetingSourceType.SPELL)
        infos[1].validTargetsByPrefix shouldBe mapOf(d.player1.toString() to listOf(mine), d.player2.toString() to listOf(theirs))
        val validator = TargetValidator(predicates)
        validator.validateTargets(d.state, listOf(ChosenTarget.Player(d.player2), ChosenTarget.Permanent(theirs)), requirements,
            d.player1, targetingSourceType = TargetingSourceType.SPELL) shouldBe null
        (validator.validateTargets(d.state, listOf(ChosenTarget.Player(d.player2), ChosenTarget.Permanent(mine)), requirements,
            d.player1, targetingSourceType = TargetingSourceType.SPELL) != null) shouldBe true
    }
    test("lookahead removes players without any matching land") {
        val d = setup()
        d.putLandOnBattlefield(d.player2, "Forest")
        utils.buildTargetInfos(d.state, d.player1, requirements, targetingSourceType = TargetingSourceType.SPELL)[0].validTargets shouldBe listOf(d.player2)
    }
    test("independent targets do not allocate prefix tables") {
        val d = setup()
        val infos = utils.buildTargetInfos(d.state, d.player1, listOf(TargetPlayer(), TargetPlayer()))
        infos.all { it.validTargetsByPrefix == null } shouldBe true
    }
    test("spell enumeration does not inherit triggered ability restrictions") {
        val d = setup()
        val land = d.putLandOnBattlefield(d.player2, "Forest")
        d.replaceState(d.state.updateEntity(land) { it.with(
            com.wingedsheep.engine.state.components.battlefield.CantBeTargetedByOpponentAbilitiesComponent()
        ) })
        val spell = utils.buildTargetInfos(d.state, d.player1, requirements, targetingSourceType = TargetingSourceType.SPELL)
        spell[1].validTargets shouldBe listOf(land)
        val ability = utils.buildTargetInfos(d.state, d.player1, requirements, targetingSourceType = TargetingSourceType.TRIGGERED_ABILITY)
        ability[0].validTargets shouldBe emptyList()
    }
    test("dependent target DTO survives serialization including its prefix keys") {
        val dto = com.wingedsheep.engine.view.LegalActionTargetInfo(
            index = 1, description = "land", minTargets = 1, maxTargets = 1,
            validTargets = listOf(com.wingedsheep.sdk.model.EntityId("land")),
            validTargetsByPrefix = mapOf("player" to listOf(com.wingedsheep.sdk.model.EntityId("land"))),
        )
        val serializer = com.wingedsheep.engine.view.LegalActionTargetInfo.serializer()
        val json = kotlinx.serialization.json.Json
        json.decodeFromString(serializer, json.encodeToString(serializer, dto)) shouldBe dto
    }
    test("resolution drops newly shrouded or opposing hexproof players but permits own hexproof") {
        val d = setup()
        val resolver = com.wingedsheep.engine.mechanics.stack.ResolutionTargetValidator(predicates)
        val targets = listOf(ChosenTarget.Player(d.player1), ChosenTarget.Player(d.player2))
        val reqs = listOf(TargetPlayer(), TargetPlayer())
        resolver.validateTargets(d.state, targets, controllerId = d.player1,
            targetRequirements = listOf(TargetOpponent(), TargetOpponent())) shouldBe listOf(targets[1])
        val hexproof = com.wingedsheep.engine.state.components.player.PlayerHexproofComponent()
        val hexState = d.state.updateEntity(d.player1) { it.with(hexproof) }
            .updateEntity(d.player2) { it.with(hexproof) }
        resolver.validateTargets(hexState, targets, controllerId = d.player1, targetRequirements = reqs) shouldBe listOf(targets[0])
        val shroudState = hexState.updateEntity(d.player1) { it.with(
            com.wingedsheep.engine.state.components.player.PlayerShroudComponent()
        ) }
        resolver.validateTargets(shroudState, targets, controllerId = d.player1, targetRequirements = reqs) shouldBe emptyList()
        val protectedState = d.state.updateEntity(d.player2) { it.with(
            com.wingedsheep.engine.state.components.player.PlayerProtectionComponent(
                scopes = listOf(com.wingedsheep.sdk.scripting.ProtectionScope.Everything)
            )
        ) }
        resolver.validateTargets(protectedState, targets, controllerId = d.player1, targetRequirements = reqs) shouldBe listOf(targets[0])
    }
})
