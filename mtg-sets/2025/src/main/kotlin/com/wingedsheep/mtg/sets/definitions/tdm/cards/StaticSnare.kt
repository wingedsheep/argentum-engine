package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Static Snare
 * {4}{W}
 * Enchantment
 *
 * Flash
 * This spell costs {1} less to cast for each attacking creature.
 * When this enchantment enters, exile target artifact or creature an opponent controls
 * until this enchantment leaves the battlefield.
 *
 * Same exile-until-leaves family as Banishing Light / Suspension Field: an ETB trigger
 * exiles the target with a source link, and a LTB trigger returns it. The cost reduction
 * reuses [CostReductionSource.PermanentsOnBattlefieldMatching] over `Creature.attacking()`
 * to count every attacking creature on the battlefield (both players').
 */
val StaticSnare = card("Static Snare") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Flash\nThis spell costs {1} less to cast for each attacking creature.\nWhen this enchantment enters, exile target artifact or creature an opponent controls until this enchantment leaves the battlefield."

    keywords(Keyword.FLASH)

    // {1} less for each attacking creature (counts all attackers on the battlefield).
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.PermanentsOnBattlefieldMatching(
                    filter = GameObjectFilter.Creature.attacking(),
                ),
            ),
        )
    }

    // ETB: exile target artifact or creature an opponent controls until this leaves.
    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter((GameObjectFilter.Artifact or GameObjectFilter.Creature).opponentControls()))
        effect = Effects.ExileUntilLeaves(permanent)
    }

    // LTB: return the exiled card under its owner's control.
    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Yohann Schepacz"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1ce50932-03a6-48bc-8aee-bc8defd896cf.jpg?1743204059"
    }
}
