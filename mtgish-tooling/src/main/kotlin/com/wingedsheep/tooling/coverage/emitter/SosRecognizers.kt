package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.Assign
import com.wingedsheep.tooling.coverage.Block
import com.wingedsheep.tooling.coverage.Composite
import com.wingedsheep.tooling.coverage.Dsl
import com.wingedsheep.tooling.coverage.Lit
import com.wingedsheep.tooling.coverage.Raw
import com.wingedsheep.tooling.coverage.RawLine
import com.wingedsheep.tooling.coverage.Stmt
import com.wingedsheep.tooling.coverage.Sub
import com.wingedsheep.tooling.coverage.arg
import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.asStr
import com.wingedsheep.tooling.coverage.call
import com.wingedsheep.tooling.coverage.compact
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.strField
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Multi-action composite recognizers for a few "Secrets of Strixhaven" spell shapes where the mtgish IR
 * splits one engine effect into sibling actions the per-action handlers can't fuse on their own. Each
 * matches an EXACT action-list shape and renders the same gameplay tree as the hand-authored card, or
 * returns null (→ SCAFFOLD) — never a lossy approximation.
 *
 * These live with the other named composite recognizers tried first in [renderEffectList].
 */

/**
 * Erode: `[DestroyPermanent(Ref_TargetPermanent),
 *          PlayerMayAction(ControllerOfTargetPermanent, SearchLibrary(basic land → battlefield tapped, shuffle))]`
 * → `Effects.Destroy(t) then MayEffect(<controller-of-target basic-land fetch pipeline>)`.
 *
 * "Destroy target creature or planeswalker. Its controller may search their library for a basic land
 * card, put it onto the battlefield tapped, then shuffle." The searcher is the *destroyed permanent's
 * controller*, not the spell's controller, so `Patterns.Library.searchLibrary` (always controller-scoped)
 * can't express it — the fetch pipeline is scoped to `Player.ControllerOf("target")` and the gate is
 * delegated to `EffectTarget.TargetController`. Renders only this exact shape (a bare basic-land tutor to
 * the battlefield tapped, then shuffle); any other search arm declines.
 */
internal fun EmitCtx.erodeDestroyControllerFetchEffect(actions: List<JsonObject>): Dsl? {
    if (actions.size != 2) return null
    val (destroy, mayAction) = actions
    if (destroy.strField("_Action") != "DestroyPermanent") return null
    if (!jsonContains(destroy, "_Permanent", "Ref_TargetPermanent")) return null
    if (mayAction.strField("_Action") != "PlayerMayAction") return null
    // The optional searcher must be the controller of the targeted permanent.
    if (!jsonContains(mayAction, "_Player", "ControllerOfTargetPermanent")) return null
    val search = innerAction(mayAction)?.takeIf { it.strField("_Action") == "SearchLibrary" } ?: return null
    val blob = compact(search)
    // Exactly: find a basic land card, put it onto the battlefield tapped, then shuffle.
    val basicLand = "IsSupertype" in blob && "\"Basic\"" in blob &&
        "IsCardtype" in blob && "\"Land\"" in blob
    val toBattlefieldTapped = "PutFoundCardsOntoBattlefield" in blob && "EntersTapped" in blob
    val shuffles = "Shuffle" in blob
    if (!basicLand || !toBattlefieldTapped || !shuffles) return null
    // Decline any extra search arm (reveal, conditional, choice, etc.) we don't render here.
    if ("ChooseAnAction" in blob || "If" in blob || "MayPut" in blob || "ExileFoundCards" in blob) return null

    val destroyEffect = call("Effects.Destroy", arg(Lit("EffectTarget.BoundVariable(\"target\")")))
    val fetch = Composite(
        listOf(
            Raw(
                "GatherCardsEffect(\n" +
                    "                source = CardSource.FromZone(\n" +
                    "                    zone = Zone.LIBRARY,\n" +
                    "                    player = Player.ControllerOf(\"target\"),\n" +
                    "                    filter = GameObjectFilter.BasicLand,\n" +
                    "                ),\n" +
                    "                storeAs = \"searchable\",\n" +
                    "            )",
            ),
            Raw(
                "SelectFromCollectionEffect(\n" +
                    "                from = \"searchable\",\n" +
                    "                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),\n" +
                    "                storeSelected = \"found\",\n" +
                    "            )",
            ),
            Raw(
                "MoveCollectionEffect(\n" +
                    "                from = \"found\",\n" +
                    "                destination = CardDestination.ToZone(\n" +
                    "                    zone = Zone.BATTLEFIELD,\n" +
                    "                    player = Player.ControllerOf(\"target\"),\n" +
                    "                    placement = ZonePlacement.Tapped,\n" +
                    "                ),\n" +
                    "            )",
            ),
            Lit("ShuffleLibraryEffect(target = EffectTarget.TargetController)"),
        ),
    )
    val may = call(
        "MayEffect",
        arg("effect", fetch),
        arg("decisionMaker", "EffectTarget.TargetController"),
    )
    return Composite(listOf(destroyEffect, may))
}

