package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Suspicious Detonation {4}{R}
 * Sorcery
 *
 * This spell costs {3} less to cast if you've sacrificed an artifact this turn.
 * This spell can't be countered.
 * Suspicious Detonation deals 4 damage to target creature.
 *
 * The reduction rides the generic gated-reduction rail ([ModifySpellCost] +
 * [CostGating.OnlyIf]) rather than a bespoke `CostReductionSource`, the same way Bite Down on
 * Crime gates its {2} on evidence. [Conditions.SacrificedArtifactThisTurn] is controller-scoped
 * turn history, so an opponent cracking their own Clue never discounts this, and the artifact
 * having since left the graveyard doesn't undo the discount.
 *
 * `cantBeCountered` covers the parenthetical "(This includes by the ward ability.)": ward's
 * counter-unless-you-pay is a countering effect, so it simply fails to counter this spell — the
 * ward cost is still *offered*, and declining it no longer counters the spell.
 */
val SuspiciousDetonation = card("Suspicious Detonation") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "This spell costs {3} less to cast if you've sacrificed an artifact this turn.\n" +
        "This spell can't be countered. (This includes by the ward ability.)\n" +
        "Suspicious Detonation deals 4 damage to target creature."

    cantBeCountered = true

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(3),
            gating = CostGating.OnlyIf(Conditions.SacrificedArtifactThisTurn),
        )
    }

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.DealDamage(4, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "145"
        artist = "Joe Slucher"
        flavorText = "\"Clearly someone wanted us to find this.\"\n—Runubi of the Foundway Associates"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e280482-ed7e-4011-899e-096ff7bd4c41.jpg?1783912874"
        ruling(
            "2024-02-02",
            "If you target a creature or planeswalker with ward, you may still pay the ward cost, " +
                "but Suspicious Detonation won't be countered even if you don't."
        )
    }
}
