package com.wingedsheep.ai.assist

import com.wingedsheep.ai.draftsim.DraftsimBuild
import com.wingedsheep.ai.draftsim.DraftsimData
import com.wingedsheep.ai.draftsim.DraftsimDeckBuilder
import com.wingedsheep.ai.draftsim.DraftsimPoolCard
import com.wingedsheep.ai.draftsim.toScorerCard

/**
 * Deckbuild engine backed by the ported Draftsim autobuilder ([DraftsimDeckBuilder]): it ranks
 * archetypes (`kf`), greedily builds + refines each (`vX`/`ek`), and returns the highest-scoring
 * 23-nonland + 17-land deck.
 *
 * When [DeckBuildRequest.locked] is empty this is a **full rebuild from the pool** (the "Auto-build"
 * button) — it ranks archetypes and returns the best few fresh 40-card limited builds (top 3) so the
 * client can offer them as alternatives. When locked is non-empty (the "Complete Deck" button) it
 * switches the builder to completion mode: the locked cards are forced into the build, protected from
 * removal/swap, and the rest is filled around them in the locked cards' colors — a single completed
 * deck. (Draftsim still fixes its own 23/17 land split, so [DeckBuildRequest.targetSize] is not
 * honored.) Basics are reported by name so the client splits them out like any other result.
 */
object DraftsimDeckBuildAdvisor : DeckBuildAdvisor {
    override val id = "draftsim"
    override val displayName = "Draftsim"

    /** How many alternative builds to surface from a fresh Auto-build (completion always yields one). */
    private const val MAX_OPTIONS = 3

    private val COLOR_TO_BASIC = mapOf("W" to "Plains", "U" to "Island", "B" to "Swamp", "R" to "Mountain", "G" to "Forest")

    override fun buildDeck(request: DeckBuildRequest): DeckBuildResult {
        val builder = DraftsimDeckBuilder(DraftsimData.tablesFor(request.setCodes))
        val pool = request.pool.mapIndexed { i, def -> DraftsimPoolCard(def.toScorerCard(), "pool-$i") }
        val byId = pool.associateBy { it.instanceId }

        // buildDecks returns the candidates best-first (completion mode: exactly one); take the top few.
        val builds = builder.buildDecks(pool, mode = "sealed", forced = lockedInstanceIds(pool, request.locked))
            .take(MAX_OPTIONS)
        if (builds.isEmpty()) return DeckBuildResult(advisorId = id, builds = emptyList())

        return DeckBuildResult(advisorId = id, builds = builds.map { toOption(it, byId) }, recommended = 0)
    }

    /** Materialize one [DraftsimBuild]'s pool instances + basics into a name → count [DeckBuildOption]. */
    private fun toOption(build: DraftsimBuild, byId: Map<String, DraftsimPoolCard>): DeckBuildOption {
        val deckList = LinkedHashMap<String, Int>()
        for (instanceId in build.deckInstanceIds) {
            val name = byId[instanceId]?.card?.name ?: continue
            deckList[name] = (deckList[name] ?: 0) + 1
        }
        for ((color, count) in build.basicsNeeded) {
            val name = COLOR_TO_BASIC[color] ?: continue
            if (count > 0) deckList[name] = (deckList[name] ?: 0) + count
        }
        return DeckBuildOption(deckList = deckList, score = build.score, archetype = build.name, colors = build.colors)
    }

    /**
     * Map the in-progress deck ([DeckBuildRequest.locked], name → count) to concrete nonland pool
     * instances to force into the build. Lands are left out — basics aren't in the pool and the
     * manabase is rebuilt around the kept spells. Empty locked → empty set → a fresh build.
     */
    private fun lockedInstanceIds(pool: List<DraftsimPoolCard>, locked: Map<String, Int>): Set<String> {
        val remaining = locked.filterValues { it > 0 }.toMutableMap()
        if (remaining.isEmpty()) return emptySet()
        val forced = HashSet<String>()
        for (pc in pool) {
            if (pc.card.typeLine.contains("Land", ignoreCase = true)) continue
            val want = remaining[pc.card.name] ?: continue
            if (want <= 0) continue
            forced += pc.instanceId
            remaining[pc.card.name] = want - 1
        }
        return forced
    }
}
