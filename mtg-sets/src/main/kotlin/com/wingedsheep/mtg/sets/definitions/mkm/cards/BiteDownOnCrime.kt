package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.collectEvidence
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty


/**
 * Bite Down on Crime
 * {3}{G}
 * Sorcery
 *
 * As an additional cost to cast this spell, you may collect evidence 6. This spell costs {2} less to
 * cast if evidence was collected.
 * Target creature you control gets +2/+0 until end of turn. It deals damage equal to its power to
 * target creature you don't control.
 *
 * The **cost-gate** shape of the linkage — the awkward-looking one, because the reduction has to be
 * known while the cost is being calculated but the collection is a choice made *during* casting.
 * It needs no special machinery: this is the same `SelfCast` [ModifySpellCost] +
 * `CostGating.OnlyIf` shape Hamlet Glutton uses for bargain, and it works for the same reason —
 * the enumerator prices each *cast branch* separately, offering the plain cast at {3}{G} and the
 * collect-evidence cast at {1}{G} plus the exile. The condition is evaluated against the branch
 * being priced, so there is no ordering problem to solve.
 *
 * As with Hamlet Glutton, the reduction changes only the total cost paid: this card's mana cost and
 * mana value stay {3}{G} / 4 however it was cast.
 *
 * The damage is `DynamicAmount.EntityProperty` power of the *first* target read at resolution, which
 * is what makes the +2/+0 count — the pump resolves first, and the damage reads the boosted power.
 */
val BiteDownOnCrime = card("Bite Down on Crime") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, you may collect evidence 6. This spell " +
        "costs {2} less to cast if evidence was collected. (To collect evidence 6, exile cards " +
        "with total mana value 6 or greater from your graveyard.)\n" +
        "Target creature you control gets +2/+0 until end of turn. It deals damage equal to its " +
        "power to target creature you don't control."

    collectEvidence(6)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(Conditions.WasEvidenceCollected),
        )
    }

    spell {
        val yours = target(
            "target creature you control",
            TargetCreature(filter = TargetFilter.Creature.youControl()),
        )
        val theirs = target(
            "target creature you don't control",
            TargetCreature(filter = TargetFilter.Creature.opponentControls()),
        )
        effect = Effects.Composite(
            Effects.ModifyStats(power = 2, toughness = 0, target = yours),
            Effects.DealDamage(
                amount = DynamicAmount.EntityProperty(
                    EntityReference.Target(0),
                    EntityNumericProperty.Power,
                ),
                target = theirs,
                damageSource = yours,
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "154"
        artist = "Mike Bierek"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29bbfe93-8225-444c-835b-33ffa006ef66.jpg"
    }
}
