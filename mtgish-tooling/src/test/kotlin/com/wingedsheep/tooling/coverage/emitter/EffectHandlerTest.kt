package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.J
import com.wingedsheep.tooling.coverage.render
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject

/**
 * Per-handler table test for the effect layer: a handful of mtgish `_Action` fragments dispatched
 * through [renderAction] (the ACTION_HANDLERS registry) and rendered, pinned to the exact Argentum
 * Effect DSL. Fast and local; the committed golden remains the exhaustive net.
 */
class EffectHandlerTest : StringSpec({

    val ctx = EmitCtx(emptySet())
    fun obj(json: String): JsonObject = J.parseToJsonElement(json) as JsonObject
    fun effect(json: String): String? = ctx.renderAction(obj(json), null)?.let(::render)

    "constant 1:1 effects render as their fixed token (a Lit)" {
        effect("""{"_Action":"CounterSpell"}""") shouldBe "CounterEffect()"
        effect("""{"_Action":"Shuffle"}""") shouldBe "ShuffleLibraryEffect()"
        effect("""{"_Action":"DiscardACardAtRandom"}""") shouldBe "Patterns.Hand.discardRandom(1)"
    }

    "mana production maps the produce tag to the Add*Mana facade" {
        effect("""{"_Action":"AddMana","args":{"_ManaProduce":"ManaProduceC"}}""") shouldBe "Effects.AddColorlessMana(1)"
        effect("""{"_Action":"AddMana","args":{"_ManaProduce":"ManaProduceR"}}""") shouldBe "Effects.AddMana(Color.RED)"
        effect("""{"_Action":"AddMana","args":{"_ManaProduce":"AnyManaColor"}}""") shouldBe "Effects.AddManaOfChoice()"
    }

    "a mixed mana pool composites inline (single line, not the multi-line Composite)" {
        // And[ManaProduceB, ManaProduceB, ManaProduceB] -> Dark Ritual's {B}{B}{B}.
        val ritual = """{"_Action":"AddMana","args":{"_ManaProduce":"And","args":[""" +
            """{"_ManaProduce":"ManaProduceB"},{"_ManaProduce":"ManaProduceB"},{"_ManaProduce":"ManaProduceB"}]}}"""
        effect(ritual) shouldBe "Effects.AddMana(Color.BLACK, 3)"
    }

    "an unrecognised action declines (-> SCAFFOLD)" {
        effect("""{"_Action":"SomeActionWeDoNotModel"}""").shouldBeNull()
    }

    "CopySpell(Trigger_ThatSpell) renders 'copy that spell' on a cast trigger (Double Down)" {
        effect("""{"_Action":"CopySpell","args":{"_Spell":"Trigger_ThatSpell"}}""") shouldBe
            "Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity)"
    }

    "CopySpell with a non-triggering-spell subject declines (-> SCAFFOLD)" {
        effect("""{"_Action":"CopySpell","args":{"_Spell":"Ref_TargetSpell"}}""").shouldBeNull()
    }

    // --- SetPT layer effect ("target creature becomes a P/T until end of turn") -------------------
    fun layer(json: String, tvar: String?): String? = ctx.renderAction(obj(json), tvar)?.let(::render)

    "SetPT sets base P/T, and the end-of-turn case emits an EXPLICIT Duration.EndOfTurn" {
        // The SetBasePowerAndToughness facade defaults to Duration.EndOfTurn, but the layer renderer
        // always spells the duration out so the intended expiration is explicit.
        layer(
            """{"_Action":"CreatePermanentLayerEffectUntil","args":[{"_Permanent":"Ref_TargetPermanent"},""" +
                """[{"_LayerEffect":"SetPT","args":{"_PT":"PT","args":[5,1]}}],{"_Expiration":"UntilEndOfTurn"}]}""",
            "t",
        ) shouldBe "Effects.SetBasePowerAndToughness(5, 1, t, Duration.EndOfTurn)"
    }

    "SetPT carries a non-default expiration verbatim (for as long as it remains tapped)" {
        layer(
            """{"_Action":"CreatePermanentLayerEffectUntil","args":[{"_Permanent":"Ref_TargetPermanent"},""" +
                """[{"_LayerEffect":"SetPT","args":{"_PT":"PT","args":[0,2]}}],""" +
                """{"_Expiration":"ForAsLongAsPermanentRemainsTapped"}]}""",
            "t",
        ) shouldBe "Effects.SetBasePowerAndToughness(0, 2, t, Duration.WhileSourceTapped())"
    }

    "the each-permanent SetPT form wraps the set over a group" {
        layer(
            """{"_Action":"CreateEachPermanentLayerEffectUntil","args":[""" +
                """{"_Permanents":"And","args":[{"_Permanents":"IsCardtype","args":"Creature"},""" +
                """{"_Permanents":"ControlledByAPlayer","args":{"_Players":"SinglePlayer","args":{"_Player":"You"}}}]},""" +
                """[{"_LayerEffect":"SetPT","args":{"_PT":"PT","args":[1,1]}}],{"_Expiration":"UntilEndOfTurn"}]}""",
            null,
        ) shouldBe "Effects.ForEachInGroup(GroupFilter(GameObjectFilter.Creature.youControl()), " +
            "Effects.SetBasePowerAndToughness(1, 1, EffectTarget.Self, Duration.EndOfTurn))"
    }

    "a SetPT riding an unsupported AddCardtype still scaffolds (no silently-dropped 'becomes an artifact')" {
        // "It becomes a 0/0 artifact creature": AddCardtype isn't rendered here, so the whole card must
        // scaffold rather than emit just the P/T set and drop the type change.
        layer(
            """{"_Action":"CreatePermanentLayerEffectUntil","args":[{"_Permanent":"Ref_TargetPermanent"},""" +
                """[{"_LayerEffect":"SetPT","args":{"_PT":"PT","args":[0,0]}},""" +
                """{"_LayerEffect":"AddCardtype","args":"Artifact"}],{"_Expiration":"UntilEndOfTurn"}]}""",
            "t",
        ).shouldBeNull()
    }

    "'becomes a white Rabbit with base P/T 0/1' renders the atomic BecomeCreature (Metamorphic Blast)" {
        // SetColor + SetCreatureType + SetPT together are the engine's atomic BecomeCreature shape (one
        // effect across the COLOUR/TYPE/P-T layers), not a per-layer Composite.
        layer(
            """{"_Action":"CreatePermanentLayerEffectUntil","args":[{"_Permanent":"Ref_TargetPermanent"},""" +
                """[{"_LayerEffect":"SetColor","args":{"_SettableColor":"SimpleColorList","args":["White"]}},""" +
                """{"_LayerEffect":"SetCreatureType","args":"Rabbit"},""" +
                """{"_LayerEffect":"SetPT","args":{"_PT":"PT","args":[0,1]}}],{"_Expiration":"UntilEndOfTurn"}]}""",
            "EffectTarget.ContextTarget(0)",
        ) shouldBe "Effects.BecomeCreature(target = EffectTarget.ContextTarget(0), " +
            "power = DynamicAmount.Fixed(0), toughness = DynamicAmount.Fixed(1), " +
            "creatureTypes = setOf(\"Rabbit\"), colors = setOf(Color.WHITE.name), " +
            "duration = Duration.EndOfTurn)"
    }

    "'becomes a Fractal with base P/T each equal to X plus 1' renders dynamic BecomeCreature" {
        // SetPowerAndToughnessBoth with a dynamic GameNumber (X plus 1) applied to BOTH P and T —
        // the WAR/Simic "Fractal" animate. BecomeCreature now takes DynamicAmount P/T.
        layer(
            """{"_Action":"CreatePermanentLayerEffectUntil","args":[{"_Permanent":"Ref_TargetPermanent"},""" +
                """[{"_LayerEffect":"SetCreatureType","args":"Fractal"},""" +
                """{"_LayerEffect":"SetPowerAndToughnessBoth","args":{"_GameNumber":"Plus","args":[""" +
                """{"_GameNumber":"ValueX"},{"_GameNumber":"Integer","args":1}]}}],""" +
                """{"_Expiration":"UntilEndOfTurn"}]}""",
            "EffectTarget.ContextTarget(0)",
        ) shouldBe "Effects.BecomeCreature(target = EffectTarget.ContextTarget(0), " +
            "power = DynamicAmount.Add(DynamicAmount.XValue, DynamicAmount.Fixed(1)), " +
            "toughness = DynamicAmount.Add(DynamicAmount.XValue, DynamicAmount.Fixed(1)), " +
            "creatureTypes = setOf(\"Fractal\"), duration = Duration.EndOfTurn)"
    }

    // --- PutExiledCardOntoBattlefield (the return half of the exile-then-return blink) -------------

    "the bare under-owner's-control return renders a plain Move to the battlefield" {
        layer(
            """{"_Action":"PutExiledCardOntoBattlefield","args":[{"_CardInExile":"TheCardExiledThisWay"},""" +
                """[{"_EnterFlag":"EntersUnderOwnersControl"}]]}""",
            "t",
        ) shouldBe "Effects.Move(t, Zone.BATTLEFIELD)"
    }

    "a return with a +1/+1 counter renders the Move then a chained AddCountersEffect (Daydream)" {
        // The returned card isn't a permanent until it's back on the battlefield, so the counter is a
        // chained AddCountersEffect after the Move, not an enters-with replacement.
        layer(
            """{"_Action":"PutExiledCardOntoBattlefield","args":[{"_CardInExile":"TheCardExiledThisWay"},""" +
                """[{"_EnterFlag":"EntersUnderOwnersControl"},""" +
                """{"_EnterFlag":"EntersWithACounter","args":{"_CounterType":"PTCounter","args":[1,1]}}]]}""",
            "t",
        ) shouldBe "Effects.Composite(\n" +
            "            Effects.Move(t, Zone.BATTLEFIELD),\n" +
            "            AddCountersEffect(counterType = Counters.PLUS_ONE_PLUS_ONE, count = 1, target = t)\n" +
            "        )"
    }

    "a return with a non-±1/±1 counter kind declines (-> SCAFFOLD) rather than guess the counter" {
        layer(
            """{"_Action":"PutExiledCardOntoBattlefield","args":[{"_CardInExile":"TheCardExiledThisWay"},""" +
                """[{"_EnterFlag":"EntersUnderOwnersControl"},""" +
                """{"_EnterFlag":"EntersWithACounter","args":{"_CounterType":"PTCounter","args":[2,2]}}]]}""",
            "t",
        ).shouldBeNull()
    }

    "a return entering tapped still declines (the tapped flag would be silently dropped)" {
        layer(
            """{"_Action":"PutExiledCardOntoBattlefield","args":[{"_CardInExile":"TheCardExiledThisWay"},""" +
                """[{"_EnterFlag":"EntersUnderOwnersControl"},{"_EnterFlag":"EntersTapped"}]]}""",
            "t",
        ).shouldBeNull()
    }

    // --- ShuffleEachPermanentIntoLibrary (self + bound target, Floodpits Drowner) -----------------

    "ShuffleEachPermanentIntoLibrary renders self + target as two ShuffleIntoLibrary moves" {
        // "Shuffle this creature and target creature with a stun counter on it into their owners'
        // libraries" — Or(SinglePermanent ThisPermanent, SinglePermanent Ref_TargetPermanent), each
        // shuffled into its own owner's library, source first then the bound target.
        layer(
            """{"_Action":"ShuffleEachPermanentIntoLibrary","args":{"_Permanents":"Or","args":[""" +
                """{"_Permanents":"SinglePermanent","args":{"_Permanent":"ThisPermanent"}},""" +
                """{"_Permanents":"SinglePermanent","args":{"_Permanent":"Ref_TargetPermanent"}}]}}""",
            "t",
        ) shouldBe "Effects.ShuffleIntoLibrary(EffectTarget.Self).then(Effects.ShuffleIntoLibrary(t))"
    }

    "ShuffleEachPermanentIntoLibrary declines a group that isn't self + one bound target" {
        // No self member (two bound targets) — not the modeled self + target shape, so decline rather
        // than emit a shuffle that drops the source half.
        layer(
            """{"_Action":"ShuffleEachPermanentIntoLibrary","args":{"_Permanents":"Or","args":[""" +
                """{"_Permanents":"SinglePermanent","args":{"_Permanent":"Ref_TargetPermanent1"}},""" +
                """{"_Permanents":"SinglePermanent","args":{"_Permanent":"Ref_TargetPermanent2"}}]}}""",
            "t",
        ).shouldBeNull()
    }

    // --- SpellDealsDamage mass-damage recipients (MultipleRecipients rendered in IR = printed order) --
    // Each MultipleRecipients clause renders in the order the IR lists it, which is the printed order the
    // hand-authored goldens preserve. EachPlayer(Opponent) -> PlayerRef(EachOpponent) (excludes the
    // controller); EachPlayer(AnyPlayer) -> ForEachPlayerEffect(Player.Each, …) (includes the controller).

    fun spellDamage(recipients: String, amount: String = """{"_GameNumber":"Integer","args":1}"""): String? =
        effect(
            """{"_Action":"SpellDealsDamage","args":[{"_Spell":"ThisSpell"},$amount,$recipients]}""",
        )

    "End the Festivities: player-FIRST then permanent, opponent-scoped -> PlayerRef(EachOpponent)" {
        // IR order is EachPlayer(Opponent) then EachPermanent(creature/planeswalker they control); the
        // printed card and golden are player-first, so honour IR order rather than hardcoding permanents
        // first. "each opponent" must be PlayerRef(EachOpponent), NOT ForEachPlayerEffect(Player.Each).
        spellDamage(
            """{"_DamageRecipient":"MultipleRecipients","args":[""" +
                """{"_DamageRecipient":"EachPlayer","args":{"_Players":"Opponent"}},""" +
                """{"_DamageRecipient":"EachPermanent","args":{"_Permanents":"And","args":[""" +
                """{"_Permanents":"Or","args":[{"_Permanents":"IsCardtype","args":"Creature"},""" +
                """{"_Permanents":"IsCardtype","args":"Planeswalker"}]},""" +
                """{"_Permanents":"ControlledByAPlayer","args":{"_Players":"Opponent"}}]}}]}""",
        ) shouldBe "Effects.Composite(\n" +
            "            DealDamageEffect(1, EffectTarget.PlayerRef(Player.EachOpponent)),\n" +
            "            Effects.ForEachInGroup(GroupFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()), " +
            "DealDamageEffect(1, EffectTarget.Self))\n" +
            "        )"
    }

    "Dry Spell: permanent-FIRST then any-player -> creature group then ForEachPlayerEffect(Player.Each)" {
        // The mirror order of End the Festivities: EachPermanent(Creature) then EachPlayer(AnyPlayer).
        // "each player" includes the controller, so ForEachPlayerEffect(Player.Each), not EachOpponent.
        spellDamage(
            """{"_DamageRecipient":"MultipleRecipients","args":[""" +
                """{"_DamageRecipient":"EachPermanent","args":{"_Permanents":"IsCardtype","args":"Creature"}},""" +
                """{"_DamageRecipient":"EachPlayer","args":{"_Players":"AnyPlayer"}}]}""",
        ) shouldBe "Effects.Composite(\n" +
            "            Effects.ForEachInGroup(GroupFilter(GameObjectFilter.Creature), DealDamageEffect(1, EffectTarget.Self)),\n" +
            "            ForEachPlayerEffect(Player.Each, listOf(DealDamageEffect(1, EffectTarget.Controller)))\n" +
            "        )"
    }

    "a lone EachPlayer(Opponent) recipient renders the single each-opponent deal (no Composite)" {
        spellDamage(
            """{"_DamageRecipient":"EachPlayer","args":{"_Players":"Opponent"}}""",
            """{"_GameNumber":"Integer","args":2}""",
        ) shouldBe "DealDamageEffect(2, EffectTarget.PlayerRef(Player.EachOpponent))"
    }

    "a lone EachPlayer(AnyPlayer) recipient renders the single each-player ForEachPlayerEffect" {
        spellDamage(
            """{"_DamageRecipient":"EachPlayer","args":{"_Players":"AnyPlayer"}}""",
            """{"_GameNumber":"Integer","args":2}""",
        ) shouldBe "ForEachPlayerEffect(Player.Each, listOf(DealDamageEffect(2, EffectTarget.Controller)))"
    }

    "an unmodeled EachPlayer scope declines the whole card (-> SCAFFOLD), never a widened recipient set" {
        // Neither Opponent nor AnyPlayer — massDamageClause returns null, so the entire SpellDealsDamage
        // declines rather than silently guessing which players take the damage.
        spellDamage("""{"_DamageRecipient":"EachPlayer","args":{"_Players":"You"}}""").shouldBeNull()
    }
})