/**
 * Heated Argument: `[SpellDealsDamage(6, Ref_TargetPermanent),
 *                    MayCost(Exile a card from your graveyard),
 *                    If(CostWasPaid)[SpellDealsDamage(2, ControllerOfPermanent Ref_TargetPermanent)]]`
 * → `Effects.DealDamage(6, t) then MayEffect(IfYouDoEffect(<exile-one-from-graveyard pipeline>,
 *      ifYouDo = Effects.DealDamage(2, TargetController), CollectionNonEmpty))`.
 *
 * The optional graveyard exile + "if you do" rider fuse into one gate: the IR's `MayCost(Exile)` becomes
 * the choose-and-exile pipeline, and `If(CostWasPaid)` becomes the `SuccessCriterion.CollectionNonEmpty`
 * gate on the moved pile. Only the exact "exile a card from YOUR graveyard, then deal N to the targeted
 * creature's controller" shape renders; any other cost / condition / recipient declines.
 */
internal fun EmitCtx.heatedArgumentExileRiderEffect(actions: List<JsonObject>): Dsl? {
    if (actions.size != 3) return null
    val (damage, mayCost, ifPaid) = actions
    if (damage.strField("_Action") != "SpellDealsDamage") return null
    if (!jsonContains(damage, "_Permanent", "Ref_TargetPermanent")) return null
    val firstAmt = findInteger(damage["args"]) as? Int ?: return null

    if (mayCost.strField("_Action") != "MayCost") return null
    // The optional cost is "exile a card from your graveyard".
    val costBlob = compact(mayCost["args"])
    val exilesOwnGraveyardCard = "\"Exile\"" in costBlob && "AGraveyardCard" in costBlob &&
        "InAPlayersGraveyard" in costBlob && "\"You\"" in costBlob
    if (!exilesOwnGraveyardCard) return null

    if (ifPaid.strField("_Action") != "If") return null
    val ifArgs = ifPaid["args"].asArr ?: return null
    val cond = ifArgs.getOrNull(0) as? JsonObject ?: return null
    if (cond.strField("_Condition") != "CostWasPaid") return null
    val riderActions = (ifArgs.getOrNull(1) as? JsonArray)
        ?.filterIsInstance<JsonObject>() ?: return null
    val rider = riderActions.singleOrNull()?.takeIf { it.strField("_Action") == "SpellDealsDamage" } ?: return null
    // The rider hits the targeted creature's controller ("...also deals N damage to that creature's controller").
    if (!jsonContains(rider, "_DamageRecipient", "Player") ||
        !jsonContains(rider, "_Player", "ControllerOfPermanent") ||
        !jsonContains(rider, "_Permanent", "Ref_TargetPermanent")
    ) return null
    val riderAmt = findInteger(rider["args"]) as? Int ?: return null

    val firstDamage = call(
        "Effects.DealDamage",
        arg("$firstAmt"),
        arg(Lit("EffectTarget.BoundVariable(\"target\")")),
    )
    val exilePipeline = Composite(
        listOf(
            Lit("GatherCardsEffect(source = CardSource.FromZone(zone = Zone.GRAVEYARD), storeAs = \"graveyardCards\")"),
            Raw(
                "SelectFromCollectionEffect(\n" +
                    "                    from = \"graveyardCards\",\n" +
                    "                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),\n" +
                    "                    storeSelected = \"toExile\",\n" +
                    "                    selectedLabel = \"Exile\",\n" +
                    "                )",
            ),
            Lit("MoveCollectionEffect(from = \"toExile\", destination = CardDestination.ToZone(Zone.EXILE))"),
        ),
    )
    val ifYouDo = call(
        "IfYouDoEffect",
        arg("action", exilePipeline),
        arg("ifYouDo", call("Effects.DealDamage", arg("$riderAmt"), arg("EffectTarget.TargetController"))),
        arg("successCriterion", "SuccessCriterion.CollectionNonEmpty(\"toExile\", min = 1)"),
    )
    return Composite(listOf(firstDamage, call("MayEffect", arg(ifYouDo))))
}

