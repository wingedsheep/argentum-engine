package com.wingedsheep.engine.registry

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.AbilityId

/**
 * The definition a **merged permanent** reads its abilities from (CR 730, mutate CR 702.140e): the
 * topmost component's definition carrying every component's abilities, topmost first.
 *
 * A merged permanent is one object represented by several cards, but every ability lookup in the
 * engine goes through one `cardDefinitionId`. Rather than teach each lookup about components, the
 * permanent's [com.wingedsheep.engine.state.components.identity.CardComponent] names a *composite*
 * id ([compose]) that [CardRegistry.getCard] resolves here. The id is a pure function of the
 * component ids, so the built definition is cached and shared across games.
 *
 * A component whose id doesn't resolve (an ordinary token) contributes no definition-borne
 * abilities: a token's abilities live on its entity, which is the merged permanent itself when the
 * token was the creature mutated onto. When the topmost component doesn't resolve, the composite is
 * an ability-only skeleton — the permanent's own `CardComponent` carries the topmost characteristics
 * either way.
 */
object MergedCardDefinitions {
    private const val PREFIX = "merged:"
    private const val SEPARATOR = " ⊕ "

    /** The composite id for [componentIds], topmost first. */
    fun compose(componentIds: List<String>): String = PREFIX + componentIds.joinToString(SEPARATOR)

    fun isComposite(id: String): Boolean = id.startsWith(PREFIX)

    /** The component ids of a composite id, topmost first. */
    fun components(id: String): List<String> = id.removePrefix(PREFIX).split(SEPARATOR)

    internal fun build(id: String, lookup: (String) -> CardDefinition?): CardDefinition {
        val parts = components(id).map(lookup)
        // A token on top has no definition: an ability-only skeleton, whose characteristics nothing
        // reads — the permanent's CardComponent carries the token's.
        val top = parts.first() ?: CardDefinition(name = id, manaCost = ManaCost.ZERO, typeLine = TypeLine(cardTypes = emptySet()))
        val defs = parts.filterNotNull()
        // Two copies of one card (a creature mutated onto its namesake) each contribute their
        // abilities — two triggers, not one — so a repeat's ability ids are made distinct.
        val seen = mutableMapOf<String, Int>()
        val scripts = defs.map { def ->
            val repeat = seen.merge(def.name, 1, Int::plus)!! - 1
            if (repeat == 0) def.script else def.script.copy(
                triggeredAbilities = def.script.triggeredAbilities.map { it.copy(id = it.id.repeated(repeat)) },
                stateTriggeredAbilities = def.script.stateTriggeredAbilities.map { it.copy(id = it.id.repeated(repeat)) },
                activatedAbilities = def.script.activatedAbilities.map { it.copy(id = it.id.repeated(repeat)) },
            )
        }
        return top.copy(
            name = id,
            keywords = defs.flatMapTo(linkedSetOf()) { it.keywords },
            flags = defs.flatMapTo(linkedSetOf()) { it.flags },
            keywordAbilities = defs.flatMap { it.keywordAbilities }.distinct(),
            script = top.script.copy(
                triggeredAbilities = scripts.flatMap { it.triggeredAbilities },
                stateTriggeredAbilities = scripts.flatMap { it.stateTriggeredAbilities },
                activatedAbilities = scripts.flatMap { it.activatedAbilities },
                staticAbilities = scripts.flatMap { it.staticAbilities },
                replacementEffects = scripts.flatMap { it.replacementEffects },
            ),
        )
    }

    private fun AbilityId.repeated(n: Int) = AbilityId("$value@${n + 1}")
}
