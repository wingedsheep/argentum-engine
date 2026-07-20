package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.Assign
import com.wingedsheep.tooling.coverage.Block
import com.wingedsheep.tooling.coverage.Eval
import com.wingedsheep.tooling.coverage.HoleLine
import com.wingedsheep.tooling.coverage.Lit
import com.wingedsheep.tooling.coverage.RawLine
import com.wingedsheep.tooling.coverage.Stmt
import com.wingedsheep.tooling.coverage.arg
import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.call
import com.wingedsheep.tooling.coverage.asInt
import com.wingedsheep.tooling.coverage.asObj
import com.wingedsheep.tooling.coverage.asStr
import com.wingedsheep.tooling.coverage.compact
import com.wingedsheep.tooling.coverage.field
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.asciiIdentifier
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.pascalToUpperSnake
import com.wingedsheep.tooling.coverage.renderBlock
import com.wingedsheep.tooling.coverage.strField
import com.wingedsheep.tooling.coverage.bridge.Bridge
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * mtgish -> Argentum cardDef EMITTER. The single source of truth for "what the
 * generator would emit": fidelity tiers cards on whether [renderCard] completes, autogen writes the
 * files, and the Kotlin gate compiles the output. A whole-card render is only a draft candidate;
 * calibrated sets must pass the gameplay-tree fidelity gate before the output is trusted.
 *
 * This facade just assembles a whole card; the per-action "mapping" rules live in the sibling files
 * (see [EmitCtx]). Imports resolve from a live SDK source scan so they can't rot.
 */