/**
 * Borrowed Knowledge (one modal arm): `[DiscardHand, DrawNumberCards(<count>)]`
 * → `Patterns.Hand.discardHand().then(Effects.DrawCards(<count>))`.
 *
 * "Discard your hand, then draw cards equal to …" — both modes of Borrowed Knowledge share this
 * shape, differing only in the draw count. `Patterns.Hand.discardHand` gathers the controller's hand
 * into the `discardedHand` collection and discards it, so a `NumCardsDiscardedThisWay` count renders as
 * `DynamicAmount.VariableReference("discardedHand_count")` (the count GatherCardsEffect auto-publishes
 * for that collection). A `TheNumberOfCardsInPlayersHand(Ref_TargetPlayer)` count renders as
 * `DynamicAmount.Count(Player.TargetOpponent, Zone.HAND)` — the modal arm targets an opponent (the only
 * player ref this shape supports). Any other count or a non-controller discard target declines (->
 * SCAFFOLD); the discard is always the controller's own hand ("discard your hand").
 *
 * Tried inside [renderEffectList], so it covers the modal arm bodies (each arm calls renderEffectList).
 */
internal fun EmitCtx.discardHandThenDrawEffect(actions: List<JsonObject>): Dsl? {
    if (actions.size != 2) return null
    val (discard, draw) = actions
    if (discard.strField("_Action") != "DiscardHand") return null
    if (draw.strField("_Action") != "DrawNumberCards") return null
    val countNode = draw["args"] as? JsonObject ?: return null
    val drawCount: String = when (countNode.strField("_GameNumber")) {
        "NumCardsDiscardedThisWay" -> "DynamicAmount.VariableReference(\"discardedHand_count\")"
        "TheNumberOfCardsInPlayersHand" -> {
            // Only "target opponent's hand" renders — the modal arm's TargetPlayer Opponent slot,
            // referenced as Ref_TargetPlayer. Any other player scope declines.
            if (!jsonContains(countNode, "_Player", "Ref_TargetPlayer")) return null
            "DynamicAmount.Count(Player.TargetOpponent, Zone.HAND)"
        }
        else -> return null
    }
    return Composite(
        listOf(
            call("Patterns.Hand.discardHand"),
            call("Effects.DrawCards", arg(Lit(drawCount))),
        ),
    )
}

/**
 * Render Speechless: `[PlayerAction(Ref_TargetPlayer,
 *                        RevealHandAndPlayerChoosesACardToDiscard(You, IsNonCardtype Land)),
 *                      PutNumberCountersOfTypeOnPermanent(2, PTCounter(1,1), Ref_TargetPermanent)]`
 * → the Divest-style targeted-discard pipeline (reveal target opponent's hand → controller chooses a
 *   nonland card → that player discards it) then `AddCountersEffect(+1/+1, 2, <creature>)`.
 *
 * Two independently-targeted clauses (`[TargetPlayer Opponent, UptoOneTargetPermanent Creature]`), so
 * this runs inside the multi-target spell block: `Ref_TargetPlayer` resolves to the opponent local
 * (the discarded-from player, addressed in the pipeline by `Player.ContextPlayer(0)` — the first
 * chosen target) and `Ref_TargetPermanent` resolves to the optional creature local. mtgish models the
 * coercive discard as a `PlayerAction` wrapping `RevealHandAndPlayerChoosesACardToDiscard`, which the
 * per-action `PlayerAction` handler can't render, so it's fused here. Renders only this exact shape (a
 * nonland-filtered discard the controller picks + a fixed ±1/±1 counter on the bound target); any other
 * filter, chooser, counter kind, or action declines (-> SCAFFOLD).
 */
