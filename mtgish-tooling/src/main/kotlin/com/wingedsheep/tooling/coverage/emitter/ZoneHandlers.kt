package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.compact
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.strField
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Zone movement: destroy / bounce / reanimate / search / look / mill. Argentum has no leaf
 *  destroy/discard verb — they compose from MoveToZone (single) / MoveCollection (mass). */
internal val zoneHandlers: Map<String, ActionHandler> = actionHandlers {

    on("PutEachPermanentIntoItsOwnersHand") { node, _, _ ->  // bounce each chosen target
        if (jsonContains(node, "_Permanents", "Ref_TargetPermanents")) {
            "ForEachTargetEffect(listOf(Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND)))"
        } else null
    }

    on("DestroyPermanent") { _, args, tvar ->
        val tgt = refTarget(args, tvar) ?: return@on null
        "Effects.Move($tgt, Zone.GRAVEYARD, byDestruction = true)"
    }
    on("DestroyEachPermanent", "DestroyEachPermanentNoRegen") { node, args, _ ->
        if (jsonContains(args, "_Permanents", "Ref_TargetPermanents")) {
            val noregen = if (node.strField("_Action") == "DestroyEachPermanentNoRegen") ", noRegenerate = true" else ""
            return@on "ForEachTargetEffect(listOf(Effects.Move(EffectTarget.ContextTarget(0), " +
                "Zone.GRAVEYARD, byDestruction = true$noregen)))"
        }
        if (oracleText?.contains("target", ignoreCase = true) == true) return@on null
        val noregen = if (node.strField("_Action") == "DestroyEachPermanentNoRegen") "true" else "false"
        val filter = groupFilterDsl(args) ?: return@on null
        "Effects.ForEachInGroup($filter, Effects.Move(EffectTarget.Self, " +
            "Zone.GRAVEYARD, byDestruction = true), noRegenerate = $noregen)"
    }

    on("PutPermanentIntoItsOwnersHand") { _, args, tvar ->  // bounce
        val tgt = refTarget(args, tvar) ?: return@on null
        "Effects.Move($tgt, Zone.HAND)"
    }

    on("ShuffleGraveyardCardIntoLibrary") { _, args, tvar ->  // e.g. Alabaster Dragon
        val tgt = refTarget(args, tvar) ?: "EffectTarget.Self"
        "Effects.Move($tgt, Zone.LIBRARY, ZonePlacement.Shuffled)"
    }

    on("SearchLibrary") { _, args, _ -> renderSearch(args) }
    on("LookAtTheTopNumberCardsOfLibrary", "LookAtTheTopNumberCardsOfPlayersLibrary") { node, args, tvar -> renderLook(node, args, tvar) }

    on("PutGraveyardCardOntoBattlefield", "PutGraveyardCardIntoHand",
        "ReturnDeadGraveyardCardToTopOfLibrary", "PutPermanentOnTopOfOwnersLibrary") { node, args, tvar ->
        val a = node.strField("_Action")
        // ReturnDead… ("return this card from the graveyard") often has no ref -> Self
        var tgt = refTarget(args, tvar)
        if (tgt == null) {
            if (a == "ReturnDeadGraveyardCardToTopOfLibrary") tgt = "EffectTarget.Self" else return@on null
        }
        val zone = mapOf(
            "PutGraveyardCardOntoBattlefield" to "BATTLEFIELD", "PutGraveyardCardIntoHand" to "HAND",
            "ReturnDeadGraveyardCardToTopOfLibrary" to "LIBRARY", "PutPermanentOnTopOfOwnersLibrary" to "LIBRARY",
        )[a]
        if (a == "PutPermanentOnTopOfOwnersLibrary" || a == "ReturnDeadGraveyardCardToTopOfLibrary") {
            "Effects.Move($tgt, Zone.$zone, ZonePlacement.Top)"
        } else {
            "Effects.Move($tgt, Zone.$zone)"
        }
    }
}

internal fun EmitCtx.renderSearch(args: JsonElement?): String? {
    val blob = compact(args)
    val dest = when {
        "PutFoundCardsOntoBattlefield" in blob -> "BATTLEFIELD"
        "PutFoundCardsIntoHand" in blob -> "HAND"
        "PutSetAsideCardsOnTopOfLibrary" in blob || "OnTopOfLibrary" in blob -> "TOP_OF_LIBRARY"
        else -> "HAND"
    }
    // "search for a card named X" (Avarax, Daru Cavalier, …) -> a name filter, which the land/type
    // search filter can't express.
    val named = Regex(""""NamedCard",\s*"args":\s*"([^"]+)"""").find(blob)?.groupValues?.get(1)
    val searchSubtype = Regex(""""IsCreatureType",\s*"args":\s*"(\w+)"""").find(blob)?.groupValues?.get(1)
    val filt = when {
        named != null -> "GameObjectFilter.Any.named(\"$named\")"
        searchSubtype != null -> "GameObjectFilter.Any.withSubtype(\"$searchSubtype\")"  // "an Elf card"
        else -> landSearchFilterDsl(args)
    }
    val count = findInteger(args)
    val parts = mutableListOf("filter = $filt")
    if (count is Int && count != 1) parts.add("count = $count")
    parts.add("destination = SearchDestination.$dest")
    if ("EntersTapped" in blob) parts.add("entersTapped = true")  // "...onto the battlefield tapped"
    if ("RevealFoundCards" in blob) parts.add("reveal = true")
    return "Patterns.Library.searchLibrary(${parts.joinToString(", ")})"
}

internal fun EmitCtx.renderLook(node: JsonObject, args: JsonElement?, tvar: String?): String? {
    val look = findInteger(node) ?: return null
    val blob = compact(node)
    if (oracleText?.contains("target", ignoreCase = true) == true) {
        if (node.strField("_Action") != "LookAtTheTopNumberCardsOfPlayersLibrary" || tvar == null) return null
        if ("PutAGenericCardIntoGraveyard" !in blob || "PutTheRemainingCardsOnTopOfLibraryInAnyOrder" !in blob) return null
        return composite(listOf(
            "GatherCardsEffect(CardSource.TopOfLibrary(DynamicAmount.Fixed($look), Player.TargetOpponent), storeAs = \"looked\")",
            "SelectFromCollectionEffect(\n" +
                "                from = \"looked\",\n" +
                "                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),\n" +
                "                storeSelected = \"toGraveyard\",\n" +
                "                storeRemainder = \"toTop\",\n" +
                "                selectedLabel = \"Put in graveyard\",\n" +
                "                remainderLabel = \"Put on top\"\n" +
                "            )",
            "MoveCollectionEffect(from = \"toGraveyard\", destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.TargetOpponent))",
            "MoveCollectionEffect(\n" +
                "                from = \"toTop\",\n" +
                "                destination = CardDestination.ToZone(Zone.LIBRARY, Player.TargetOpponent, ZonePlacement.Top),\n" +
                "                order = CardOrder.ControllerChooses\n" +
                "            )",
        ))
    }
    var keep: Int? = null
    for (m in Regex(""""PutNumber\w*IntoHand".*?"args":\s*(\d+)""").findAll(blob)) keep = m.groupValues[1].toInt()
    if (keep != null) return "Patterns.Library.lookAtTopAndKeep(count = $look, keepCount = $keep)"
    if ("PutTheRemainingCardsOnTopOfLibraryInAnyOrder" in blob) return "Patterns.Library.lookAtTopAndReorder(count = $look)"
    return null
}
