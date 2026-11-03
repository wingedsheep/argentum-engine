package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.MultiplyTokenCreation
import com.wingedsheep.sdk.scripting.RedirectTokenCreationController
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [RedirectTokenCreationController] — "each token that would be created under an opponent's
 * control is created under your control instead" (Crafty Cutpurse). The rulings are the spec:
 *
 * - The new controller is also the owner.
 * - It applies before every other token replacement: an opponent's doubler no longer applies, the
 *   redirecting player's own does.
 * - Every token-creation path is redirected — creature tokens, predefined tokens and investigate,
 *   token copies — and tokens already on the battlefield aren't touched.
 * - A "tapped and attacking" token whose new controller isn't the attacking player enters tapped,
 *   not attacking.
 * - Delayed triggers the creating effect sets up stay with that effect's controller, so a
 *   "sacrifice it at the beginning of the next end step" does nothing to the stolen token.
 * - Granted for the turn, it ends in the cleanup step.
 * - Several redirects: the would-be controller picks one, the new would-be controller picks among
 *   the rest, each applies once. Two players' redirects in a two-player game cancel out with no
 *   question; in a three-player game where the order decides, the would-be controller is asked.
 */
class TokenControllerRedirectTest : FunSpec({

    val redirector = card("Test Token Redirector") {
        typeLine = "Enchantment"
        replacementEffect(RedirectTokenCreationController())
    }
    val doubler = card("Test Redirect Doubler") {
        typeLine = "Enchantment"
        replacementEffect(MultiplyTokenCreation(appliesTo = EventPattern.TokenCreationEvent(controller = Player.You)))
    }
    val cutpurse = card("Test Granting Cutpurse") {
        manaCost = "{0}"
        typeLine = "Creature — Human Pirate"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.GrantReplacementEffect(
                RedirectTokenCreationController(), EffectTarget.Self, Duration.EndOfTurn
            )
        }
    }
    val soldier = Effects.CreateToken(1, 1, setOf(Color.WHITE), setOf("Soldier"))

    fun spell(name: String, effect: Effect) = card(name) {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { this.effect = effect }
    }
    val twoSoldiers = spell("Test Redirect Two Soldiers", soldier.copy(count = com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed(2)))
    val opponentsSoldier = spell(
        "Test Redirect Opponent Soldier",
        soldier.copy(controller = EffectTarget.PlayerRef(Player.EachOpponent))
    )
    val treasure = spell("Test Redirect Treasure", Effects.CreateTreasure())
    val investigate = spell("Test Redirect Investigate", Effects.Investigate())
    val batch = spell("Test Redirect Batch", Effects.CreateTokensTogether(Effects.CreateTreasure(), soldier))
    val mobilized = spell("Test Redirect Mobilized Soldier", soldier.copy(sacrificeAtStep = Step.END))
    val copyCreature = card("Test Redirect Copy") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.CreateTokenCopyOfTarget(creature)
        }
    }
    val captain = card("Test Redirect Captain") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Human Soldier"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.self.attacks()
            effect = soldier.copy(tapped = true, attacking = true)
        }
    }

    val testCards = listOf(
        redirector, doubler, cutpurse, twoSoldiers, opponentsSoldier, treasure, investigate, batch,
        mobilized, copyCreature, captain, PredefinedTokens.Treasure, PredefinedTokens.Clue
    )

    fun driver(): GameTestDriver = GameTestDriver().also { d ->
        d.registerCards(TestCards.all + testCards)
        d.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && pendingDecision == null && guard++ < 20) {
            passPriority(state.priorityPlayerId!!).error.shouldBeNull()
        }
    }

    fun GameTestDriver.castAndResolve(caster: EntityId, spellName: String, targets: List<EntityId> = emptyList()) {
        castSpell(caster, putCardInHand(caster, spellName), targets).error.shouldBeNull()
        resolveStack()
    }

    fun GameState.tokensOf(player: EntityId): List<EntityId> = getBattlefield().filter { id ->
        val c = getEntity(id) ?: return@filter false
        c.has<TokenComponent>() && c.get<ControllerComponent>()?.playerId == player
    }

    fun GameState.ownerOf(id: EntityId): EntityId? = getEntity(id)?.get<CardComponent>()?.ownerId

    test("an opponent's token is created under the redirecting player's control, and owned by them") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, redirector.name)
        d.castAndResolve(d.player1, twoSoldiers.name)
        d.state.tokensOf(d.player1).shouldBeEmpty()
        val tokens = d.state.tokensOf(d.player2)
        tokens.size shouldBe 2
        tokens.forEach { d.state.ownerOf(it) shouldBe d.player2 }
    }

    test("the redirecting player's own tokens are unaffected") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, redirector.name)
        d.castAndResolve(d.player1, twoSoldiers.name)
        d.state.tokensOf(d.player1).size shouldBe 2
        d.state.tokensOf(d.player2).shouldBeEmpty()
    }

    test("tokens an effect creates for an opponent of the redirecting player come back to them") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, redirector.name)
        d.castAndResolve(d.player1, opponentsSoldier.name)
        d.state.tokensOf(d.player2).shouldBeEmpty()
        d.state.tokensOf(d.player1).size shouldBe 1
    }

    test("predefined tokens are redirected: a Treasure") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, redirector.name)
        d.castAndResolve(d.player1, treasure.name)
        val token = d.state.tokensOf(d.player2).single()
        d.state.projectedState.hasSubtype(token, "Treasure") shouldBe true
        d.state.ownerOf(token) shouldBe d.player2
    }

    test("investigating creates the Clue under the redirecting player; the investigator still investigated") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, redirector.name)
        d.castAndResolve(d.player1, investigate.name)
        d.state.tokensOf(d.player1).shouldBeEmpty()
        d.state.projectedState.hasSubtype(d.state.tokensOf(d.player2).single(), "Clue") shouldBe true
        d.events.any { it is com.wingedsheep.engine.core.InvestigatedEvent && it.playerId == d.player1 } shouldBe true
    }

    test("a batch of tokens created together is one redirected event") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, redirector.name)
        d.castAndResolve(d.player1, batch.name)
        d.state.tokensOf(d.player1).shouldBeEmpty()
        d.state.tokensOf(d.player2).size shouldBe 2
    }

    test("token copies are redirected") {
        val d = driver()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.putPermanentOnBattlefield(d.player2, redirector.name)
        d.castAndResolve(d.player1, copyCreature.name, listOf(courser))
        val copy = d.state.tokensOf(d.player2).single()
        d.state.getEntity(copy)!!.get<CardComponent>()!!.name shouldBe "Centaur Courser"
        d.state.ownerOf(copy) shouldBe d.player2
        d.state.tokensOf(d.player1).shouldBeEmpty()
    }

    test("the creator's own doubler no longer applies once the tokens are redirected") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, doubler.name)
        d.putPermanentOnBattlefield(d.player2, redirector.name)
        d.castAndResolve(d.player1, twoSoldiers.name)
        withClue("redirect first: the creator's Anointed Procession sees tokens under its opponent") {
            d.state.tokensOf(d.player2).size shouldBe 2
        }
        d.state.tokensOf(d.player1).shouldBeEmpty()
    }

    test("the redirecting player's own doubler does apply to the redirected tokens") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, doubler.name)
        d.putPermanentOnBattlefield(d.player2, redirector.name)
        d.castAndResolve(d.player1, twoSoldiers.name)
        d.state.tokensOf(d.player2).size shouldBe 4
    }

    test("tokens already on the battlefield keep their controller") {
        val d = driver()
        d.castAndResolve(d.player1, twoSoldiers.name)
        val existing = d.state.tokensOf(d.player1)
        d.putPermanentOnBattlefield(d.player2, redirector.name)
        d.state.tokensOf(d.player1) shouldBe existing
    }

    test("an attacking player's \"tapped and attacking\" token redirected to the defender enters tapped, not attacking") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, redirector.name)
        val attacker = d.putCreatureOnBattlefield(d.player1, captain.name).also { d.removeSummoningSickness(it) }
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).error.shouldBeNull()
        d.resolveStack()
        val token = d.state.tokensOf(d.player2).single()
        val container = d.state.getEntity(token)!!
        container.has<TappedComponent>() shouldBe true
        container.get<AttackingComponent>().shouldBeNull()
    }

    test("a delayed trigger from the creating effect stays with its controller, who can't sacrifice the stolen token") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, redirector.name)
        d.castAndResolve(d.player1, mobilized.name)
        val token = d.state.tokensOf(d.player2).single()
        d.state.delayedTriggers.single().controllerId shouldBe d.player1
        d.passPriorityUntil(Step.END)
        d.resolveStack()
        withClue("player 1 controls the \"sacrifice it\" trigger but not the token, so it stays") {
            d.state.getBattlefield().contains(token) shouldBe true
        }
    }

    test("a granted redirect lasts the turn and ends in the cleanup step") {
        val d = driver()
        d.castAndResolve(d.player1, cutpurse.name)
        d.state.grantedReplacementEffects.size shouldBe 1
        d.castAndResolve(d.player1, opponentsSoldier.name)
        withClue("this turn, the Soldier player 2 would get is created under player 1") {
            d.state.tokensOf(d.player1).size shouldBe 1
            d.state.tokensOf(d.player2).shouldBeEmpty()
        }
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.player2)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.player1)
        d.state.grantedReplacementEffects.shouldBeEmpty()
        d.castAndResolve(d.player1, opponentsSoldier.name)
        d.state.tokensOf(d.player2).size shouldBe 1
    }

    test("two players' redirects in a two-player game cancel out, with no question") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, redirector.name)
        d.putPermanentOnBattlefield(d.player2, redirector.name)
        d.castAndResolve(d.player1, twoSoldiers.name)
        d.pendingDecision.shouldBeNull()
        d.state.tokensOf(d.player1).size shouldBe 2
        d.state.tokensOf(d.player2).shouldBeEmpty()
    }

    test("two redirects from one player apply once between them") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, redirector.name)
        d.putPermanentOnBattlefield(d.player2, redirector.name)
        d.castAndResolve(d.player1, twoSoldiers.name)
        d.pendingDecision.shouldBeNull()
        d.state.tokensOf(d.player2).size shouldBe 2
    }

    context("three players, two other players' redirects: the order decides, so the creator is asked") {
        fun threePlayer(): Pair<GameTestDriver, List<EntityId>> {
            val d = GameTestDriver()
            d.registerCards(TestCards.all + testCards)
            val players = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) }, skipMulligans = true, startingPlayer = 0)
            d.putPermanentOnBattlefield(players[1], redirector.name)
            d.putPermanentOnBattlefield(players[2], redirector.name)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN, players[0])
            return d to players
        }

        fun GameTestDriver.chooseOption(containing: String) {
            val decision = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
            val index = decision.options.indexOfFirst { containing in it }
            submitDecision(decision.playerId, OptionChosenResponse(decision.id, index)).error.shouldBeNull()
            resolveStack()
        }

        test("the would-be controller is asked, one option per applicable redirect") {
            val (d, players) = threePlayer()
            d.castAndResolve(players[0], twoSoldiers.name)
            val decision = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
            decision.playerId shouldBe players[0]
            decision.options.size shouldBe 2
            players.forEach { d.state.tokensOf(it).shouldBeEmpty() }
        }

        test("choosing player 2's redirect first ends with player 3, whose redirect then applies") {
            val (d, players) = threePlayer()
            d.castAndResolve(players[0], twoSoldiers.name)
            d.chooseOption("Player 2")
            d.pendingDecision.shouldBeNull()
            d.state.tokensOf(players[2]).size shouldBe 2
            d.state.tokensOf(players[1]).shouldBeEmpty()
            d.state.tokensOf(players[0]).shouldBeEmpty()
        }

        test("choosing player 3's redirect first ends with player 2") {
            val (d, players) = threePlayer()
            d.castAndResolve(players[0], twoSoldiers.name)
            d.chooseOption("Player 3")
            d.pendingDecision.shouldBeNull()
            d.state.tokensOf(players[1]).size shouldBe 2
            d.state.tokensOf(players[2]).shouldBeEmpty()
        }
    }
})
