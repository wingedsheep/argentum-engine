package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.conditions.Condition
import kotlinx.serialization.Serializable

/**
 * Represents an alternative cost that a spell can define for itself.
 * Unlike [GrantAlternativeCastingCost] (which is a static ability on a permanent
 * that grants alternative costs to other spells), this is declared on the spell's
 * own CardScript.
 *
 * The player may choose to pay the alternative mana cost (plus any additional costs)
 * instead of the spell's normal mana cost.
 *
 * Example: Zahid, Djinn of the Lamp — "You may pay {3}{U} and tap an untapped
 * artifact you control rather than pay this spell's mana cost."
 *
 * @property manaCost The alternative mana cost (e.g., `ManaCost.parse("{3}{U}")`).
 *           Typed like every other cost; serializes as its string form.
 * @property additionalCosts Non-mana costs that must be paid alongside the alternative
 *           mana cost (e.g., tapping an artifact)
 * @property condition Game-state gate on the alternative being *available at all* — the
 *           "…if <condition>" clause on cards like Blasphemous Edict ("You may pay {B} rather
 *           than pay this spell's mana cost if there are thirteen or more creatures on the
 *           battlefield"). Evaluated with no target/trigger context, at the two mirrored sites
 *           that decide legality: action enumeration and cast authorization. `null` (the default)
 *           means unconditionally available, which is Zahid's shape.
 * @property manaSources Which sources' mana may pay this alternative — "Spend only mana produced by
 *           Treasures to cast it this way" (Security Rhox: `GameObjectFilter.Artifact.withSubtype("Treasure")`).
 *           Only mana abilities of permanents matching the filter may be activated for the payment,
 *           and floating mana counts only when the source that produced it matched — judged against
 *           the type line (card types, subtypes, supertypes) that source had when it made the mana,
 *           so a Treasure sacrificed for its own mana still made Treasure mana. Mana with no recorded
 *           source never pays a restricted cast. Keep the filter to type-line predicates: those are
 *           what a unit of mana remembers about its source. `null` (the default) means mana from
 *           any source, which is every other alternative cost's shape.
 */
@Serializable
data class SelfAlternativeCost(
    val manaCost: ManaCost,
    val additionalCosts: List<AdditionalCost> = emptyList(),
    val condition: Condition? = null,
    val manaSources: GameObjectFilter? = null
)