internal fun EmitCtx.renderSpeechlessDiscardCountersEffect(actions: List<JsonObject>): Dsl? {
    if (actions.size != 2) return null
    val (playerAction, counters) = actions
    if (playerAction.strField("_Action") != "PlayerAction") return null
    // The acting player is the targeted opponent (Ref_TargetPlayer).
    if (!jsonContains(playerAction, "_Player", "Ref_TargetPlayer")) return null
    val reveal = innerAction(playerAction)
        ?.takeIf { it.strField("_Action") == "RevealHandAndPlayerChoosesACardToDiscard" } ?: return null
    val revealArgs = reveal["args"].asArr ?: return null
    // The chooser is the spell's controller ("You choose a card from it").
    if ((revealArgs.getOrNull(0) as? JsonObject)?.strField("_Player") != "You") return null
    // The choosable card is filtered to nonland (IsNonCardtype Land); any other filter declines.
    val filterNode = revealArgs.getOrNull(1) as? JsonObject ?: return null
    val isNonland = filterNode.strField("_Cards") == "IsNonCardtype" &&
        filterNode["args"].asStr() == "Land"
    if (!isNonland) return null

    // "Put two +1/+1 counters on up to one target creature."
    if (counters.strField("_Action") != "PutNumberCountersOfTypeOnPermanent") return null
    val counterArgs = counters["args"].asArr ?: return null
    val count = findInteger(counterArgs.getOrNull(0)) as? Int ?: return null
    val counterType = counterTypeDsl(counterArgs.getOrNull(1)) ?: return null
    if (!jsonContains(counterArgs.getOrNull(2), "_Permanent", "Ref_TargetPermanent")) return null
    val creatureTarget = refTarget(counterArgs.getOrNull(2), null) ?: return null

    val discardPipeline = Composite(
        listOf(
            Lit("RevealHandEffect(EffectTarget.ContextTarget(0))"),
            Lit(
                "GatherCardsEffect(source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)), " +
                    "storeAs = \"opponentHand\")",
            ),
            Raw(
                "SelectFromCollectionEffect(\n" +
                    "                from = \"opponentHand\",\n" +
                    "                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),\n" +
                    "                chooser = Chooser.Controller,\n" +
                    "                filter = GameObjectFilter.Nonland,\n" +
                    "                storeSelected = \"toDiscard\",\n" +
                    "                prompt = \"Choose a nonland card to discard\",\n" +
                    "                alwaysPrompt = true,\n" +
                    "                showAllCards = true,\n" +
                    "            )",
            ),
            Lit(
                "MoveCollectionEffect(from = \"toDiscard\", destination = " +
                    "CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)), moveType = MoveType.Discard)",
            ),
        ),
    )
    val addCounters = call(
        "AddCountersEffect",
        arg("counterType", counterType),
        arg("count", "$count"),
        arg("target", creatureTarget),
    )
    return Composite(listOf(discardPipeline, addCounters))
}

/**
 * Mana Sculpt: `[CounterSpell(Ref_TargetSpell),
 *                If(PlayerPassesFilter(You, ControlsA(Wizard)))
 *                  [CreateFutureTrigger(AtTheBeginningOfPlayersNextMainPhase(You),
 *                     [AddManaRepeated(C, AmountOfManaSpentToCastSpell(Ref_TargetSpell))])]]`
 * → `ConditionalEffect(YouControl(Wizard), CreateDelayedTriggerEffect(PRECOMBAT_MAIN, fireOnPlayer = You,
 *      AddColorlessManaEffect(targetManaSpent(0)))) then Effects.CounterSpell()`.
 *
 * "Counter target spell. If you control a Wizard, add an amount of {C} equal to the amount of mana spent
 * to cast that spell at the beginning of your next main phase." The delayed trigger fires at the
 * controller's next precombat main phase; the mana-spent amount references the (now-countered) target
 * spell — the engine snapshots it at delayed-trigger creation time, so the gated trigger creation is
 * sequenced before the counter. Renders only this exact shape (control-a-Wizard gate, your-next-main-
 * phase timing, {C} = mana spent to cast the target spell); any other condition / timing / mana declines.
 */