object Emitter {
    /**
     * Render [card] to Argentum cardDef DSL. By default the emitter declines the WHOLE card to a bare
     * scaffold on the first un-renderable part. With [partial] = true it instead keeps every part that
     * maps and replaces each un-renderable part with a located `// TODO(hole)` line, so the result
     * carries a renderable fraction + the list of holes (what the dashboard/TUI shows). The default
     * (non-partial) path is byte-for-byte unchanged — it is what the autogen write path and the golden
     * test depend on.
     */
    fun renderCard(
        card: JsonObject, scryfall: JsonObject?, effects: Set<String>, keywords: Set<String>,
        pkg: String = "com.wingedsheep.mtg.sets.generated.demo.cards",
        partial: Boolean = false,
    ): RenderResult {
        val ctx = EmitCtx(keywords, scryfall?.strField("oracle_text"))
        val name = card["Name"].asStr() ?: ""
        val ident = asciiIdentifier(name)
        val pt = card["CardPT"].asObj

        val permanent = isPermanent(card)
        val kw = if (permanent) keywordLines(card, keywords, ctx.oracleText) else emptySet()

        // The whole card is one Block tree: the KDoc precedes it, the shell/ability lines are its body
        // statements, and the renderer turns it into source. Shell scaffolding (mana/typeline/metadata)
        // stays as RawLine — the formatted, un-modeled leaves the Raw escape-hatch is for — while the
        // ability builders contribute structured statements.
        val pre = docCommentLines(card, scryfall)
        val header = "val $ident = card(\"${ktStr(name)}\")"
        val body = mutableListOf<Stmt>()

        // A part the emitter can't render. In the default path this bails the whole card to a scaffold
        // (returns the [incomplete] result, which the caller `return`s). In [partial] mode it records a
        // located hole, drops a `// TODO(hole)` line in place, and returns null so the loop continues
        // with the parts that DO map. [addReason] mirrors the historical `ctx.reasons` mutation at each
        // site exactly, so the non-partial output stays byte-identical.
        val holes = mutableListOf<String>()
        val skipRules = mutableSetOf<JsonObject>()
        var parts = 0
        fun gap(holeLabel: String, addReason: String? = null): RenderResult? {
            addReason?.let { ctx.reasons.add(it) }
            parts++
            if (partial) { holes.add(holeLabel); body.add(HoleLine(holeLabel)); return null }
            return incomplete(ctx, pre, header, body, scryfall, pkg)
        }

        // Preparation cards (Secrets of Strixhaven) are also two-faced on Scryfall (creature // prepare
        // spell), but unlike generic multi-faced cards the mtgish IR carries BOTH faces — the creature
        // as the top-level card and the prepare spell under `Prepared` — so the dedicated Preparer path
        // below renders the whole card faithfully. Detect it before the multi-faced guard so it isn't
        // declined as a lossy single-face render.
        val isPreparer = card.strField("_OracleCard") == "Preparer" && card["Prepared"] != null

        // Multi-faced cards (transform/MDFC/adventure/split) reach us with their faces' oracle text joined
        // by a `\n//\n` separator (see Scryfall.cardMetadata). The bridge only sees the mtgish IR for the
        // FRONT face, so a generic render would silently emit a single-faced card and drop the entire back
        // face — exactly the lossy approximation the hard rules forbid. Decline unconditionally (even in
        // partial mode) so these classify as SCAFFOLD/BLOCKED and get hand-authored instead. Preparer
        // cards are exempt — both faces come from the IR (see above).
        if (!isPreparer && ctx.oracleText?.contains("\n//\n") == true) {
            ctx.reasons.add("multi-faced")
            return incomplete(ctx, pre, header, body, scryfall, pkg)
        }

        body.add(RawLine("    manaCost = \"${renderMana(card["ManaCost"])}\""))
        colorIdentityDsl(scryfall)?.let { body.add(RawLine("    colorIdentity = \"$it\"")) }
        body.add(RawLine("    typeLine = \"${renderTypeline(card["Typeline"])}\""))
        // Printed oracle text is display/training data (the gym observation surfaces it to agents); the
        // engine still derives behaviour from the structured keywords/effects below, never from this string.
        scryfall?.strField("oracle_text")?.takeIf { it.isNotEmpty() }?.let { body.add(RawLine("    oracleText = \"${ktStr(it)}\"")) }
        if (pt != null) { body.add(RawLine("    power = ${pt["Power"].asInt()}")); body.add(RawLine("    toughness = ${pt["Toughness"].asInt()}")) }
        // Emit keywords in printed (rule) order, not alphabetical — `keywordLines` collects them in the
        // card's rule order, which matches the hand-authored golden's `keywords(...)` order (e.g.
        // "Trample, haste" stays TRAMPLE, HASTE rather than re-sorting to HASTE, TRAMPLE).
        // Prowess and Training are keyword abilities whose triggered ability the engine derives from
        // an explicit trigger, NOT the keyword tag — so each must use its DSL helper (keyword +
        // trigger), never a bare keywords(Keyword.PROWESS) / keywords(Keyword.TRAINING), which would
        // render the +1/+1 pump / attack-counter trigger as a no-op. Training (CR 702.149a) is the
        // `training()` builder helper — keyword + the "attacks alongside a greater-power creature ->
        // +1/+1 counter" attack trigger; the paired *payoff* ("whenever this creature trains, …",
        // Savior of Ollenbock) is a separate WhenAPermanentTrains trigger the emitter still scaffolds
        // (per the fidelity policy — that fusion isn't recoverable), so this only renders the plain
        // keyword faithfully, exactly like prowess()/station()/increment(). Emitted after the plain
        // keyword line so the order matches the golden (e.g. Gryffwing Cavalry: FLYING then training()).
        val builderKw = setOf("PROWESS", "TRAINING")
        val plainKw = kw.filterNot { it in builderKw }
        if (plainKw.isNotEmpty()) body.add(RawLine("    keywords(${plainKw.joinToString(", ") { "Keyword.$it" }})"))
        if ("TRAINING" in kw) body.add(RawLine("    training()"))
        if ("PROWESS" in kw) body.add(RawLine("    prowess()"))

        // Preparation card (Secrets of Strixhaven): `_OracleCard: "Preparer"` with a sibling
        // `Prepared` prepare spell. Always render a prepare("…") { spell { … } } block from the
        // nested prepare card. Two flavours:
        //  - "enters prepared" — a top-level AsPermanentEnters[EntersPrepared] rule. Emit the PREPARED
        //    keyword ("This creature enters prepared") and skip that rule (the keyword encodes it).
        //  - "becomes prepared via a trigger" (Leech Collector) — no AsPermanentEnters rule; the IR
        //    carries a TriggerA whose action is PreparePermanent. NO PREPARED keyword (it doesn't enter
        //    prepared); the trigger renders through the generic trigger path (Triggers.* + BecomePrepared).
        // The whole card scaffolds if the prepare spell can't be rendered exactly.
        if (isPreparer) {
            val entersPreparedRule = (card["Rules"].asArr ?: JsonArray(emptyList()))
                .filterIsInstance<JsonObject>()
                .firstOrNull { it.strField("_Rule") == "AsPermanentEnters" && jsonContains(it, "_ReplacementActionWouldEnter", "EntersPrepared") }
            if (entersPreparedRule != null) {
                body.add(RawLine("    keywords(Keyword.PREPARED)"))
                skipRules.add(entersPreparedRule)
            }
            val prepStmts = ctx.prepareBlock(card)
            if (prepStmts == null) gap("Prepared")?.let { return it }
            else body.addAll(prepStmts)
            parts++
        }
        val cardLevelLines = ctx.cardLevelCastEffectLines(card)
        if (cardLevelLines == null) gap("CastEffect")?.let { return it }
        else body.addAll(cardLevelLines.map { RawLine(it) })

        // Auras: the pure static-buff shape (EnchantPermanent + PermanentLayerEffect) renders faithfully,
        // but an activated/triggered ability on an Aura references its "enchanted creature" — context the
        // generic activated/trigger emitters don't model (they'd render a plain self/own-permanent ability).
        // e.g. the Onslaught Crowns' "Sacrifice this Aura: enchanted creature AND others sharing a type get …".
        // Scaffold those rather than emit a confidently-wrong complete render.
        if (jsonContains(card["Rules"], "_Rule", "EnchantPermanent")) {
            val unfaithfulOnAuras = setOf("Activated", "ActivatedWithModifiers", "TriggerA", "AsPermanentEnters", "FromAnyZone")
            (card["Rules"].asArr ?: JsonArray(emptyList())).forEach { r ->
                val rn = (r as? JsonObject)?.strField("_Rule")?.takeIf { it in unfaithfulOnAuras } ?: return@forEach
                // A "sacrifice this Aura: enchanted creature and creatures sharing its type get …" activated
                // ability (the Crowns) IS modelled exactly via GrantToEnchantedCreatureTypeGroupEffect, so it
                // renders rather than scaffolds; every other ability on an Aura still scaffolds.
                if ((rn == "Activated" || rn == "ActivatedWithModifiers") &&
                    "SharesACreatureTypeWithPermanent" in compact(r)) return@forEach
                // "When this Aura is put into a graveyard from the battlefield, …" (Reach for the Sky):
                // a SELF leaves-to-graveyard trigger references the Aura itself, NOT its enchanted
                // creature, so the generic trigger emitter renders it faithfully. Let it through (only
                // the self put-into-graveyard shape; any other TriggerA on an Aura still scaffolds).
                if (rn == "TriggerA" &&
                    jsonContains(r, "_Trigger", "WhenAPermanentIsPutIntoAPlayersGraveyard") &&
                    ctx.triggerBlock(r) != null
                ) return@forEach
                // In partial mode the offending ability becomes a located hole and is skipped in the
                // main loop below (so the generic activated/trigger emitter doesn't render it wrongly).
                gap("aura-with-$rn", addReason = "aura-with-$rn")?.let { return it }
                skipRules.add(r)
            }
        }

        val handledRules = setOf("SpellActions", "SpellActions_Spree", "TriggerA", "PermanentRuleEffect",
            "Flying", "Haste", "Vigilance", "Reach", "Defender", "Landwalk", "FirstStrike", "Trample", "CastEffect")
        for (rule in (card["Rules"].asArr ?: JsonArray(emptyList()))) {
            if (rule !is JsonObject) continue
            if (rule in skipRules) continue  // already holed by the aura pre-check (partial mode)
            val rname = rule.strField("_Rule")
            // Every ability builder returns structured statements; the renderer turns the whole card
            // tree into source lines.
            val block: List<Stmt>?
            when {
                rname == "CastEffect" -> {
                    // Already accounted for by the pre-loop cardLevelCastEffectLines pass; this is
                    // defensive (castEffectHandled is true for every CastEffect that survived it).
                    if (!ctx.castEffectHandled(rule)) gap("CastEffect")?.let { return it }
                    continue
                }
                rname == "SpellActions" -> block = ctx.spellBlock(card)
                rname == "SpellActions_Spree" -> block = ctx.spreeSpellBlock(rule)
                rname == "TriggerA" -> block = ctx.triggerBlock(rule)
                rname == "TriggerI" -> block = ctx.triggerIBlock(rule)
                rname == "TriggerIOnce" -> block = ctx.triggerIOnceBlock(rule)
                rname == "TriggerOnceEachTurn" -> block = ctx.triggerBlock(rule, oncePerTurn = true)
                rname == "TriggerMayOnceEachTurn" -> block = ctx.triggerMayOnceEachTurnBlock(rule)
                rname == "PermanentRuleEffect" -> block = ctx.staticBlock(rule)
                rname == "If" -> block = ctx.ifRuleBlock(rule)
                rname == "PlayerEffect" -> block = ctx.playerEffectBlock(rule)
                rname == "EachPlayerEffect" -> block = ctx.eachPlayerEffectBlock(rule)
                rname == "EnchantPermanent" -> block = ctx.auraTargetBlock(rule)
                rname == "PermanentLayerEffect" -> block = ctx.staticHostBlock(rule)
                rname == "AsPermanentEnters" -> block = ctx.asEntersBlock(rule)
                rname == "ReplaceAPlayerWouldCreateTokens" -> block = ctx.replaceTokenCreationBlock(card, rule)
                rname == "EachPermanentLayerEffect" -> block = ctx.staticLordBlock(rule)
                rname == "AbilitiesTriggerAnAdditionalTime" -> block = ctx.additionalSourceTriggersBlock(rule)
                rname == "FromAnyZone" -> block = ctx.fromAnyZoneBlock(rule)
                rname == "FromGraveyard" -> block = ctx.fromGraveyardBlock(rule)
                // "You may cast this from your graveyard if [condition]. If you do, it enters with a +1/+1
                // counter." (Undead Sprinter) — a conditional self-cast permission + cast-this-way counter
                // rider. Only the exact MayCastGraveyardCardWithEnterActions(self, [+1/+1]) shape gated by a
                // nameable died-this-turn condition renders; anything else scaffolds with `FromGraveyardIf`.
                rname == "FromGraveyardIf" -> {
                    block = ctx.fromGraveyardIfBlock(rule)
                    if (block == null) { gap(rname)?.let { return it }; continue }
                }
                rname == "FromHand" -> block = ctx.fromHandBlock(rule)
                // A `FromStack` rule wraps a cast trigger that fires while the spell is still on the
                // stack (Infusion copy — Lumaret's Favor). Only the exact Infusion-copy shape renders;
                // anything else (e.g. the "can't be countered" cast effect on Scragnoth/Bruna) declines
                // and scaffolds with `FromStack` as the unrecovered structure reason, like before.
                rname == "FromStack" -> {
                    block = ctx.lumaretsFavorInfusionCopyBlock(rule)
                    if (block == null) { gap(rname, addReason = rname)?.let { return it }; continue }
                }
                rname == "CDA_Power" -> block = ctx.cdaStatsBlock(card, rule)
                rname == "CDA_Toughness" ->
                    if (jsonContains(card["Rules"], "_Rule", "CDA_Power")) continue  // emitted with CDA_Power
                    else block = ctx.cdaToughnessBlock(rule)
                rname == "Activated" || rname == "ActivatedWithModifiers" -> block = ctx.activatedBlock(rule)
                rname == "Cycling" -> block = manaKeywordCost(rule)?.let { listOf(Eval(call("keywordAbility", arg(call("KeywordAbility.cycling", arg("\"$it\"")))))) }
                // Typecycling (CR 702.29) — the land-type "Forestcycling"/"Swampcycling"/… forms carry
                // `[Cards IsLandType <Type>, Cost PayMana {cost}]`. Render the subtype-cycling builder
                // `KeywordAbility.typecycling("<Type>", ManaCost.parse("{cost}"))`; the engine reuses the
                // Cycling handler with that search filter. Pure-mana cost only; a non-mana typecycling
                // cost (none printed) declines -> scaffold.
                rname == "TypeCycling" -> block = ctx.typecyclingLine(rule)
                rname == "Morph" -> block = manaKeywordCost(rule)?.let { listOf(Assign("morph", Lit("\"$it\""))) }
                // FlashForCasters (conditional flash, CR 702.8) — "<this> has flash as long as you
                // control a [filter]" (Colossal Rattlewurm: "...as long as you control a Desert"). The
                // condition rides as `PlayerPassesFilter(You, ControlsA(filter))`; render the card-level
                // `conditionalFlash = Conditions.YouControl(<filter>)` assignment (identical tree to a
                // raw `Exists(You, BATTLEFIELD, filter)`). Only the "you control a [filter]" shape the
                // shared youControlConditionDsl can render exactly produces a line; any other condition
                // declines -> SCAFFOLD rather than guess.
                rname == "FlashForCasters" -> block = ctx.conditionalFlashLines(rule)
                rname == "Flashback" -> block = manaKeywordCost(rule)?.let { listOf(Eval(call("keywordAbility", arg(call("KeywordAbility.flashback", arg("\"$it\"")))))) }
                rname == "Crew" -> block = rule["args"].asInt()?.let { listOf(Eval(call("keywordAbility", arg(call("KeywordAbility.crew", arg("$it")))))) }
                // "Crew N. Activate only once each turn." (Luxurious Locomotive) — CrewOnceEachTurn carries
                // the crew power N. Renders `KeywordAbility.crew(N, onceEachTurn = true)`; the engine enforces
                // the once-per-turn cap in the crew enumerator/handler.
                rname == "CrewOnceEachTurn" -> block = rule["args"].asInt()?.let {
                    listOf(Eval(call("keywordAbility", arg(call("KeywordAbility.crew", arg("$it"), arg("onceEachTurn", "true"))))))
                }
                // Saddle N (CR 702.171) — a numeric keyword ability. mtgish shapes the count as a nested
                // `_GameNumber: Integer` game number, so dig the integer out of the args (findInteger) rather
                // than reading args directly as an Int. Renders `keywordAbility(KeywordAbility.saddle(N))`,
                // exactly like Crew above; the engine synthesises the Saddle special action from the keyword.
                rname == "Saddle" -> block = (findInteger(rule["args"]) as? Int)?.let { listOf(Eval(call("keywordAbility", arg(call("KeywordAbility.saddle", arg("$it")))))) }
                // Firebending N (CR 702.189, Avatar: The Last Airbender) — a numeric keyword ability
                // whose count rides in a nested `_GameNumber: Integer` (like Saddle). Unlike Saddle,
                // the engine has no Firebending handler: the behavior (attack -> add N {R} that lasts
                // until end of combat) lives in the `firebending(n)` CardBuilder helper's triggered
                // ability, so render the builder call directly (like `station()`), NOT a bare
                // keywordAbility (which would add the display keyword but drop the mana trigger).
                // "firebending X (X = its power)" carries an XValue node -> findInteger returns "X" ->
                // `as? Int` is null -> scaffold, rather than guessing.
                rname == "Firebending" -> block = (findInteger(rule["args"]) as? Int)?.let { listOf(Eval(call("firebending", arg("$it")))) }
                // Impending N—[cost] (CR 702.176, Duskmourn) — a self-alternative-cost keyword whose
                // count rides as a `_GameNumber: Integer` and whose cost is a `_Cost: PayMana`, both in
                // a 2-element args array. The whole mechanic (alt cost + time counters + "not a creature
                // while it has a time counter" static + end-step countdown trigger) is composed by the
                // `impending(n, cost)` CardBuilder helper, so render the builder call directly (like
                // `firebending(n)` / `station()`), NOT a bare keywordAbility (which would print the keyword
                // but drop the alt cost and the counter machinery). Pure-mana cost only; a non-mana or
                // missing cost / non-Int count declines -> scaffold rather than emit an inexact cost.
                rname == "Impending" -> block = ctx.impendingLine(rule)
                // Ward—<cost> (CR 702.21) carries a `_Cost` arg. `wardKeywordLine` renders the cost
                // shapes the SDK exposes — mana (`Ward {x}`), discard-a-card, pay-N-life, sacrifice-a-
                // <filter>; compound/dynamic costs return null -> scaffold rather than drop the cost. A
                // bare WARD enum keyword would lose the cost entirely, so this must never fall through
                // to the keyword case.
                rname == "Ward" -> block = ctx.wardKeywordLine(rule)
                // Plot {cost} (CR 718, OTJ) carries a `_Cost: PayMana` arg -> KeywordAbility.plot("{cost}").
                // Pure-mana only; a non-mana plot cost (none printed) declines -> scaffold.
                rname == "Plot" -> block = manaKeywordCost(rule)?.let {
                    listOf(Eval(call("keywordAbility", arg(call("KeywordAbility.plot", arg("\"$it\""))))))
                }
                // Affinity for <group> (cost-reduction keyword, CR 702.41) — "this spell costs {1} less
                // to cast for each <filter> you control." The engine has no Keyword.AFFINITY card-keyword
                // path for arbitrary group affinity, so render a self-cast ModifySpellCost whose
                // per-permanent generic reduction counts the matching permanents you control (the general
                // PermanentsYouControlMatching primitive). Only the group filters we can render exactly
                // produce a block; anything else declines -> scaffold.
                rname == "Affinity" -> block = ctx.affinityBlock(rule)
                // "Instant and sorcery spells you cast have affinity for creatures" (Witherbloom, the
                // Balancer) — a StackSpellsEffect granting a spell effect to a class of spells you cast.
                // Only the granted-affinity shape over the Instant/Sorcery filter renders; anything else
                // declines -> SCAFFOLD.
                rname == "StackSpellsEffect" -> block = ctx.stackSpellsEffectBlock(rule)
                // Station keyword ability (CR 702.184a) — fully fixed, renders the no-arg builder.
                rname == "Station" -> block = listOf(Eval(call("station")))
                // Increment (Secrets of Strixhaven) — a keyword whose whole mechanic ("whenever you cast
                // a spell, if the mana you spent exceeds this creature's power or toughness, +1/+1
                // counter") is composed by the `increment()` CardBuilder helper. Like `station()` /
                // `firebending(n)`, render the builder call directly rather than a bare keywordAbility
                // (which would print the keyword but drop the cast-spell trigger). The rule carries no args.
                rname == "Increment" -> block = listOf(Eval(call("increment")))
                // {N+} station symbol that animates into a creature (CR 721.2b). Non-animating
                // `StationCharged` (gating an activated/triggered ability) is left to the default
                // branch → scaffold, since its payload is arbitrary.
                rname == "StationChargedAnimate" -> block = ctx.stationAnimateBlock(rule)
                rname == "Equip" -> block = equipAbilityLine(rule)
                rname == "Protection" -> block = protectionScopeDsl(rule)?.let { listOf(Eval(call("keywordAbility", arg(call("KeywordAbility.Protection", arg(Lit(it))))))) }
                // Torpor Orb: "Creatures entering don't cause abilities to trigger." A whole-card static
                // -> staticAbility { ability = SuppressEntersTriggers(<filter>) }. Only an `IsCardtype`
                // permanent filter renders; any richer filter declines -> scaffold.
                rname == "PermanentsEnteringTheBattlefieldDontCauseAbilitiesToTrigger" ->
                    block = ctx.suppressEntersTriggersBlock(rule)
                // Rest in Peace: "If a card or token would be put into a graveyard from anywhere, exile
                // it instead." -> staticAbility-less RedirectZoneChange(EXILE) replacement. Only the
                // unrestricted "any card/token, any player, from anywhere -> exile" shape renders.
                rname == "ReplaceWouldPutIntoGraveyard" -> block = ctx.replaceWouldPutIntoGraveyardBlock(rule)
                rname != null && (rname in handledRules || Bridge[rname]?.kind == "keyword") -> continue
                // A bare auto-keyword rule (Flying, Menace, …) carries no args. A keyword rule that DOES
                // carry args is parameterized (Crew N, Flashback {cost}, Bushido N, …) — those must be
                // rendered exactly by an explicit case above or scaffold; never silently stamped bare,
                // which would drop the parameter.
                rname != null && pascalToUpperSnake(rname) in keywords && rule["args"] == null -> continue
                else -> { gap(rname ?: "unknown-rule", addReason = rname ?: "unknown-rule")?.let { return it }; continue }
            }
            if (block == null) { gap(rname)?.let { return it }; continue }
            parts++
            body.addAll(block)
        }

        // Banishing Light / O-Ring: an `ExilePermanentUntil … UntilPermanentLeavesBattlefield` action
        // (rendered above as `Effects.ExileUntilLeaves`) needs the paired "when this leaves, return the
        // linked exiled card" trigger, which mtgish leaves implicit in the expiration. Synthesize it once
        // here so the exile is reversible exactly as the hand-authored card wires it.
        if (hasLinkedExileUntilLeaves(card)) {
            body.addAll(linkedExileReturnTrigger())
            parts++
        }

        if (!permanent && !jsonContains(card["Rules"], "_Rule", "SpellActions") &&
            !jsonContains(card["Rules"], "_Rule", "SpellActions_Spree")
        ) {
            gap("no-renderable-effect", addReason = "no-renderable-effect")?.let { return it }
        }

        body.addAll(metadataLines(scryfall).map { RawLine(it) })
        // In partial mode the card is "complete" only if no part holed; the non-partial path never
        // reaches here with holes (gap returned the scaffold), so this stays `true`/empty as before.
        val complete = holes.isEmpty()
        return RenderResult(
            assemble(pre + renderBlock(Block(header, body)), pkg, complete = complete),
            complete, ctx.reasons, holes = holes, parts = parts,
        )
    }

