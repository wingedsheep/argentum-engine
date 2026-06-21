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
import com.wingedsheep.tooling.coverage.argWordsTagged
import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.asStr
import com.wingedsheep.tooling.coverage.call
import com.wingedsheep.tooling.coverage.compact
import com.wingedsheep.tooling.coverage.dot
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
 * Colossus of the Blood Age (dies trigger): `[DiscardAnyNumberOfCards,
 *   DrawNumberCards(Plus(NumCardsDiscardedThisWay, <k>))]`
 * → the inline Gather → ChooseAnyNumber(discard) → draw "that many plus <k>" pipeline.
 *
 * "Discard any number of cards, then draw that many cards plus <k>." The controller discards any
 * subset of their own hand, then draws the discarded count plus a fixed bonus. Rendered as the exact
 * `gathered0 / selected1` raw-step Composite the inline `Effects.Pipeline { }` builder produces (the
 * same idiom as ReturnAnyNumberOfPermanentsToTheirOwnersHands), so the serialized tree matches the
 * hand-authored card. The discard is always the controller's own hand (no chooser/target variants); the
 * draw count is `selected1_count` (+ the bonus). Any other discard scope or a non-`NumCardsDiscardedThisWay`
 * /-non-integer draw shape declines (→ SCAFFOLD) rather than approximate.
 *
 * Tried inside [renderEffectList], so it covers both spell bodies and triggered-ability bodies.
 */