internal fun EmitCtx.manaSculptCounterWizardManaEffect(actions: List<JsonObject>): Dsl? {
    if (actions.size != 2) return null
    val (counter, ifWizard) = actions
    if (counter.strField("_Action") != "CounterSpell") return null
    if (!jsonContains(counter, "_Spell", "Ref_TargetSpell")) return null

    if (ifWizard.strField("_Action") != "If") return null
    val ifArgs = ifWizard["args"].asArr ?: return null
    val cond = ifArgs.getOrNull(0) as? JsonObject ?: return null
    // "if you control a Wizard".
    val condBlob = compact(cond)
    val controlsWizard = cond.strField("_Condition") == "PlayerPassesFilter" &&
        "\"You\"" in condBlob && "ControlsA" in condBlob &&
        "IsCreatureType" in condBlob && "\"Wizard\"" in condBlob
    if (!controlsWizard) return null

    val thenActions = (ifArgs.getOrNull(1) as? JsonArray)
        ?.filterIsInstance<JsonObject>() ?: return null
    val future = thenActions.singleOrNull()?.takeIf { it.strField("_Action") == "CreateFutureTrigger" } ?: return null
    val futureArgs = future["args"].asArr ?: return null
    val timing = futureArgs.getOrNull(0) as? JsonObject ?: return null
    // "at the beginning of your next main phase".
    if (timing.strField("_FutureTrigger") != "AtTheBeginningOfPlayersNextMainPhase") return null
    if (!jsonContains(timing, "_Player", "You")) return null
    val futureBody = (futureArgs.getOrNull(1) as? JsonObject)?.get("args").asArr
        ?.filterIsInstance<JsonObject>() ?: return null
    val addMana = futureBody.singleOrNull()?.takeIf { it.strField("_Action") == "AddManaRepeated" } ?: return null
    val addBlob = compact(addMana)
    // {C} equal to the amount of mana spent to cast THAT (countered) spell.
    val colorlessManaSpent = "ManaProduceC" in addBlob &&
        "AmountOfManaSpentToCastSpell" in addBlob && "Ref_TargetSpell" in addBlob
    if (!colorlessManaSpent) return null

    val delayedTrigger = call(
        "CreateDelayedTriggerEffect",
        arg("step", "Step.PRECOMBAT_MAIN"),
        arg("fireOnPlayer", "EffectTarget.PlayerRef(Player.You)"),
        arg("effect", call("Effects.AddColorlessMana", arg(Lit("DynamicAmounts.targetManaSpent(0)")))),
    )
    val gated = call(
        "ConditionalEffect",
        arg("condition", "Conditions.YouControl(GameObjectFilter.Creature.withSubtype(\"Wizard\"))"),
        arg("effect", delayedTrigger),
    )
    return Composite(listOf(gated, call("Effects.CounterSpell")))
}