    /** Standard equip — `Equip {cost}: attach to target creature you control` (CR 702.6). mtgish models
     *  the keyword as `[equip-filter, cost]`. We render the bare `equipAbility("{cost}")` (which
     *  synthesises the canonical sorcery-speed attach ability) ONLY for the unrestricted "any creature"
     *  filter with a pure-mana cost. Equip-quality variants ("equip legendary creature", a creature-type
     *  restriction) and non-mana costs aren't expressible by `equipAbility`, so they return null ->
     *  scaffold rather than silently drop the restriction. */
    private fun equipAbilityLine(rule: JsonObject): List<Stmt>? {
        val args = rule["args"].asArr ?: return null
        if (args.size != 2) return null
        // Only the unrestricted IsCardtype=Creature filter renders; anything narrower must scaffold.
        val filter = args[0]
        if (filter.strField("_Permanents") != "IsCardtype" || filter.field("args").asStr() != "Creature") return null
        val cost = args[1]
        if (cost.strField("_Cost") != "PayMana") return null
        val mana = renderMana(cost.field("args"))
        if (mana.isEmpty()) return null
        return listOf(Eval(call("equipAbility", arg("\"$mana\""))))
    }

    /** A keyword-ability whose cost is pure mana (`Cycling {2}`, `Morph {4}{W}`). Returns the rendered
     *  mana string, or null when the cost isn't simple mana — which downgrades the card to a scaffold
     *  rather than emit an inexact cost. */
    private fun manaKeywordCost(rule: JsonObject): String? {
        val cost = rule.field("args")
        if (cost.strField("_Cost") != "PayMana") return null
        return renderMana(cost.field("args"))
    }

