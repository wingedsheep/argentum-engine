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

    "creatureFilterDsl recovers a multi-colour Or as withAnyColor (Escape Routes, Hunting Drake)" {
        val whiteOrBlack = obj(
            """{"_Permanents":"And","args":[""" +
                """{"_Permanents":"Or","args":[""" +
                    """{"_Permanents":"IsColor","args":{"_Color":"White"}},""" +
                    """{"_Permanents":"IsColor","args":{"_Color":"Black"}}]},""" +
                """{"_Permanents":"IsCardtype","args":"Creature"}]}""",
        )
        ctx.creatureFilterDsl(whiteOrBlack) shouldBe "TargetFilter.Creature.withAnyColor(Color.WHITE, Color.BLACK)"
    }

    "creatureFilterDsl declines a source-relative blocking relation (Cromat)" {
        // "creature blocking or blocked by <source>" — IsBlockingAttacker / IsBlockedByDefender bound to
        // ThisPermanent isn't a static filter, and must not be mis-caught as the BlockingCreature constant.
        val blockingSource = obj(
            """{"_Permanents":"And","args":[""" +
                """{"_Permanents":"IsCardtype","args":"Creature"},""" +
                """{"_Permanents":"Or","args":[""" +
                    """{"_Permanents":"IsBlockingAttacker","args":{"_Permanent":"ThisPermanent"}},""" +
                    """{"_Permanents":"IsBlockedByDefender","args":{"_Permanent":"ThisPermanent"}}]}]}""",
        )
        ctx.creatureFilterDsl(blockingSource).shouldBeNull()
    }

    "targetExpr restricts a counter to a colour-bound spell (Gainsay)" {
        ctx.targetDsl(obj("""{"_Target":"TargetSpell","args":{"_Spells":"IsColor","args":{"_Color":"Blue"}}}""")) shouldBe
            "TargetSpell(filter = TargetFilter.SpellOnStack.withColor(Color.BLUE))"
    }

    "targetExpr declines a spell whose filter is about what it targets, not its type (Confound)" {
        // "counter target spell that targets a creature": the nested IsCardtype describes the spell's
        // target, so it must not collapse to CreatureSpellOnStack ("a creature spell"). Decline -> SCAFFOLD.
        ctx.targetDsl(obj("""{"_Target":"TargetSpell","args":{"_Spells":"TargetsAPermanent","args":{"_Permanents":"IsCardtype","args":"Creature"}}}""")).shouldBeNull()
    }

    "targetExpr renders an artifact-subtype target via withSubtype (Turn to Dust, Rustspore Ram)" {
        // "destroy target Equipment" — IsArtifactType carries no IsCardtype, so it must narrow the artifact
        // filter by subtype rather than silently widening to "any permanent".
        ctx.targetDsl(obj("""{"_Target":"TargetPermanent","args":{"_Permanents":"IsArtifactType","args":"Equipment"}}""")) shouldBe
            "TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT)))"
    }

    "targetExpr declines a lowest-mana-value permanent target (Culling Scales)" {
        // "destroy target nonland permanent with the lowest mana value" — a global superlative no target
        // filter expresses; dropping it would let it hit any nonland permanent. Decline -> SCAFFOLD.
        ctx.targetDsl(obj("""{"_Target":"TargetPermanent","args":{"_Permanents":"APermanentWithTheLowestManaValue","args":{"_Permanents":"IsNonCardtype","args":"Land"}}}""")).shouldBeNull()
    }

    "targetExpr declines a noncreature-artifact target (Blinkmoth Well)" {
        // "tap target noncreature artifact" — the IsNonCardtype Creature restriction has no filter form
        // (there is no .noncreature()), so it must decline rather than widen to "any artifact".
        ctx.targetDsl(obj("""{"_Target":"TargetPermanent","args":{"_Permanents":"And","args":[{"_Permanents":"IsNonCardtype","args":"Creature"},{"_Permanents":"IsCardtype","args":"Artifact"}]}}""")).shouldBeNull()
    }

    "landSearchFilterDsl recovers an artifact-card search filter (Fabricate)" {
        // "search your library for an artifact card" — a single positive cardtype with no creature clause;
        // must render GameObjectFilter.Artifact, not the bare GameObjectFilter.Any fallthrough.
        ctx.landSearchFilterDsl(obj("""{"_SearchLibraryAction":"FindACardOfType","args":{"_CardsInLibrary":"IsCardtype","args":"Artifact"}}""")) shouldBe
            "GameObjectFilter.Artifact"
    }
})
