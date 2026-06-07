package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.J
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject

/**
 * Per-handler table test for the target/filter recovery layer: pins a handful of mtgish IR fragments to
 * the exact Argentum DSL the [creatureFilterExpr] / [gameObjectFilterExpr] / [targetExpr] node builders
 * render. Fast and local (no IR download / compile); the committed golden remains the exhaustive net.
 */
class TargetRecoveryTest : StringSpec({

    val ctx = EmitCtx(emptySet())
    fun obj(json: String): JsonObject = J.parseToJsonElement(json) as JsonObject

    // PowerIs clauses (Fleet-Footed Monk's "power 2 or greater").
    val pow2 = """{"_Permanents":"PowerIs","args":{"_Comparison":"GreaterThanOrEqualTo","args":{"_GameNumber":"Integer","args":2}}}"""

    "creatureFilterDsl composes the plain-creature base with recovered suffixes" {
        ctx.creatureFilterDsl(obj("""{"_Permanents":"IsTapped"}""")) shouldBe "TargetFilter.Creature.tapped()"
        ctx.creatureFilterDsl(obj(pow2)) shouldBe "TargetFilter.Creature.powerAtLeast(2)"
    }

    "creatureFilterDsl distributes an Or of per-subtype creature filters" {
        val orSubs = obj(
            """{"_Filter":"Or","args":[""" +
                """{"_Permanents":"IsCreatureType","args":"Goblin"},""" +
                """{"_Permanents":"IsCreatureType","args":"Soldier"}]}""",
        )
        ctx.creatureFilterDsl(orSubs) shouldBe
            "TargetFilter(GameObjectFilter.Creature.withSubtype(\"Goblin\") or GameObjectFilter.Creature.withSubtype(\"Soldier\"))"
    }

    "creatureFilterDsl renders power-or-toughness and suppresses the standalone power bound" {
        // "creature with power or toughness 4 or greater" (Repel Calamity) — the Or owns the bound, so the
        // standalone powerAtLeast must NOT also fire (which would narrow the filter to power alone).
        val powerOrToughness = obj(
            """{"_Filter":"And","args":[{"_Permanents":"IsCardtype","args":"Creature"},{"_Permanents":"Or","args":[""" +
                """{"_Permanents":"PowerIs","args":{"_Comparison":"GreaterThanOrEqualTo","args":{"_GameNumber":"Integer","args":4}}},""" +
                """{"_Permanents":"ToughnessIs","args":{"_Comparison":"GreaterThanOrEqualTo","args":{"_GameNumber":"Integer","args":4}}}]}]}""",
        )
        ctx.creatureFilterDsl(powerOrToughness) shouldBe "TargetFilter.Creature.powerOrToughnessAtLeast(4)"
    }

    "gameObjectFilterDsl renders an Or of creature subtypes as withAnyOfSubtypes" {
        // "another Frog, Rabbit, Raccoon, or Squirrel you control" (Valley Mightcaller) — an explicit Or,
        // distinct from an And ("Goblin Wizard") which would decline.
        val orSubs = obj(
            """{"_Filter":"And","args":[{"_Permanents":"Or","args":[""" +
                """{"_Permanents":"IsCreatureType","args":"Frog"},""" +
                """{"_Permanents":"IsCreatureType","args":"Squirrel"}]}]}""",
        )
        ctx.gameObjectFilterDsl(orSubs) shouldBe
            "GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype.FROG, Subtype.SQUIRREL))"
    }

    "gameObjectFilterDsl appends nontoken from the IsNonToken marker" {
        val nontokenBird = obj(
            """{"_Filter":"And","args":[""" +
                """{"_Permanents":"IsCreatureType","args":"Bird"},{"_Permanents":"IsNonToken"}]}""",
        )
        ctx.gameObjectFilterDsl(nontokenBird) shouldBe "GameObjectFilter.Creature.withSubtype(Subtype.BIRD).nontoken()"
    }

    "targetExpr renders a nonland permanent target" {
        // "untap target nonland permanent" (Thistledown Players) — IsNonCardtype Land with no positive type.
        ctx.targetDsl(obj("""{"_Target":"TargetPermanent","args":{"_Permanents":"IsNonCardtype","args":"Land"}}""")) shouldBe
            "TargetPermanent(filter = TargetFilter.NonlandPermanent)"
    }

    "gameObjectFilterDsl reads the base cardtype and appends the tapped suffix" {
        val tappedCreature = obj(
            """{"_Filter":"And","args":[""" +
                """{"_Permanents":"IsCardtype","args":"Creature"},{"_Permanents":"IsTapped"}]}""",
        )
        ctx.gameObjectFilterDsl(tappedCreature) shouldBe "GameObjectFilter.Creature.tapped()"
    }

    "gameObjectFilterDsl declines a filter with no recoverable base type" {
        // No cardtype, subtype, or "Permanent" token anywhere -> SCAFFOLD rather than a widened filter.
        ctx.gameObjectFilterDsl(obj("""{"_Color":"Red"}""")).shouldBeNull()
    }

    "targetExpr maps the player / spell / graveyard target shapes" {
        ctx.targetDsl(obj("""{"_Target":"TargetPlayer"}""")) shouldBe "TargetPlayer()"
        ctx.targetDsl(obj("""{"_Target":"TargetSpell","args":{}}""")) shouldBe "TargetSpell()"
        ctx.targetDsl(obj("""{"_Target":"TargetGraveyardCard","args":{}}""")) shouldBe
            "TargetObject(filter = TargetFilter.CardInGraveyard)"
    }
})