/**
 * Wisdom of Ages — a whole-spell recognizer (returns the full `spell { … }` statement list, since
 * the trailing `ExileSpell(ThisSpell)` lowers to the envelope flag `selfExile()`, not an effect):
 *
 * `SpellActions { ActionList[
 *     ReturnEachCardFromGraveyardToHand(Or(IsCardtype Instant, IsCardtype Sorcery), You),
 *     CreatePlayerEffect(You, [HasNoMaximumHandSize]),
 *     ExileSpell(ThisSpell) ] }`
 * →
 * ```
 * spell {
 *     selfExile()
 *     effect = Effects.Composite(
 *         Effects.Pipeline { val cards = gather(CardSource.FromZone(Zone.GRAVEYARD,
 *             filter = GameObjectFilter.InstantOrSorcery)); toHand(cards) },
 *         Effects.RemoveMaximumHandSize(),
 *     )
 * }
 * ```
 *
 * The gather→toHand pipeline is emitted as the explicit `GatherCardsEffect(storeAs = "gathered0")`
 * + `MoveCollectionEffect(from = "gathered0", → Hand)` pair wrapped in `Effects.Composite(...)`,
 * which is exactly the nested tree the hand-authored card's inline `Effects.Pipeline { gather; toHand }`
 * builder produces (deterministic `gathered0` slot key). Matches only this exact shape (instant-or-
 * sorcery from YOUR graveyard, no-maximum-hand-size for the rest of the game, self-exile); any other
 * filter / player-effect / exile target declines (→ SCAFFOLD).
 */
internal fun EmitCtx.wisdomOfAgesSpell(card: JsonObject): List<Stmt>? {
    val (_, actions) = extractEnvelope(card["Rules"])
    if (actions == null || actions.size != 3) return null
    val (ret, playerEffect, exile) = actions

    // 1. Return all instant AND sorcery cards from your graveyard to your hand. (The IR carries no
    // explicit player scope on this action — graveyard return defaults to the controller's graveyard.)
    if (ret.strField("_Action") != "ReturnEachCardFromGraveyardToHand") return null
    val retBlob = compact(ret)
    val isInstantOrSorcery = "\"Or\"" in retBlob &&
        "IsCardtype" in retBlob && "\"Instant\"" in retBlob && "\"Sorcery\"" in retBlob
    if (!isInstantOrSorcery) return null

    // 2. "You have no maximum hand size for the rest of the game."
    if (playerEffect.strField("_Action") != "CreatePlayerEffect") return null
    if (!jsonContains(playerEffect, "_Player", "You")) return null
    if (!jsonContains(playerEffect, "_PlayerEffect", "HasNoMaximumHandSize")) return null

    // 3. Exile this spell (→ selfExile() on the spell envelope).
    if (exile.strField("_Action") != "ExileSpell") return null
    if (!jsonContains(exile, "_Spell", "ThisSpell")) return null

    val returnPipeline = Composite(
        listOf(
            Lit(
                "GatherCardsEffect(source = CardSource.FromZone(Zone.GRAVEYARD, " +
                    "filter = GameObjectFilter.InstantOrSorcery), storeAs = \"gathered0\")",
            ),
            Lit("MoveCollectionEffect(from = \"gathered0\", destination = CardDestination.ToZone(Zone.HAND))"),
        ),
    )
    val effect = call(
        "Effects.Composite",
        arg(returnPipeline),
        arg(call("Effects.RemoveMaximumHandSize")),
    )
    return listOf(
        Sub(
            Block(
                "spell",
                listOf(
                    RawLine("        selfExile()"),
                    Assign("effect", effect),
                ),
            ),
        ),
    )
}

/**
 * Boilerbilges Ripper — the "you may [pay a sacrifice cost]. When you do, [reflexive effect targeting]"
 * idiom. mtgish models it as two sibling actions inside the ETB triggered ability:
 *
 * `[ MayCost(SacrificeAPermanent: And(Other ThisPermanent, Or(IsCardtype Creature, IsCardtype Enchantment))),
 *    If(CostWasPaid)[ ReflexiveTrigger(Targeted([AnyTarget],
 *        ActionList[ PermanentDealsDamage(ThisPermanent, 2, Ref_AnyTarget) ])) ] ]`
 * →
 * ```
 * ReflexiveTriggerEffect(
 *     action = Effects.Composite(listOf(
 *         SelectTargetEffect(requirement = TargetObject(filter =
 *             TargetFilter.CreatureOrEnchantment.youControl().other()), storeAs = "permanentToSacrifice"),
 *         Effects.SacrificeTarget(EffectTarget.PipelineTarget("permanentToSacrifice")))),
 *     optional = true,
 *     reflexiveEffect = Effects.DealDamage(2, EffectTarget.ContextTarget(0), damageSource = EffectTarget.Self),
 *     reflexiveTargetRequirements = listOf(<reflexive target>))
 * ```
 *
 * The optional sacrifice is a resolution-time CHOICE (not a target), so it's a select-then-sacrifice
 * pipeline whose chosen permanent the SacrificeTarget reads via the pipeline slot; the "When you do"
 * reflexive trigger then chooses its own target as it goes on the stack (`ContextTarget(0)`). The damage
 * source is the source permanent (Self). Renders ONLY this exact shape — sacrifice "another creature or
 * enchantment" (you control), a CostWasPaid-gated single reflexive trigger whose sole action is the
 * source dealing a fixed amount of damage to its chosen target — and only when the reflexive target
 * round-trips through [targetExpr]; anything else declines (-> SCAFFOLD).
 */
