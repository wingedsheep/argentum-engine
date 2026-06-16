package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.Composite
import com.wingedsheep.tooling.coverage.Dsl
import com.wingedsheep.tooling.coverage.Lit
import com.wingedsheep.tooling.coverage.Raw
import com.wingedsheep.tooling.coverage.arg
import com.wingedsheep.tooling.coverage.asArr
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
