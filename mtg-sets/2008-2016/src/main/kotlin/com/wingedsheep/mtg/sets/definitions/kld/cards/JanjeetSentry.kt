package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Janjeet Sentry — Kaladesh #53
 * {2}{U} · Creature — Vedalken Soldier · 2/3
 *
 * When this creature enters, you get {E}{E} (two energy counters).
 * {T}, Pay {E}{E}: You may tap or untap target artifact or creature.
 *
 * "You may tap or untap" is the Captain of the Mists shape: an [Effects.May] over a two-mode
 * resolution-time modal (`countsAsModalSpell = false`) on the single declared target.
 */
val JanjeetSentry = card("Janjeet Sentry") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Vedalken Soldier"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, you get {E}{E} (two energy counters).\n" +
        "{T}, Pay {E}{E}: You may tap or untap target artifact or creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 2))
        val t = target(TargetFilter.CreatureOrArtifact)
        effect = Effects.May(
            Effects.Modal(
                modes = listOf(
                    Mode.noTarget(Effects.Tap(t), "Tap that permanent"),
                    Mode.noTarget(Effects.Untap(t), "Untap that permanent"),
                ),
                chooseCount = 1,
                countsAsModalSpell = false,
            ),
            descriptionOverride = "You may tap or untap that permanent",
        )
        description = "{T}, Pay {E}{E}: You may tap or untap target artifact or creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "Dan Murayama Scott"
        flavorText = "Janjeet Observatory is open to the public, but access to the adjoining laboratories is restricted."
        imageUri = "https://cards.scryfall.io/normal/front/8/2/826b9080-2b6d-4f4e-93a6-cf977d40654f.jpg?1783937218"
    }
}