internal fun EmitCtx.discardAnyNumberThenDrawEffect(actions: List<JsonObject>): Dsl? {
    if (actions.size != 2) return null
    val (discard, draw) = actions
    if (discard.strField("_Action") != "DiscardAnyNumberOfCards") return null
    if (draw.strField("_Action") != "DrawNumberCards") return null
    val countNode = draw["args"] as? JsonObject ?: return null
    // The draw count must be Plus(NumCardsDiscardedThisWay, <integer bonus>).
    if (countNode.strField("_GameNumber") != "Plus") return null
    val terms = countNode["args"].asArr ?: return null
    if (terms.size != 2) return null
    val hasDiscardedRef = terms.any {
        (it as? JsonObject)?.strField("_GameNumber") == "NumCardsDiscardedThisWay"
    }
    if (!hasDiscardedRef) return null
    val bonus = terms.firstNotNullOfOrNull { findInteger(it) as? Int } ?: return null
    val drawCount = "DynamicAmount.Add(" +
        "DynamicAmount.VariableReference(\"selected1_count\"), DynamicAmount.Fixed($bonus))"
    return call(
        "Effects.Composite",
        arg(Lit("GatherCardsEffect(CardSource.FromZone(Zone.HAND, Player.You), storeAs = \"gathered0\")")),
        arg(Lit(
            "SelectFromCollectionEffect(from = \"gathered0\", selection = SelectionMode.ChooseAnyNumber, " +
                "storeSelected = \"selected1\", prompt = \"Choose any number of cards to discard\")"
        )),
        arg(Lit(
            "MoveCollectionEffect(from = \"selected1\", " +
                "destination = CardDestination.ToZone(Zone.GRAVEYARD), moveType = MoveType.Discard)"
        )),
        arg(Lit("DrawCardsEffect($drawCount)")),
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
/**
 * Run Behind (bounce arm): `[PlayerAction(OwnerOfPermanent(Ref_TargetPermanent),
 *                              ReturnPermanentToTopOrBottomOfLibrary(Ref_TargetPermanent))]`
 * → `Effects.PutOnTopOrBottomOfLibrary(<bound target>)`.
 *
 * "Target creature's owner puts it on their choice of the top or bottom of their library." The engine's
 * `PutOnTopOrBottomOfLibrary` already pauses for the *owner* to choose top or bottom, so the IR's
 * `PlayerAction(OwnerOfPermanent, …)` wrapper (which the per-action `PlayerAction` handler can't render)
 * collapses onto it exactly. Renders ONLY this shape — owner-of-target choosing top/bottom for the bound
 * permanent; any other player scope, action, or extra sibling declines (-> SCAFFOLD). The "{1} less if it
 * targets an attacking creature" half is a separate `CastEffect` rule the cost-reduction pass renders.
 */
internal fun EmitCtx.runBehindOwnerTopOrBottomEffect(actions: List<JsonObject>, tvar: String?): Dsl? {
    val only = actions.singleOrNull() ?: return null
    if (only.strField("_Action") != "PlayerAction") return null
    // The acting player must be the OWNER of the targeted permanent ("that creature's owner puts it…").
    if (!jsonContains(only, "_Player", "OwnerOfPermanent")) return null
    if (!jsonContains(only, "_Permanent", "Ref_TargetPermanent")) return null
    val inner = innerAction(only)
        ?.takeIf { it.strField("_Action") == "ReturnPermanentToTopOrBottomOfLibrary" } ?: return null
    if (!jsonContains(inner, "_Permanent", "Ref_TargetPermanent")) return null
    val target = refTarget(inner["args"], tvar) ?: return null
    return call("Effects.PutOnTopOrBottomOfLibrary", arg(Lit(target)))
}

/**
 * Dina's Guidance: `[SearchLibrary(FindACardOfType(Creature), RevealFoundCards,
 *                      ChooseAnAction[PutFoundCardsIntoHand, PutFoundCardsIntoGraveyard], Shuffle)]`
 * → the gather → choose → reveal → split-move (hand vs graveyard) pipeline the hand-authored card uses.
 *
 * "Search your library for a creature card, reveal it, put it into your hand or graveyard, then shuffle."
 * The destination is a *player choice* (`ChooseAnAction[…IntoHand, …IntoGraveyard]`), which the generic
 * `Patterns.Library.searchLibrary` (single fixed `SearchDestination`) can't express — it explicitly
 * declines `ChooseAnAction`. Rendered here as the exact `Effects.Pipeline { }` tree the hand-authored
 * card produces: gather creatures, `chooseUpTo(1)` (the search may fail to find), reveal, a binary split
 * over the found card (selecting → hand, leaving → graveyard), then shuffle. Renders ONLY this exact
 * shape (find a creature card, reveal, choose hand-or-graveyard, shuffle); any other filter, extra search
 * arm, or destination set declines (-> SCAFFOLD).
 */
internal fun EmitCtx.dinasGuidanceSearchHandOrGraveyardEffect(actions: List<JsonObject>): Dsl? {
    val only = actions.singleOrNull() ?: return null
    if (only.strField("_Action") != "SearchLibrary") return null
    val searchArgs = only["args"].asArr?.filterIsInstance<JsonObject>() ?: return null
    // 1. Find a creature card.
    val find = searchArgs.getOrNull(0)?.takeIf { it.strField("_SearchLibraryAction") == "FindACardOfType" } ?: return null
    val findBlob = compact(find)
    if (!("IsCardtype" in findBlob && "\"Creature\"" in findBlob)) return null
    // 2. Reveal the found card.
    if (searchArgs.getOrNull(1)?.strField("_SearchLibraryAction") != "RevealFoundCards") return null
    // 3. Choose: put into hand OR graveyard.
    val choose = searchArgs.getOrNull(2)?.takeIf { it.strField("_SearchLibraryAction") == "ChooseAnAction" } ?: return null
    val chooseArms = choose["args"].asArr?.filterIsInstance<JsonObject>()?.map { it.strField("_SearchLibraryAction") } ?: return null
    if (chooseArms.toSet() != setOf("PutFoundCardsIntoHand", "PutFoundCardsIntoGraveyard")) return null
    // 4. Shuffle — and nothing else.
    if (searchArgs.getOrNull(3)?.strField("_SearchLibraryAction") != "Shuffle") return null
    if (searchArgs.size != 4) return null

    return Raw(
        "Effects.Pipeline {\n" +
            "            val pool = gather(\n" +
            "                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Creature)\n" +
            "            )\n" +
            "            val found = chooseUpTo(\n" +
            "                1,\n" +
            "                from = pool,\n" +
            "                prompt = \"Search your library for a creature card\",\n" +
            "            )\n" +
            "            reveal(found)\n" +
            "            val placed = chooseUpToSplit(\n" +
            "                1,\n" +
            "                from = found,\n" +
            "                prompt = \"Put the creature card into your hand or graveyard\",\n" +
            "                selectedLabel = \"Put into your hand\",\n" +
            "                remainderLabel = \"Put into your graveyard\",\n" +
            "            )\n" +
            "            move(placed.selected, CardDestination.ToZone(Zone.HAND))\n" +
            "            move(placed.remainder, CardDestination.ToZone(Zone.GRAVEYARD))\n" +
            "            run(ShuffleLibraryEffect())\n" +
            "        }",
    )
}

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

/**
 * Anthropede — the "you may [discard a card or pay {N}]. When you do, [reflexive effect targeting]"
 * idiom, the two-way-cost sibling of [mayCostReflexiveDamageEffect]. mtgish models it as two sibling
 * actions inside the ETB triggered ability, where the optional cost is an `Or` of two payment options:
 *
 * `[ MayCost(Or[ DiscardACard, PayMana {N} ]),
 *    If(CostWasPaid)[ ReflexiveTrigger(Targeted([TargetPermanent IsEnchantmentType:Room],
 *        ActionList[ DestroyPermanent Ref_TargetPermanent ])) ] ]`
 * →
 * ```
 * ReflexiveTriggerEffect(
 *     action = ChooseActionEffect(choices = listOf(
 *         EffectChoice("Discard a card", Patterns.Hand.discardCards(1),
 *             feasibilityCheck = FeasibilityCheck.HasCardsInZone(Zone.HAND)),
 *         EffectChoice("Pay {N}", PayManaCostEffect(ManaCost.parse("{N}"))))),
 *     optional = true,
 *     reflexiveEffect = Effects.Destroy(EffectTarget.ContextTarget(0)),
 *     reflexiveTargetRequirements = listOf(<reflexive Room target>))
 * ```
 *
 * The "you may [discard / pay]" is a resolution-time CHOICE between two payments (not a target), so it's a
 * `ChooseActionEffect` with one [EffectChoice] per arm; the discard arm carries the `HasCardsInZone(HAND)`
 * feasibility check (you can't choose to discard with an empty hand). The "When you do" reflexive trigger
 * then chooses its own target as it goes on the stack (`ContextTarget(0)`). Renders ONLY this exact shape —
 * a two-arm `Or[DiscardACard, PayMana]` optional cost, a `CostWasPaid`-gated single reflexive trigger whose
 * sole action is destroying its chosen Room target — and only when the reflexive Room target round-trips;
 * anything else declines (-> SCAFFOLD).
 */
internal fun EmitCtx.mayCostReflexiveDestroyRoomEffect(actions: List<JsonObject>): Dsl? {
    if (actions.size != 2) return null
    val (mayCost, ifPaid) = actions
    if (mayCost.strField("_Action") != "MayCost") return null
    // The optional cost must be exactly Or[ DiscardACard, PayMana {N} ] — a two-arm choice between
    // discarding a card and paying a fixed mana amount (no filter on the discard, no other arm).
    val cost = mayCost["args"] as? JsonObject ?: return null
    if (cost.strField("_Cost") != "Or") return null
    val arms = cost["args"].asArr?.filterIsInstance<JsonObject>() ?: return null
    if (arms.size != 2) return null
    val discardArm = arms.firstOrNull { it.strField("_Cost") == "DiscardACard" } ?: return null
    // The bare "discard a card" cost carries no filter/count args — anything richer declines.
    if (discardArm["args"] != null) return null
    val payArm = arms.firstOrNull { it.strField("_Cost") == "PayMana" } ?: return null
    val mana = renderMana(payArm["args"])
    if (mana.isBlank() || "{?}" in mana) return null

    // The gate must be exactly If(CostWasPaid)[ ReflexiveTrigger(...) ] — no else-branch.
    if (ifPaid.strField("_Action") != "If") return null
    val ifArgs = ifPaid["args"].asArr ?: return null
    if (!jsonContains(ifArgs.getOrNull(0), "_Condition", "CostWasPaid") || ifArgs.getOrNull(2) != null) return null
    val reflexive = (ifArgs.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>()
        ?.singleOrNull()?.takeIf { it.strField("_Action") == "ReflexiveTrigger" } ?: return null

    // The reflexive trigger's Targeted envelope: a single Room target, one DestroyPermanent action.
    val (rTargets, rActions) = extractEnvelope(reflexive)
    val reflexiveTargetNode = rTargets?.singleOrNull() ?: return null
    val reflexiveTarget = enchantmentSubtypeTargetExpr(reflexiveTargetNode) ?: return null
    val destroy = rActions?.singleOrNull()?.takeIf { it.strField("_Action") == "DestroyPermanent" } ?: return null
    // The destroyed permanent is the reflexive trigger's chosen target (Ref_TargetPermanent).
    if (!jsonContains(destroy["args"], "_Permanent", "Ref_TargetPermanent")) return null

    val action = call(
        "ChooseActionEffect",
        arg("choices", call(
            "listOf",
            arg(call(
                "EffectChoice",
                arg(Lit("\"Discard a card\"")),
                arg(call("Patterns.Hand.discardCards", arg("1"))),
                arg("feasibilityCheck", Lit("FeasibilityCheck.HasCardsInZone(Zone.HAND)")),
            )),
            arg(call(
                "EffectChoice",
                arg(Lit("\"Pay $mana\"")),
                arg(call("PayManaCostEffect", arg(Lit("ManaCost.parse(\"$mana\")")))),
            )),
        )),
    )
    return call(
        "ReflexiveTriggerEffect",
        arg("action", action),
        arg("optional", Lit("true")),
        arg("reflexiveEffect", call("Effects.Destroy", arg(Lit("EffectTarget.ContextTarget(0)")))),
        arg("reflexiveTargetRequirements", call("listOf", arg(reflexiveTarget))),
    )
}

/**
 * A `TargetPermanent` whose only restriction is a single enchantment subtype (`IsEnchantmentType:<sub>`,
 * e.g. "target Room") -> `TargetPermanent(filter = TargetFilter(GameObjectFilter.Permanent.withSubtype(
 * <sub>)))`. The generic [targetExpr] declines a bare enchantment-subtype permanent target (it has no
 * single GameObjectFilter for it and refuses to widen to "any enchantment"); this narrow helper recovers
 * exactly the permanent-with-subtype shape the hand-authored card uses. Only the lone subtype renders — a
 * count, controller clause, self-exclusion, or any other predicate declines (-> SCAFFOLD).
 */
private fun EmitCtx.enchantmentSubtypeTargetExpr(tnode: JsonObject): Dsl? {
    if (tnode.strField("_Target") != "TargetPermanent") return null
    val args = tnode["args"]
    val subs = args.argWordsTagged("IsEnchantmentType")
    if (subs.size != 1) return null
    val blob = compact(args)
    // Reject anything beyond the bare enchantment-subtype clause so a dropped restriction never widens
    // the target (no controller scope, no count, no self-exclusion, no other type/predicate).
    val extras = listOf(
        "ControlledByAPlayer", "Other", "IsTapped", "IsUntapped", "IsAttacking", "IsBlocking",
        "PowerIs", "ToughnessIs", "ManaValueIs", "ManaValueAtMost", "ManaValueAtLeast", "IsColor",
        "IsNonColor", "HasAbility", "DoesntHaveAbility", "IsNonToken", "IsToken", "IsSupertype",
        "IsNonSupertype", "IsCardtype", "IsNonCardtype", "IsCreatureType", "IsNonCreatureType",
        "IsArtifactType", "IsLandType", "HasACounterOfType",
    )
    if (extras.any { it in blob }) return null
    val base = Lit("GameObjectFilter.Permanent").dot("withSubtype", arg(subtypeArg(subs[0])))
    return call("TargetPermanent", arg("filter", call("TargetFilter", arg(base))))
}

/**
 * Lumaret's Favor (Infusion copy) — a whole `FromStack` rule recognizer (returns the
 * `triggeredAbility { … }` statement list):
 *
 * `FromStack { TriggerA[
 *     WhenAPlayerCastsASpell(You, ThisSpell),
 *     ActionList[ If(PlayerPassesFilter(You, GainedLifeThisTurn))
 *                   [ CopySpellAndMayChooseNewTargets(Trigger_ThatSpell) ] ] ] }`
 * →
 * ```
 * triggeredAbility {
 *     trigger = Triggers.WhenYouCastThisSpell()
 *     triggerCondition = Conditions.YouGainedLifeThisTurn
 *     effect = Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity)
 * }
 * ```
 *
 * "Infusion — When you cast this spell, copy it if you gained life this turn. You may choose new targets
 * for the copy." The Infusion copy is the cast-trigger sibling of Social Snub, but **mandatory** (no "you
 * may") and gated by the Infusion intervening-"if" (`GainedLifeThisTurn`). The IR wraps the cast trigger
 * in a top-level `FromStack` rule — the per-rule dispatch has no `FromStack` handler, so this fuses the
 * whole shape. Renders ONLY this exact shape (cast-self trigger, `GainedLifeThisTurn` gate, a lone
 * mandatory copy-with-new-targets of the triggering spell); any other trigger, condition, or body
 * declines (-> SCAFFOLD). The "Target creature gets +2/+4" body is the separate `SpellActions` rule.
 */
internal fun EmitCtx.lumaretsFavorInfusionCopyBlock(rule: JsonObject): List<Stmt>? {
    if (rule.strField("_Rule") != "FromStack") return null
    val trigger = rule["args"] as? JsonObject ?: return null
    if (trigger.strField("_Rule") != "TriggerA") return null
    val trigArgs = trigger["args"].asArr ?: return null

    // 1. Trigger: WhenAPlayerCastsASpell(You, ThisSpell).
    val trig = trigArgs.getOrNull(0) as? JsonObject ?: return null
    if (trig.strField("_Trigger") != "WhenAPlayerCastsASpell") return null
    val whenArgs = trig["args"].asArr ?: return null
    if (!jsonContains(whenArgs.getOrNull(0), "_Player", "You")) return null
    if (!jsonContains(whenArgs.getOrNull(1), "_Spell", "ThisSpell")) return null

    // 2. Sole action: If(PlayerPassesFilter(You, GainedLifeThisTurn))[ CopySpellAndMayChooseNewTargets ].
    val actionsNode = trigArgs.getOrNull(1) as? JsonObject ?: return null
    val actions = actionsNode["args"].asArr?.filterIsInstance<JsonObject>() ?: return null
    val ifAction = actions.singleOrNull()?.takeIf { it.strField("_Action") == "If" } ?: return null
    val ifArgs = ifAction["args"].asArr ?: return null
    val cond = ifArgs.getOrNull(0) as? JsonObject ?: return null
    // "if you gained life this turn".
    val condBlob = compact(cond)
    val gainedLife = cond.strField("_Condition") == "PlayerPassesFilter" &&
        "\"You\"" in condBlob && "GainedLifeThisTurn" in condBlob
    if (!gainedLife) return null
    // No else-branch (a plain intervening-if).
    if (ifArgs.getOrNull(2) != null) return null

    val thenActions = (ifArgs.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return null
    val copy = thenActions.singleOrNull()?.takeIf { it.strField("_Action") == "CopySpellAndMayChooseNewTargets" } ?: return null
    // The copy is of the triggering spell.
    if (copy["args"].strField("_Spell") != "Trigger_ThatSpell") return null

    return listOf(
        Sub(Block("triggeredAbility", listOf(
            Assign("trigger", call("Triggers.WhenYouCastThisSpell")),
            Assign("triggerCondition", Lit("Conditions.YouGainedLifeThisTurn")),
            Assign("effect", call("Effects.CopyTargetSpell", arg("target", "EffectTarget.TriggeringEntity"))),
        ))),
    )
}

/**
 * Improvisation Capstone: `[ExileTopCardsOfLibraryUntilGroupCardsAreExiled(TotalManaValueIs >= N),
 *                           CastAnyNumberOfSpellsFromExileWithoutPaying(AnySpell, TheCardsExiledThisWay)]`
 * → `Effects.ExileLibraryUntilManaValue(Player.You, N, storeAs) + GrantMayPlayFromExileEffect +
 *    GrantPlayWithoutPayingCostEffect` (the Dream Harvest impulse-and-free-cast shape, on your own
 *    library).
 *
 * The mtgish IR splits this into a self-library "exile until total mana value ≥ N" action and a
 * separate "cast any number of the cards exiled this way for free" action; the engine fuses them with
 * a shared pipeline collection. Renders only this exact shape with an integer threshold and the
 * cast scoped to `TheCardsExiledThisWay`; any other group filter, threshold, or cast scope declines
 * (→ SCAFFOLD) rather than emit a lossy approximation.
 */
internal fun EmitCtx.improvisationExileUntilCastFreeEffect(actions: List<JsonObject>): Dsl? {
    if (actions.size != 2) return null
    val (exile, cast) = actions
    if (exile.strField("_Action") != "ExileTopCardsOfLibraryUntilGroupCardsAreExiled") return null
    if (cast.strField("_Action") != "CastAnyNumberOfSpellsFromExileWithoutPaying") return null
    // The stop condition must be "total mana value ≥ <integer>".
    val gf = exile["args"] as? JsonObject ?: return null
    if (gf.strField("_GroupFilter") != "TotalManaValueIs") return null
    val cmp = gf["args"] as? JsonObject ?: return null
    if (cmp.strField("_Comparison") != "GreaterThanOrEqualTo") return null
    val threshold = findInteger(cmp["args"]) as? Int ?: return null
    // The free cast must be of any spell among the cards exiled this way.
    if (!jsonContains(cast["args"], "_Spells", "AnySpell")) return null
    if (!jsonContains(cast["args"], "_CardsInExile", "TheCardsExiledThisWay")) return null
    return Composite(listOf(
        Lit("Effects.ExileLibraryUntilManaValue(players = Player.You, threshold = $threshold, storeAs = \"exiled\")"),
        Lit("GrantMayPlayFromExileEffect(\"exiled\")"),
        Lit("GrantPlayWithoutPayingCostEffect(\"exiled\")"),
    ))
}
