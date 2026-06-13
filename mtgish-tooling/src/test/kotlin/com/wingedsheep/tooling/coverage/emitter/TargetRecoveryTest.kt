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

    "creatureFilterDsl declines an 'another' creature target (excludeSelf not composed, Deserter's Disciple)" {
        // "Another target creature you control" — an Other(ThisPermanent) self-exclusion the TargetFilter
        // surface can't compose; dropping it would let the source target itself. Decline -> SCAFFOLD.
        val anotherCreature = obj(
            """{"_Permanents":"And","args":[""" +
                """{"_Permanents":"IsCardtype","args":"Creature"},""" +
                """{"_Permanents":"Other","args":{"_Permanent":"ThisPermanent"}}]}""",
        )
        ctx.creatureFilterDsl(anotherCreature).shouldBeNull()
    }

    "gameObjectFilterDsl declines a '+1/+1 counters on them' group (Badgermole's trample lord)" {
        // "creatures you control with +1/+1 counters on them" — a HasACounterOfType predicate the flat
        // GroupFilter can't express; dropping it would widen the grant to every creature. Decline.
        val withCounters = obj(
            """{"_Permanents":"And","args":[""" +
                """{"_Permanents":"IsCardtype","args":"Creature"},""" +
                """{"_Permanents":"HasACounterOfType","_CounterType":"PTCounter","args":[1,1]}]}""",
        )
        ctx.gameObjectFilterDsl(withCounters).shouldBeNull()
    }

    "gameObjectFilterDsl declines an enchantment subtype (Shrine triggers, The Spirit Oasis)" {
        // "another Shrine you control" — an IsEnchantmentType subtype this surface can't render; widening
        // it to GameObjectFilter.Permanent/Enchantment would drop the subtype. Decline -> SCAFFOLD.
        val shrine = obj(
            """{"_Permanents":"And","args":[""" +
                """{"_Permanents":"IsEnchantmentType","args":"Shrine"},""" +
                """{"_Permanents":"Other","args":{"_Permanent":"ThisPermanent"}}]}""",
        )
        ctx.gameObjectFilterDsl(shrine).shouldBeNull()
    }

    "gameObjectFilterDsl declines a creature-subtype + non-creature-type union (Great Divide Guide)" {
        // "Each land and Ally you control" — a creature subtype unioned with the Land cardtype. The
        // single-subtype branch would render only the Ally half and drop the land half. Decline.
        val landAndAlly = obj(
            """{"_Permanents":"And","args":[""" +
                """{"_Permanents":"IsCardtype","args":"Land"},""" +
                """{"_Permanents":"IsCreatureType","args":"Ally"}]}""",
        )
        ctx.gameObjectFilterDsl(landAndAlly).shouldBeNull()
    }

    "targetExpr declines a bare permanent target carrying a dropped controller restriction (North Pole Patrol)" {
        // "untap another target permanent you control" — a bare TargetPermanent would drop the
        // ControlledByAPlayer (and Other) restriction, widening to any permanent. Decline -> SCAFFOLD.
        ctx.targetDsl(obj("""{"_Target":"TargetPermanent","args":{"_Permanents":"ControlledByAPlayer","args":{"_Players":"You"}}}""")).shouldBeNull()
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

    "creatureFilterDsl renders a negated creature subtype via notSubtype (Sterling Keykeeper)" {
        // "target non-Mount creature" — IsNonCreatureType has no TargetFilter passthrough, so it must
        // render the GameObjectFilter form wrapped in TargetFilter rather than dropping "non-Mount".
        val nonMount = obj(
            """{"_Permanents":"And","args":[""" +
                """{"_Permanents":"IsNonCreatureType","args":"Mount"},""" +
                """{"_Permanents":"IsCardtype","args":"Creature"}]}""",
        )
        ctx.creatureFilterDsl(nonMount) shouldBe "TargetFilter(GameObjectFilter.Creature.notSubtype(Subtype(\"Mount\")))"
    }

    "creatureFilterDsl renders a 'dealt damage this turn' restriction (Rooftop Assassin)" {
        // "target creature an opponent controls that was dealt damage this turn" -> the
        // .dealtDamageThisTurn() state-predicate suffix composed with the controller suffix.
        val dealtDamage = obj(
            """{"_Permanents":"And","args":[""" +
                """{"_Permanents":"IsCardtype","args":"Creature"},""" +
                """{"_Permanents":"ControlledByAPlayer","args":{"_Players":"Opponent"}},""" +
                """{"_Permanents":"WasDealtDamageThisTurn"}]}""",
        )
        ctx.creatureFilterDsl(dealtDamage) shouldBe
            "TargetFilter.Creature.dealtDamageThisTurn().opponentControls()"
    }

    "creatureFilterDsl declines 'dealt damage this turn' combined with an unrenderable shape" {
        // The .dealtDamageThisTurn() suffix only composes on the plain-creature path; combined with a
        // creature-subtype clause (a branch that returns before the suffix) it would be silently dropped,
        // so the recovery declines (-> SCAFFOLD) rather than widen the kill.
        val dealtDamageGoblin = obj(
            """{"_Permanents":"And","args":[""" +
                """{"_Permanents":"IsCreatureType","args":"Goblin"},{"_Permanents":"WasDealtDamageThisTurn"}]}""",
        )
        ctx.creatureFilterDsl(dealtDamageGoblin).shouldBeNull()
    }

    "gameObjectFilterDsl renders an outlaw filter via withAnyOfSubtypes (Vial Smasher)" {
        // "another outlaw you control" — IsAnOutlaw must render the outlaw creature group, not widen to
        // any permanent.
        val outlaw = obj(
            """{"_Permanents":"And","args":[{"_Permanents":"IsAnOutlaw"},""" +
                """{"_Permanents":"ControlledByAPlayer","args":{"_Players":"SinglePlayer","args":{"_Player":"You"}}}]}""",
        )
        ctx.gameObjectFilterDsl(outlaw) shouldBe
            "GameObjectFilter.Creature.withAnyOfSubtypes(Subtype.OUTLAW_TYPES).youControl()"
    }

    "gameObjectFilterDsl declines a group controlled by a target player (Neutralize the Guards)" {
        // "creatures target opponent controls get -1/-1" — the controller is the chosen Ref_TargetPlayer,
        // which no static GroupFilter expresses; dropping it would debuff every creature on the battlefield.
        val targetPlayersCreatures = obj(
            """{"_Permanents":"And","args":[{"_Permanents":"IsCardtype","args":"Creature"},""" +
                """{"_Permanents":"ControlledByAPlayer","args":{"_Players":"SinglePlayer","args":{"_Player":"Ref_TargetPlayer"}}}]}""",
        )
        ctx.gameObjectFilterDsl(targetPlayersCreatures).shouldBeNull()
    }

    "gameObjectFilterDsl renders 'a player other than you' as opponentControls (Artistic Process)" {
        // "each creature you DON'T control" — ControlledByAPlayer wrapping Other(You). The Other inverts
        // the controller, so this must render opponentControls, NOT youControl (which would damage your
        // own board instead of the opponent's).
        val youDontControl = obj(
            """{"_Permanents":"And","args":[{"_Permanents":"IsCardtype","args":"Creature"},""" +
                """{"_Permanents":"ControlledByAPlayer","args":{"_Players":"Other","args":{"_Player":"You"}}}]}""",
        )
        ctx.gameObjectFilterDsl(youDontControl) shouldBe "GameObjectFilter.Creature.opponentControls()"
    }

    "gameObjectFilterDsl declines a cardtype union it can't express (Splatter Technique's creature+planeswalker)" {
        // "each creature and planeswalker" — Or[Creature, Planeswalker]. There is no CreatureOrPlaneswalker
        // GroupFilter, so keeping only the Creature half would silently drop planeswalkers. Decline instead.
        val creatureOrPlaneswalker = obj(
            """{"_Permanents":"Or","args":[{"_Permanents":"IsCardtype","args":"Creature"},""" +
                """{"_Permanents":"IsCardtype","args":"Planeswalker"}]}""",
        )
        ctx.gameObjectFilterDsl(creatureOrPlaneswalker).shouldBeNull()
    }
})
