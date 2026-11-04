package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blb.cards.SplashLasher
import com.wingedsheep.mtg.sets.definitions.fra.cards.TamsResistance
import com.wingedsheep.mtg.sets.definitions.grn.cards.GirdForBattle
import com.wingedsheep.mtg.sets.definitions.lea.cards.GiantGrowth
import com.wingedsheep.mtg.sets.definitions.mh1.cards.ForceOfVigor
import com.wingedsheep.mtg.sets.definitions.usg.cards.GloriousAnthem
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * An optional target slot ("up to one target …", CR 601.2c) is a choice that includes *none*: the
 * AI only fills it with a target on the side of the table the effect is for.
 *
 * Found while fixing enabler sequencing (#3220): Tam's Resistance became a candidate on a board
 * whose only creature was the opponent's, and the AI put the +1/+1 counter on it.
 */
class OptionalTargetSlotsTest : FunSpec({

    val cards = TestCards.all + listOf(
        TamsResistance, ForceOfVigor, GloriousAnthem, GiantGrowth, SplashLasher, GirdForBattle, PredefinedTokens.Jace,
    )
    val registry = CardRegistry().apply { register(cards) }
    val intents = IntentCatalog.of(registry)
    val fixed = AiProfile.PRODUCTION_CANDIDATE_POLARITY

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(cards)
        initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun lands(d: GameTestDriver, player: EntityId, name: String, count: Int) =
        repeat(count) { d.putLandOnBattlefield(player, name) }

    fun castOf(d: GameTestDriver, card: EntityId): LegalAction = d.legalActions(d.player1).first {
        val cast = it.action as? CastSpell
        cast?.cardId == card && !cast.useAlternativeCost
    }

    fun heuristic(d: GameTestDriver, action: LegalAction, polarity: Boolean): List<EntityId> =
        (TargetSelection.fillHeuristically(
            d.state, action, d.player1, fillPartialRequirements = true,
            intents = intents, polarityFromEffect = polarity,
        ) as CastSpell).targets.map { (it as ChosenTarget.Permanent).entityId }

    fun committed(d: GameTestDriver, choices: List<LegalAction>): CastSpell =
        AIPlayer.create(registry, d.player1, fixed).chooseFrom(d.state, choices).action
            .shouldBeInstanceOf<CastSpell>()

    context("Tam's Resistance — a +1/+1 counter on up to one target creature, then Empower Jace") {
        test("with only the opponent's creature, it is cast at no target") {
            val d = driver()
            val theirs = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
            lands(d, d.player1, "Island", 2)
            val spell = d.putCardInHand(d.player1, "Tam's Resistance")
            val cast = castOf(d, spell)

            TargetSelection.polarities(d.state, cast, intents) shouldBe listOf(TargetPolarity.BENEFICIAL)
            heuristic(d, cast, polarity = false) shouldBe listOf(theirs)
            heuristic(d, cast, polarity = true).shouldBeEmpty()

            val chosen = committed(d, listOf(cast))
            chosen.targets.shouldBeEmpty()
            // A target-less cast is legal and still empowers Jace.
            d.submitSuccess(chosen)
            d.bothPass()
            d.state.projectedState.getPower(theirs) shouldBe 2
            d.getCreatures(d.player1).size shouldBe 0
            d.state.getBattlefield().any { d.state.getEntity(it)
                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Jace" } shouldBe true
        }

        test("with a creature of ours on the board, it targets ours") {
            val d = driver()
            d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
            val ours = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
            lands(d, d.player1, "Island", 2)
            val cast = castOf(d, d.putCardInHand(d.player1, "Tam's Resistance"))

            heuristic(d, cast, polarity = true) shouldBe listOf(ours)
            committed(d, listOf(cast)).targets shouldBe listOf(ChosenTarget.Permanent(ours))
        }
    }

    test("an up-to-two counter spell benefits both friendly creatures without helping the opponent") {
        val d = driver()
        val first = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val second = d.putCreatureOnBattlefield(d.player1, "Hill Giant")
        d.putCreatureOnBattlefield(d.player2, "Savannah Lions")
        lands(d, d.player1, "Plains", 1)
        val cast = castOf(d, d.putCardInHand(d.player1, "Gird for Battle"))

        TargetSelection.polarities(d.state, cast, intents) shouldBe listOf(TargetPolarity.BENEFICIAL)
        committed(d, listOf(cast)).targets.shouldContainExactlyInAnyOrder(
            ChosenTarget.Permanent(first), ChosenTarget.Permanent(second),
        )
    }

    test("an \"up to two\" destroy aims only at the opponent's permanent") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Glorious Anthem")
        val theirs = d.putPermanentOnBattlefield(d.player2, "Glorious Anthem")
        lands(d, d.player1, "Forest", 4)
        val cast = castOf(d, d.putCardInHand(d.player1, "Force of Vigor"))

        TargetSelection.polarities(d.state, cast, intents) shouldBe listOf(TargetPolarity.HARMFUL)
        heuristic(d, cast, polarity = true) shouldBe listOf(theirs)
        committed(d, listOf(cast)).targets shouldBe listOf(ChosenTarget.Permanent(theirs))
    }

    test("an \"up to two\" destroy with only our own permanent never destroys it") {
        val d = driver()
        val ours = d.putPermanentOnBattlefield(d.player1, "Glorious Anthem")
        lands(d, d.player1, "Forest", 4)
        val cast = castOf(d, d.putCardInHand(d.player1, "Force of Vigor"))
        val pass = d.legalActions(d.player1).single { it.action is PassPriority }

        heuristic(d, cast, polarity = false) shouldBe listOf(ours)
        heuristic(d, cast, polarity = true).shouldBeEmpty()
        val chosen = AIPlayer.create(registry, d.player1, fixed).chooseFrom(d.state, listOf(cast, pass)).action
        ((chosen as? CastSpell)?.targets ?: emptyList()).shouldBeEmpty()
    }

    test("a mandatory beneficial target still takes a legal target, and the spell is held") {
        val d = driver()
        val theirs = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        lands(d, d.player1, "Forest", 1)
        val cast = castOf(d, d.putCardInHand(d.player1, "Giant Growth"))
        val pass = d.legalActions(d.player1).single { it.action is PassPriority }

        heuristic(d, cast, polarity = true) shouldBe listOf(theirs)
        AIPlayer.create(registry, d.player1, fixed).chooseFrom(d.state, listOf(cast, pass)).action
            .shouldBeInstanceOf<PassPriority>()
    }

    test("a slot skipped ahead of a filled one is announced with explicit group counts") {
        val d = driver()
        fun slot(index: Int) = com.wingedsheep.engine.legalactions.TargetInfo(
            index = index, description = "up to one target creature", minTargets = 0, maxTargets = 1,
            validTargets = listOf(d.player2), targetZone = null,
        )
        val action = LegalAction(
            action = PassPriority(d.player1), actionType = "CastSpell", description = "two optional slots",
            targetRequirements = listOf(slot(0), slot(1), slot(2)),
        )
        TargetSelection.groupCountsFor(action, listOf(1, 0)) shouldBe null
        TargetSelection.groupCountsFor(action, listOf(0, 1)) shouldBe listOf(0, 1, 0)
    }

    test("an optional harmful trigger with only our own creatures to hit is left untargeted") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        lands(d, d.player1, "Island", 4)
        val lasher = d.putCardInHand(d.player1, "Splash Lasher")
        d.castSpell(d.player1, lasher)
        d.bothPass()
        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        val response = AIPlayer.create(registry, d.player1, fixed).respondToDecision(d.state, decision)
        response.shouldBeInstanceOf<TargetsResponse>().selectedTargets.values.flatten().shouldBeEmpty()
    }
})