internal fun EmitCtx.mayCostReflexiveDamageEffect(actions: List<JsonObject>): Dsl? {
    if (actions.size != 2) return null
    val (mayCost, ifPaid) = actions
    if (mayCost.strField("_Action") != "MayCost") return null
    // The optional cost must be "sacrifice another creature or enchantment (you control)".
    val cost = mayCost["args"] as? JsonObject ?: return null
    if (cost.strField("_Cost") != "SacrificeAPermanent") return null
    val costBlob = compact(cost["args"])
    val sacFilter = "\"Other\"" in costBlob && "ThisPermanent" in costBlob &&
        "\"Or\"" in costBlob && "IsCardtype" in costBlob &&
        "\"Creature\"" in costBlob && "\"Enchantment\"" in costBlob &&
        "ControlledByAPlayer" !in costBlob && "IsCreatureType" !in costBlob
    if (!sacFilter) return null

    // The gate must be exactly If(CostWasPaid)[ ReflexiveTrigger(...) ] — no else-branch.
    if (ifPaid.strField("_Action") != "If") return null
    val ifArgs = ifPaid["args"].asArr ?: return null
    if (!jsonContains(ifArgs.getOrNull(0), "_Condition", "CostWasPaid") || ifArgs.getOrNull(2) != null) return null
    val reflexive = (ifArgs.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>()
        ?.singleOrNull()?.takeIf { it.strField("_Action") == "ReflexiveTrigger" } ?: return null

    // The reflexive trigger's Targeted envelope: a single AnyTarget, one PermanentDealsDamage action.
    val (rTargets, rActions) = extractEnvelope(reflexive)
    val reflexiveTargetNode = rTargets?.singleOrNull() ?: return null
    val reflexiveTarget = targetExpr(reflexiveTargetNode) ?: return null
    val damage = rActions?.singleOrNull()?.takeIf { it.strField("_Action") == "PermanentDealsDamage" } ?: return null
    // The damage source is this permanent; recipient is the reflexive AnyTarget (Ref_AnyTarget).
    if (!jsonContains(damage["args"].asArr?.firstOrNull(), "_Permanent", "ThisPermanent")) return null
    if (!jsonContains(damage, "_DamageRecipient", "Ref_AnyTarget")) return null
    val amt = findInteger(damage["args"]) as? Int ?: return null

    val action = call(
        "Effects.Composite",
        arg(call(
            "listOf",
            arg(call(
                "SelectTargetEffect",
                arg("requirement", call(
                    "TargetObject",
                    arg("filter", Lit("TargetFilter.CreatureOrEnchantment.youControl().other()")),
                )),
                arg("storeAs", Lit("\"permanentToSacrifice\"")),
            )),
            arg(call("Effects.SacrificeTarget", arg(Lit("EffectTarget.PipelineTarget(\"permanentToSacrifice\")")))),
        )),
    )
    return call(
        "ReflexiveTriggerEffect",
        arg("action", action),
        arg("optional", Lit("true")),
        arg("reflexiveEffect", call(
            "Effects.DealDamage",
            arg("$amt"),
            arg("EffectTarget.ContextTarget(0)"),
            arg("damageSource", Lit("EffectTarget.Self")),
        )),
        arg("reflexiveTargetRequirements", call("listOf", arg(reflexiveTarget))),
    )
}
