package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.MultiplyTokenCreation
import com.wingedsheep.sdk.scripting.ReplaceTokenCreationWithToken
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A [ReplaceTokenCreationWithToken] that asks: optional, with a pick among several substitutes
 * ("you may instead create that many 2/2 green Cat creature tokens with haste or that many 3/1
 * green Dog creature tokens with vigilance" — Jinnie Fay, Jetmir's Second).
 *
 * - The replacement's controller picks Cats, Dogs or (optional) the original tokens, once per
 *   creation event; the count carries over ("that many"), a doubler having scaled it first.
 * - It applies to any token — a Treasure becomes a Cat.
 * - The substitutes keep the creating effect's riders (tapped) — the card's ruling.
 * - A batch creating several kinds of token at once is one event: one question, one answer.
 * - "Each player creates" fans out per player; only the replacement controller's tokens are asked
 *   about, and the players whose tokens already exist aren't given more on the re-run (and their
 *   tokens' enter events still go out).
 * - A CR 508.4 defender question for attacking substitutes re-runs the whole event, so the
 *   doubler and the substitution apply once.
 * - The question survives a paused game being serialized.
 */
class TokenSubstitutionChoiceTest : FunSpec({

    val catOrDog = card("Test Cat Or Dog") {
        typeLine = "Enchantment"
        replacementEffect(
            ReplaceTokenCreationWithToken(
                tokens = listOf(
                    Effects.CreateToken(2, 2, setOf(Color.GREEN), setOf("Cat"), setOf(Keyword.HASTE)),
                    Effects.CreateToken(3, 1, setOf(Color.GREEN), setOf("Dog"), setOf(Keyword.VIGILANCE))
                ),
                appliesTo = EventPattern.TokenCreationEvent(controller = Player.You),
                optional = true
            )
        )
    }
    val doubler = card("Test Token Doubler") {
        typeLine = "Enchantment"
        replacementEffect(MultiplyTokenCreation(appliesTo = EventPattern.TokenCreationEvent(controller = Player.You)))
    }
    val soldier = Effects.CreateToken(1, 1, setOf(Color.WHITE), setOf("Soldier"))

    fun spell(name: String, effect: Effect) = card(name) {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { this.effect = effect }
    }
    val twoSoldiers = spell("Test Two Soldiers", Effects.CreateToken(1, 1, setOf(Color.WHITE), setOf("Soldier"), count = 2))
    val tappedSoldier = spell("Test Tapped Soldier", soldier.copy(tapped = true))
    val treasure = spell("Test Treasure", Effects.CreateTreasure())
    val treasureAndSoldier = spell("Test Treasure And Soldier", Effects.CreateTokensTogether(Effects.CreateTreasure(), soldier))
    val eachPlayerSoldier = spell(
        "Test Each Player Soldier",
        soldier.copy(controller = EffectTarget.PlayerRef(Player.Each))
    )

    val warden = card("Test Arrival Warden") {
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.another(GameObjectFilter.Creature).enters()
            effect = Effects.GainLife(1)
        }
    }
    val captain = card("Test Choosing Captain") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Human Soldier"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.self.attacks()
            effect = Effects.CreateToken(
                power = 1, toughness = 1, colors = setOf(Color.RED), creatureTypes = setOf("Soldier"),
                tapped = true, attacking = true, attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
            )
        }
    }
    val walker = card("Test Defending Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }

    fun driver(vararg permanents: String): GameTestDriver = GameTestDriver().also { d ->
        d.registerCards(
            TestCards.all + listOf(
                catOrDog, doubler, twoSoldiers, tappedSoldier, treasure, treasureAndSoldier, eachPlayerSoldier,
                warden, captain, walker, PredefinedTokens.Treasure
            )
        )
        d.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        permanents.forEach { d.putPermanentOnBattlefield(d.player1, it) }
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Cast [spellName] and pass until it asks something or finishes resolving. */
    fun GameTestDriver.castAndResolve(spellName: String) {
        castSpell(player1, putCardInHand(player1, spellName)).error.shouldBeNull()
        var guard = 0
        while (state.stack.isNotEmpty() && pendingDecision == null && guard++ < 20) {
            passPriority(state.priorityPlayerId!!).error.shouldBeNull()
        }
    }

    fun GameTestDriver.choose(option: Int) {
        val decision = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(decision.playerId, OptionChosenResponse(decision.id, option)).error.shouldBeNull()
    }

    fun GameState.tokensOf(player: EntityId): List<EntityId> = getBattlefield().filter { id ->
        val c = getEntity(id) ?: return@filter false
        c.has<TokenComponent>() && c.get<ControllerComponent>()?.playerId == player
    }

    fun GameState.tokenTypes(player: EntityId): List<String> = tokensOf(player).map { id ->
        listOf("Cat", "Dog", "Soldier", "Treasure").single { projectedState.hasSubtype(id, it) }
    }

    test("the controller is asked once, with each substitute and the original as options") {
        val d = driver(catOrDog.name)
        d.castAndResolve(twoSoldiers.name)
        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe d.player1
        decision.options shouldBe listOf(
            "Create 2 2/2 green Cat creature tokens with haste",
            "Create 2 3/1 green Dog creature tokens with vigilance",
            "Create the original tokens"
        )
    }

    test("picking the first substitute creates that many of it") {
        val d = driver(catOrDog.name)
        d.castAndResolve(twoSoldiers.name)
        d.choose(0)
        d.pendingDecision.shouldBeNull()
        d.state.tokenTypes(d.player1) shouldBe listOf("Cat", "Cat")
        val cat = d.state.tokensOf(d.player1).first()
        d.state.projectedState.getPower(cat) shouldBe 2
        d.state.projectedState.hasKeyword(cat, Keyword.HASTE) shouldBe true
    }

    test("picking the second substitute creates that many of it") {
        val d = driver(catOrDog.name)
        d.castAndResolve(twoSoldiers.name)
        d.choose(1)
        d.state.tokenTypes(d.player1) shouldBe listOf("Dog", "Dog")
        d.state.projectedState.getPower(d.state.tokensOf(d.player1).first()) shouldBe 3
    }

    test("declining an optional substitution keeps the original tokens") {
        val d = driver(catOrDog.name)
        d.castAndResolve(twoSoldiers.name)
        d.choose(2)
        d.pendingDecision.shouldBeNull()
        d.state.tokenTypes(d.player1) shouldBe listOf("Soldier", "Soldier")
    }

    test("it applies to noncreature tokens: a Treasure becomes a Dog") {
        val d = driver(catOrDog.name)
        d.castAndResolve(treasure.name)
        d.choose(1)
        d.state.tokenTypes(d.player1) shouldBe listOf("Dog")
    }

    test("the creating effect's riders still apply to the substitutes") {
        val d = driver(catOrDog.name)
        d.castAndResolve(tappedSoldier.name)
        d.choose(0)
        val cat = d.state.tokensOf(d.player1).single()
        withClue("\"create a tapped token\" — the Cat enters tapped") {
            d.state.getEntity(cat)!!.has<TappedComponent>() shouldBe true
        }
    }

    test("a doubler scales the event first; the one question then covers the doubled count") {
        val d = driver(doubler.name, catOrDog.name)
        d.castAndResolve(twoSoldiers.name)
        d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>().options.first() shouldBe
            "Create 4 2/2 green Cat creature tokens with haste"
        d.choose(0)
        d.pendingDecision.shouldBeNull()
        d.state.tokenTypes(d.player1) shouldBe List(4) { "Cat" }
    }

    test("a batch of different tokens is one event: one question, every token replaced") {
        val d = driver(catOrDog.name)
        d.castAndResolve(treasureAndSoldier.name)
        d.choose(1)
        d.pendingDecision.shouldBeNull()
        d.state.tokenTypes(d.player1) shouldBe listOf("Dog", "Dog")
    }

    test("an opponent's tokens are neither asked about nor replaced") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, catOrDog.name)
        d.castAndResolve(twoSoldiers.name)
        d.pendingDecision.shouldBeNull()
        d.state.tokenTypes(d.player1) shouldBe listOf("Soldier", "Soldier")
    }

    test("each player creating a token: only the controller's is asked about, nobody gets extras") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, catOrDog.name)
        d.castAndResolve(eachPlayerSoldier.name)
        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe d.player2
        d.choose(0)
        d.pendingDecision.shouldBeNull()
        d.state.tokenTypes(d.player1) shouldBe listOf("Soldier")
        d.state.tokenTypes(d.player2) shouldBe listOf("Cat")
    }

    test("the tokens made before another player's question still announce they entered") {
        val d = driver(warden.name)
        d.putPermanentOnBattlefield(d.player2, catOrDog.name)
        d.castAndResolve(eachPlayerSoldier.name)
        d.choose(0)
        var guard = 0
        while (d.state.stack.isNotEmpty() && d.pendingDecision == null && guard++ < 20) {
            d.passPriority(d.state.priorityPlayerId!!).error.shouldBeNull()
        }
        withClue("the warden sees player 1's Soldier (made before the question) and player 2's Cat") {
            d.getLifeTotal(d.player1) shouldBe 22
        }
    }

    test("a defender question for attacking substitutes re-runs the event once: doubled once, asked once") {
        val d = driver(doubler.name, catOrDog.name)
        val walkerId = d.putPermanentOnBattlefield(d.player2, walker.name).also { pw ->
            d.replaceState(d.state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3)) })
        }
        val attacker = d.putCreatureOnBattlefield(d.player1, captain.name).also { d.removeSummoningSickness(it) }
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).error.shouldBeNull()
        var guard = 0
        while (d.pendingDecision == null && d.state.stack.isNotEmpty() && guard++ < 20) {
            d.passPriority(d.state.priorityPlayerId!!).error.shouldBeNull()
        }
        d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>().options.first() shouldBe
            "Create 2 2/2 green Cat creature tokens with haste"
        d.choose(0)
        val defenders = listOf(d.player2, walkerId)
        repeat(2) { i ->
            val question = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            d.submitTargetSelection(question.playerId, listOf(defenders[i])).error.shouldBeNull()
        }
        withClue("no second substitution question and no further defender questions") {
            d.pendingDecision.shouldBeNull()
        }
        val cats = d.state.tokensOf(d.player1)
        d.state.tokenTypes(d.player1) shouldBe listOf("Cat", "Cat")
        cats.map { d.state.getEntity(it)!!.get<AttackingComponent>()?.defenderId } shouldBe defenders
        cats.all { d.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
    }

    test("the paused question round-trips through serialization") {
        val d = driver(catOrDog.name)
        d.castAndResolve(twoSoldiers.name)
        val json = kotlinx.serialization.json.Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
        val restored = json.decodeFromString(GameState.serializer(), json.encodeToString(GameState.serializer(), d.state))
        restored.continuationStack shouldBe d.state.continuationStack
        restored.pendingDecision shouldBe d.state.pendingDecision
    }
})