    /** `Protection from X` -> the `ProtectionScope.*` DSL, or null (scaffold) for scopes the emitter
     *  can't render exactly yet. ONS only needs single/multi colour and creature-subtype. */
    private fun protectionScopeDsl(rule: JsonObject): String? {
        val inner = rule.field("args")
        return when (inner.strField("_Protectable")) {
            "FromColor" -> {
                val colorsNode = inner.field("args")
                if (colorsNode.strField("_ProtectableColor") != "Colors") return null
                val colors = (colorsNode.field("args").asArr ?: return null).mapNotNull { it.strField("_Color")?.uppercase() }
                when {
                    colors.isEmpty() -> null
                    colors.size == 1 -> "ProtectionScope.Color(Color.${colors[0]})"
                    else -> "ProtectionScope.Colors(setOf(${colors.joinToString(", ") { "Color.$it" }}))"
                }
            }
            "FromTypes" -> {
                val typesNode = inner.field("args")
                if (typesNode.strField("_Cards") != "IsCreatureType") return null
                typesNode.field("args").asStr()?.let { "ProtectionScope.Subtype(\"$it\")" }
            }
            else -> null
        }
    }

    /** Landwalk-keyword recovery, exposed for the fidelity scorer's generated-capability set. */
    fun findLandwalkKeywords(node: JsonElement?, keywords: Set<String>, out: MutableSet<String>) =
        com.wingedsheep.tooling.coverage.emitter.findLandwalkKeywords(node, keywords, out)

    private fun incomplete(ctx: EmitCtx, pre: List<String>, header: String, body: List<Stmt>, scryfall: JsonObject?, pkg: String): RenderResult {
        val b = body.toMutableList()
        b.add(RawLine("    // STRUCTURE needs human wiring: ${ctx.reasons.sorted().joinToString(", ")}"))
        b.addAll(metadataLines(scryfall).map { RawLine(it) })
        return RenderResult(assemble(pre + renderBlock(Block(header, b)), pkg, complete = false), false, ctx.reasons)
    }
}
