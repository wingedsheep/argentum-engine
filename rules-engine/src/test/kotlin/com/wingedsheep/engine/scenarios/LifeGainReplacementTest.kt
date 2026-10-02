package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.GameState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.player.LifeGainedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.CantGainLifeComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyLifeGain
import com.wingedsheep.sdk.scripting.ReplaceLifeGainWith
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe

/** End-to-end life replacement execution, ordering, and continuation boundaries. */
class LifeGainReplacementTest : ScenarioTestBase() {
    private val amount = DynamicAmounts.replacementLifeGainAmount()
    private val recipient = EffectTarget.PlayerRef(Player.TriggeringPlayer)
    private fun conversion(effect: Effect = Effects.DrawCards(amount, recipient), player: Player = Player.Each) =
        ReplaceLifeGainWith(effect, EventPattern.LifeGainEvent(player))
    private fun enchantment(name: String, replacement: com.wingedsheep.sdk.scripting.ReplacementEffect) = card(name) {
        manaCost = "{0}"
        typeLine = "Enchantment"
        replacementEffect(replacement)
    }
    private fun spell(name: String, body: Effect) = card(name) {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = body }
    }
    private val tappedOnly = enchantment("Test Tapped Life Conversion", conversion().copy(
        restrictions = listOf(Conditions.SourceIsTapped)))
    private val controllerRestricted = enchantment("Test Controller Restricted Life Conversion", conversion().copy(
        restrictions = listOf(Conditions.YouControl(GameObjectFilter.Creature))))
    private val firstOnly = enchantment("Test First Life Conversion", conversion().copy(
        appliesTo = EventPattern.LifeGainEvent(Player.Each, firstTimeEachTurn = true)))
    private val drawReplacement = enchantment("Test Life Into Draw", conversion())
    private val ownerDraw = enchantment("Test Opponent Into Owner Draw", conversion(Effects.DrawCards(amount), Player.EachOpponent))
    private val doubler = enchantment("Test Life Doubler", ModifyLifeGain(multiplier = 2))
    private val plusOne = enchantment("Test Life Plus One", ModifyLifeGain(multiplier = 1, modifier = 1))
    private val recursive = enchantment("Test Recursive Life", conversion(Effects.GainLife(amount, recipient)))
    private val chooser = enchantment("Test Life Choice", conversion(Effects.Composite(listOf(
        Effects.ChooseOption(OptionType.COLOR), Effects.DrawCards(amount, recipient)
    ))))
    private val drawThenGain = enchantment("Test Life Draw Then Gain", conversion(Effects.Composite(listOf(
        Effects.ChooseOption(OptionType.COLOR),
        Effects.DrawCards(amount, recipient),
        Effects.GainLife(amount, recipient)
    ))))
    private val eachThenCount = spell("Test Each Gain Then Count", Effects.Composite(listOf(
        Effects.GainLife(3, EffectTarget.PlayerRef(Player.Each)),
        Effects.LoseLife(DynamicAmounts.count(Player.You, Zone.HAND), EffectTarget.Controller)
    )))
    private val drawIntoLife = enchantment("Test Draw Into Life", ReplaceDrawWith(Effects.GainLife(3)))
    private val drawIntoLoss = enchantment("Test Draw Into Loss", ReplaceDrawWith(Effects.LoseLife(1, EffectTarget.Controller)))
    private val drawTwice = spell("Test Independent Draws", Effects.Composite(listOf(
        Effects.DrawCards(1), Effects.DrawCards(1)
    )))
    private val gainThree = spell("Test Gain Three", Effects.GainLife(3))
    private val gainOpponent = spell("Test Opponent Gains", Effects.GainLife(3, EffectTarget.PlayerRef(Player.EachOpponent)))
    private val consecutive = spell("Test Separate Gains", Effects.Composite(listOf(Effects.GainLife(2), Effects.GainLife(3))))
    private val drawThenCount = spell("Test Gain Then Count Hand", Effects.Composite(listOf(
        Effects.GainLife(3), Effects.LoseLife(DynamicAmounts.count(Player.You, Zone.HAND), EffectTarget.Controller)
    )))
    private val setLife = spell("Test Set Life Higher", Effects.SetLifeTotal(23))
    private val erase = card("Test Erase Life Ability") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val permanent = target(TargetFilter.Permanent); effect = Effects.RemoveAllAbilities(permanent) }
    }
    private val steal = card("Test Steal Life Ability") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val permanent = target(TargetFilter.Permanent); effect = Effects.GainControl(permanent) }
    }
    private val grant = spell("Test Grant Life Conversion", Effects.GrantReplacementEffect(conversion(), duration = Duration.EndOfTurn))
    private val lifelinkBurn = card("Test Lifelink Burn") {
        manaCost = "{0}"
        typeLine = "Instant"
        keywords(Keyword.LIFELINK)
        spell { effect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.EachOpponent)) }
    }
    private val lifelinkBear = card("Test Conversion Lifelink Bear") {
        manaCost = "{0}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.LIFELINK)
    }
    private val zero = spell("Test Gain Zero", Effects.GainLife(0))
    private val negative = spell("Test Gain Negative", Effects.GainLife(-3))

    private fun board(replacements: List<String>, spell: String, extraSpells: List<String> = emptyList()): TestGame {
        val builder = scenario().withPlayers("Player1", "Player2")
            .withCardInHand(1, spell)
            .withActivePlayer(1).withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        replacements.forEach { builder.withCardOnBattlefield(1, it) }
        extraSpells.forEach { builder.withCardInHand(1, it) }
        repeat(20) {
            builder.withCardInLibrary(1, "Island")
            builder.withCardInLibrary(2, "Island")
        }
        return builder.build()
    }
    private fun resolve(game: TestGame, spell: String) {
        game.castSpell(1, spell).error shouldBe null
        game.resolveStack()
    }
    private fun choose(game: TestGame, name: String) {
        val decision = game.state.pendingDecision as ChooseOptionDecision
        val index = decision.options.indexOfFirst { it.startsWith(name) }
        (index >= 0) shouldBe true
        game.submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
        game.resolveStack()
    }

    init {
        listOf(drawReplacement, ownerDraw, doubler, plusOne, recursive, chooser, gainThree, gainOpponent,
            consecutive, drawThenCount, setLife, erase, steal, grant, zero, negative, lifelinkBurn, lifelinkBear, drawThenGain, eachThenCount, tappedOnly, firstOnly, controllerRestricted, drawIntoLife, drawIntoLoss, drawTwice).forEach(cardRegistry::register)

        for (competing in listOf(false, true)) {
            test("paused life conversion restores the parent draw chain only until that draw completes with competing=$competing") {
                val replacements = listOf(drawIntoLife.name, drawThenGain.name) +
                    if (competing) listOf(drawIntoLoss.name) else emptyList()
                val game = board(replacements, drawTwice.name)
                resolve(game, drawTwice.name)
                if (competing) choose(game, drawIntoLife.name)
                val first = game.state.pendingDecision as ChooseOptionDecision
                game.submitDecision(OptionChosenResponse(first.id, 0)).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe if (competing) 20 else 23
                game.handSize(1) shouldBe if (competing) 0 else 3
                if (competing) choose(game, drawIntoLife.name)
                val second = game.state.pendingDecision as ChooseOptionDecision
                game.submitDecision(OptionChosenResponse(second.id, 0)).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe if (competing) 20 else 26
                game.handSize(1) shouldBe if (competing) 0 else 6
                game.state.activeReplacementChain shouldBe null
                game.state.pendingDecision shouldBe null
            }
        }

        for (gainSource in listOf(gainThree, lifelinkBurn)) {
            for (sourceControlsCreature in listOf(false, true)) {
                test("${gainSource.name} binds replacement restrictions to its source controller when source controls creature=$sourceControlsCreature") {
                    val builder = scenario().withPlayers("Player1", "Player2")
                        .withCardOnBattlefield(1, controllerRestricted.name)
                        .withCardOnBattlefield(if (sourceControlsCreature) 1 else 2, lifelinkBear.name)
                        .withCardInHand(2, gainSource.name)
                        .withActivePlayer(2).withPriorityPlayer(2)
                        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    repeat(10) { builder.withCardInLibrary(2, "Island") }
                    val game = builder.build()
                    game.castSpell(2, gainSource.name).error shouldBe null
                    game.resolveStack()
                    game.getLifeTotal(2) shouldBe if (sourceControlsCreature) 20 else 23
                    game.handSize(2) shouldBe if (sourceControlsCreature) 3 else 0
                    game.state.pendingDecision shouldBe null
                }
            }
        }

        test("replacement happens before the next instruction and emits no life gain") {
            val game = board(listOf(drawReplacement.name), drawThenCount.name)
            resolve(game, drawThenCount.name)
            game.handSize(1) shouldBe 3
            game.getLifeTotal(1) shouldBe 17
            game.state.pendingDecision shouldBe null
        }
        for (noop in listOf(zero, negative)) {
            test("${noop.name} does not create a replacement event") {
                val game = board(listOf(drawReplacement.name), noop.name)
                resolve(game, noop.name)
                game.handSize(1) shouldBe 0
                game.getLifeTotal(1) shouldBe 20
            }
        }
        test("global replacement draws for the recipient rather than its controller") {
            val game = board(listOf(drawReplacement.name), gainOpponent.name)
            resolve(game, gainOpponent.name)
            game.handSize(1) shouldBe 0
            game.handSize(2) shouldBe 3
            game.getLifeTotal(2) shouldBe 20
        }
        test("opponent scope and explicit controller draw keep their distinct player bindings") {
            val game = board(listOf(ownerDraw.name), gainOpponent.name)
            resolve(game, gainOpponent.name)
            game.handSize(1) shouldBe 3
            game.handSize(2) shouldBe 0
            game.getLifeTotal(2) shouldBe 20
            val ownGain = board(listOf(ownerDraw.name), gainThree.name)
            resolve(ownGain, gainThree.name)
            ownGain.getLifeTotal(1) shouldBe 23
            ownGain.handSize(1) shouldBe 0
        }
        test("a player who cannot gain life does not execute the replacement") {
            val game = board(listOf(drawReplacement.name, doubler.name), gainThree.name)
            game.state = game.state.updateEntity(game.player1Id) { it.with(CantGainLifeComponent()) }
            resolve(game, gainThree.name)
            game.handSize(1) shouldBe 0
            game.getLifeTotal(1) shouldBe 20
            game.state.pendingDecision shouldBe null
        }
        for ((first, expected) in listOf(drawReplacement.name to 3, doubler.name to 6)) {
            test("choosing $first first draws $expected cards") {
                val game = board(listOf(drawReplacement.name, doubler.name), gainThree.name)
                resolve(game, gainThree.name)
                choose(game, first)
                game.handSize(1) shouldBe expected
                game.getLifeTotal(1) shouldBe 20
                game.state.pendingDecision shouldBe null
            }
        }
        test("two modifiers preserve amount and ordering across successive choices") {
            val game = board(listOf(drawReplacement.name, doubler.name, plusOne.name), gainThree.name)
            resolve(game, gainThree.name)
            choose(game, plusOne.name)
            choose(game, doubler.name)
            game.handSize(1) shouldBe 8
            game.getLifeTotal(1) shouldBe 20
            game.state.pendingDecision shouldBe null
        }
        test("independent gains each apply the same replacement") {
            val game = board(listOf(drawReplacement.name), consecutive.name)
            resolve(game, consecutive.name)
            game.handSize(1) shouldBe 5
            game.getLifeTotal(1) shouldBe 20
        }
        test("replacement-generated life gain does not reapply the same replacement") {
            val game = board(listOf(recursive.name), consecutive.name)
            resolve(game, consecutive.name)
            game.getLifeTotal(1) shouldBe 25
            game.state.pendingDecision shouldBe null
        }
        test("a choice in the replacement preserves its amount and the outer spell continuation") {
            val game = board(listOf(chooser.name), drawThenCount.name)
            resolve(game, drawThenCount.name)
            game.getLifeTotal(1) shouldBe 20
            val decision = game.state.pendingDecision as ChooseOptionDecision
            game.submitDecision(OptionChosenResponse(decision.id, 0)).error shouldBe null
            game.resolveStack()
            game.handSize(1) shouldBe 3
            game.getLifeTotal(1) shouldBe 17
            game.state.pendingDecision shouldBe null
        }
        test("setting life higher replaces the gained difference") {
            val game = board(listOf(drawReplacement.name), setLife.name)
            resolve(game, setLife.name)
            game.getLifeTotal(1) shouldBe 20
            game.handSize(1) shouldBe 3
        }
        test("paused multi-recipient replacement resumes each recipient and the next spell instruction") {
            val game = board(listOf(chooser.name), eachThenCount.name)
            resolve(game, eachThenCount.name)
            val first = game.state.pendingDecision as ChooseOptionDecision
            first.playerId shouldBe game.player1Id
            game.handSize(1) shouldBe 0
            game.handSize(2) shouldBe 0
            game.submitDecision(OptionChosenResponse(first.id, 0)).error shouldBe null
            game.resolveStack()
            val second = game.state.pendingDecision as ChooseOptionDecision
            second.playerId shouldBe game.player1Id
            game.handSize(1) shouldBe 3
            game.handSize(2) shouldBe 0
            game.getLifeTotal(1) shouldBe 20
            game.submitDecision(OptionChosenResponse(second.id, 0)).error shouldBe null
            game.resolveStack()
            game.handSize(1) shouldBe 3
            game.handSize(2) shouldBe 3
            game.getLifeTotal(1) shouldBe 17
            game.getLifeTotal(2) shouldBe 20
            game.state.pendingDecision shouldBe null
        }
        test("the life replacement chain survives its choice and all contained draws") {
            val game = board(listOf(drawThenGain.name), consecutive.name)
            resolve(game, consecutive.name)
            val first = game.state.pendingDecision as ChooseOptionDecision
            game.submitDecision(OptionChosenResponse(first.id, 0)).error shouldBe null
            game.resolveStack()
            // The generated gain of two does not restart the conversion. The second question
            // belongs to the independent next instruction, whose amount must be three.
            game.getLifeTotal(1) shouldBe 22
            game.handSize(1) shouldBe 2
            val second = game.state.pendingDecision as ChooseOptionDecision
            game.submitDecision(OptionChosenResponse(second.id, 0)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 25
            game.handSize(1) shouldBe 5
            game.state.pendingDecision shouldBe null
        }
        test("a serialized competing life event preserves its amount and continuations") {
            val game = board(listOf(drawReplacement.name, doubler.name, plusOne.name), drawThenCount.name)
            val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
            resolve(game, drawThenCount.name)
            game.state = json.decodeFromString<GameState>(json.encodeToString(game.state))
            choose(game, plusOne.name)
            game.state = json.decodeFromString<GameState>(json.encodeToString(game.state))
            choose(game, doubler.name)
            game.handSize(1) shouldBe 8
            game.getLifeTotal(1) shouldBe 12
            game.state.pendingDecision shouldBe null
        }
        test("a serialized queued lifelink rider preserves the replacement chain inside its draws") {
            val game = board(listOf(drawThenGain.name), lifelinkBurn.name)
            val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
            resolve(game, lifelinkBurn.name)
            val decision = game.state.pendingDecision as ChooseOptionDecision
            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 17
            game.state = json.decodeFromString<GameState>(json.encodeToString(game.state))
            game.submitDecision(OptionChosenResponse(decision.id, 0)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 23
            game.handSize(1) shouldBe 3
            game.state.pendingDecision shouldBe null
        }
        test("noncombat lifelink executes the replacement without changing the gained player's life") {
            val game = board(listOf(drawReplacement.name), lifelinkBurn.name)
            resolve(game, lifelinkBurn.name)
            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 17
            game.handSize(1) shouldBe 3
        }
        test("combat lifelink executes the replacement after dealing damage") {
            val game = board(listOf(drawReplacement.name, lifelinkBear.name), zero.name)
            resolve(game, zero.name)
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf(lifelinkBear.name to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 18
            game.handSize(1) shouldBe 2
        }
        test("losing all abilities disables a printed replacement") {
            val game = board(listOf(drawReplacement.name), erase.name, listOf(gainThree.name))
            game.castSpell(1, erase.name, game.findPermanent(drawReplacement.name)!!).error shouldBe null
            game.resolveStack()
            game.state.projectedState.hasLostAllAbilities(game.findPermanent(drawReplacement.name)!!) shouldBe true
            resolve(game, gainThree.name)
            game.getLifeTotal(1) shouldBe 23
            game.handSize(1) shouldBe 0
        }
        test("a granted replacement survives the spell that granted it leaving the stack") {
            val game = board(emptyList(), grant.name, listOf(gainThree.name))
            resolve(game, grant.name)
            game.isInGraveyard(1, grant.name) shouldBe true
            resolve(game, gainThree.name)
            game.getLifeTotal(1) shouldBe 20
            game.handSize(1) shouldBe 3
        }
        test("source-relative restrictions read the replacement source's tapped state") {
            for (tapped in listOf(false, true)) {
                val game = board(listOf(tappedOnly.name), gainThree.name)
                if (tapped) {
                    game.state = game.state.updateEntity(game.findPermanent(tappedOnly.name)!!) {
                        it.with(TappedComponent)
                    }
                }
                resolve(game, gainThree.name)
                game.getLifeTotal(1) shouldBe if (tapped) 20 else 23
                game.handSize(1) shouldBe if (tapped) 3 else 0
            }
        }
        test("a first-time scope does not replace a player who has already gained life this turn") {
            for (alreadyGained in listOf(false, true)) {
                val game = board(listOf(firstOnly.name), gainThree.name)
                if (alreadyGained) {
                    game.state = game.state.updateEntity(game.player1Id) { it.with(LifeGainedThisTurnComponent) }
                }
                resolve(game, gainThree.name)
                game.getLifeTotal(1) shouldBe if (alreadyGained) 23 else 20
                game.handSize(1) shouldBe if (alreadyGained) 0 else 3
            }
        }
        test("an end-of-turn granted replacement expires before the next turn") {
            val game = board(emptyList(), grant.name, listOf(gainThree.name))
            resolve(game, grant.name)
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id
            val handBefore = game.handSize(1)
            resolve(game, gainThree.name)
            game.getLifeTotal(1) shouldBe 23
            game.handSize(1) shouldBe handBefore - 1
        }
        test("a printed replacement follows the permanent's projected controller") {
            // The enchantment initially belongs to the opponent; theft changes only projected control.
            val other = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(2, ownerDraw.name)
                .withCardInHand(1, steal.name).withCardInHand(1, gainOpponent.name)
                .withActivePlayer(1).withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(10) { other.withCardInLibrary(1, "Island") }
            val stolen = other.build()
            val source = stolen.findPermanent(ownerDraw.name)!!
            stolen.castSpell(1, steal.name, source).error shouldBe null
            stolen.resolveStack()
            stolen.state.projectedState.getController(source) shouldBe stolen.player1Id
            resolve(stolen, gainOpponent.name)
            stolen.getLifeTotal(2) shouldBe 20
            stolen.handSize(1) shouldBe 3
        }
    }
}
