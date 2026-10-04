package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vat of Rebirth
 * {B}
 * Artifact
 *
 * Whenever another artifact or creature you control is put into a graveyard from the battlefield,
 * put an oil counter on this artifact.
 * {2}{B}, {T}, Remove four oil counters from this artifact: Return target creature card from your
 * graveyard to the battlefield. Activate only as a sorcery.
 */
val VatOfRebirth = card("Vat of Rebirth") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Artifact"
    oracleText = "Whenever another artifact or creature you control is put into a graveyard from the " +
        "battlefield, put an oil counter on this artifact.\n" +
        "{2}{B}, {T}, Remove four oil counters from this artifact: Return target creature card from " +
        "your graveyard to the battlefield. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.another((GameObjectFilter.Artifact or GameObjectFilter.Creature).youControl()).dies()
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B}"),
            Costs.Tap,
            Costs.RemoveCounterFromSelf(CounterType.OIL, 4),
        )
        val creature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Move(creature, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "Peter Polach"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20a961ea-0639-4f5d-8cf4-f909c59a4ae1.jpg?1783918038"
    }
}
