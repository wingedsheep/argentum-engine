package com.wingedsheep.ai.jev

import com.wingedsheep.engine.core.*
import com.wingedsheep.sdk.model.EntityId

/** Typed assembly only: Jev makes the choices, the engine validates the completed response. */
internal class JevDecisions(private val q: JevChoices, private val label: (EntityId) -> String, private val playCard: ((PlayCardDecision) -> GameAction)? = null) {
    private fun cards(prompt: String, ids: List<EntityId>, min: Int, max: Int) =
        q.select(prompt, ids, min, max, label)

    fun answer(d: PendingDecision): DecisionResponse = when (d) {
        is PlayCardDecision -> PlayCardResponse(d.id, requireNotNull(playCard)(d))
        is YesNoDecision -> YesNoResponse(d.id, q.pick(d.prompt, listOf(true, false)) { if (it) d.yesText else d.noText })
        is BatchYesNoDecision -> BatchYesNoResponse(d.id,
            q.pick(d.prompt, listOf(true, false)) { if (it) d.yesText else d.noText }, applyToAll = false)
        is ChooseOptionDecision -> OptionChosenResponse(d.id, q.pick(d.prompt, d.options.indices.toList()) { d.options[it] })
        is ChooseNumberDecision -> NumberChosenResponse(d.id, q.number(d.prompt, d.minValue, d.maxValue))
        is ChooseColorDecision -> ColorChosenResponse(d.id, q.pick(d.prompt, d.availableColors.toList()))
        is ChooseModeDecision -> ModesChosenResponse(d.id,
            q.select(d.prompt, d.modes.filter { it.available }, d.minModes, d.maxModes) { it.text }.map { it.index })
        is SelectCardsDecision -> CardsSelectedResponse(d.id, cards(d.prompt, d.options, d.minSelections, d.maxSelections))
        is SearchLibraryDecision -> CardsSelectedResponse(d.id, cards(d.prompt, d.options, d.minSelections, d.maxSelections))
        is ChooseTargetsDecision -> TargetsResponse(d.id, d.targetRequirements.associate { r ->
            r.index to cards("${d.prompt}: ${r.description}", d.legalTargets[r.index].orEmpty(), r.minTargets, r.maxTargets)
        })
        is OrderObjectsDecision -> OrderedResponse(d.id, cards("${d.prompt}: choose next in order", d.objects, d.objects.size, d.objects.size))
        is ReorderLibraryDecision -> OrderedResponse(d.id, cards("${d.prompt}: top first", d.cards, d.cards.size, d.cards.size))
        is SplitPilesDecision -> if (d.suggestedPiles != null) PilesSplitResponse(d.id, d.suggestedPiles!!) else {
            val piles = List(d.numberOfPiles) { mutableListOf<EntityId>() }
            d.cards.forEach { card ->
                val pile = q.pick("${d.prompt}: place ${label(card)}", piles.indices.toList()) {
                    "Pile $it ${d.pileLabels.getOrNull(it).orEmpty()}: ${piles[it].map(label)}"
                }
                piles[pile] += card
            }
            PilesSplitResponse(d.id, piles)
        }
        is ChooseReplacementDecision -> {
            val from = q.pick("${d.prompt}: replace which word?", d.fromOptions.indices.toList()) { d.fromOptions[it] }
            val to = q.pick("Replace ${d.fromOptions[from]} with", d.allowedToByFrom.getOrNull(from) ?: d.toOptions.indices.toList()) {
                d.toOptions[it]
            }
            ReplacementChosenResponse(d.id, from, to)
        }
        is BudgetModalDecision -> {
            var budget = d.budget
            val selected = mutableListOf<Int>()
            while (budget > 0) {
                val options = d.modes.indices.filter { d.modes[it].cost in 1..budget }
                val next = q.pick("${d.prompt}: $budget budget remains", options + listOf(-1)) {
                    if (it < 0) "Finish" else "${d.modes[it].description} (cost ${d.modes[it].cost})"
                }
                if (next < 0) break
                selected += next
                budget -= d.modes[next].cost
            }
            BudgetModalResponse(d.id, selected)
        }
        is DistributeDecision -> DistributionResponse(d.id,
            distribute(d.prompt, d.targets, d.totalAmount, d.minPerTarget, d.maxPerTarget, d.allowPartial))
        is AssignDamageDecision -> {
            val targets = d.orderedTargets + listOfNotNull(d.defenderId.takeIf { d.hasTrample })
            DamageAssignmentResponse(d.id, distribute(d.prompt, targets, d.availablePower))
        }
        is CombatResolutionDecision -> {
            val edges = d.edges.filter { it.editableBy == d.playerId }.groupBy { it.sourceId }.flatMap { (source, edges) ->
                var remaining = edges.maxOf { it.maximum } - d.edges.filter { it.sourceId == source && it.editableBy != d.playerId }.sumOf { it.amount }
                edges.mapIndexed { index, edge ->
                    val amount = q.number("${d.prompt}: damage from ${label(source)} to ${label(edge.targetId)}; " +
                        "$remaining remains, lethal=${edge.lethal}, trample=${edge.isTrampleDrain}", if (index == edges.lastIndex) remaining else 0, minOf(remaining, edge.maximum))
                    remaining -= amount
                    DamageEdgeAmount(edge.id, amount)
                }
            }
            CombatResolutionResponse(d.id, edges)
        }
        is SelectManaSourcesDecision -> {
            if (d.canDecline && q.pick("${d.prompt}: pay ${d.requiredCost}?", listOf(true, false)) == false) {
                ManaSourcesSelectedResponse(d.id, declined = true)
            } else {
                val waterbend = cards("Tap to help pay ${d.requiredCost}", d.waterbendPermanents.map { it.entityId }, 0, d.waterbendPermanents.size)
                val sources = cards("Choose mana sources to pay ${d.requiredCost}", d.availableSources.map { it.entityId }, 0, d.availableSources.size)
                ManaSourcesSelectedResponse(d.id, selectedSources = sources, waterbendPermanents = waterbend.toSet())
            }
        }
    }

    fun distribute(prompt: String, targets: List<EntityId>, total: Int, min: Int = 0,
                   caps: Map<EntityId, Int> = emptyMap(), partial: Boolean = false): Map<EntityId, Int> {
        var remaining = total
        return targets.mapIndexed { index, id ->
            val later = targets.drop(index + 1)
            val lower = if (partial) min else maxOf(min, remaining - later.sumOf { caps[it] ?: total })
            val upper = minOf(caps[id] ?: total, remaining - later.size * min)
            val amount = q.number("$prompt: amount for ${label(id)} ($remaining remains)", lower, upper)
            remaining -= amount
            id to amount
        }.toMap()
    }
}
