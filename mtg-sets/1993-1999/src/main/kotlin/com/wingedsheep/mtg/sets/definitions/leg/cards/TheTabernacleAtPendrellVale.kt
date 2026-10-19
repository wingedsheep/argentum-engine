package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Tabernacle at Pendrell Vale
 * Legendary Land
 *
 * All creatures have "At the beginning of your upkeep, destroy this creature unless you pay {1}."
 *
 * Vile Consumption's shape: a [GrantTriggeredAbility] over [GroupFilter.AllCreatures], so each
 * creature carries its own upkeep trigger, controlled by (and firing on the upkeep of) that
 * creature's controller. [Effects.PayOrSuffer] asks that controller for {1}; unpaid, the creature
 * itself is destroyed (regeneration still applies — it's a destroy, not a sacrifice).
 */
val TheTabernacleAtPendrellVale = card("The Tabernacle at Pendrell Vale") {
    typeLine = "Legendary Land"
    oracleText = "All creatures have \"At the beginning of your upkeep, destroy this creature unless you pay {1}.\""

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.you.beginningOf(Step.UPKEEP),
                effect = Effects.PayOrSuffer(
                    cost = Costs.pay.Mana(ManaCost.parse("{1}")),
                    suffer = Effects.Destroy(EffectTarget.Self),
                    consequenceDescription = "destroy this creature",
                ),
                descriptionOverride = "At the beginning of your upkeep, destroy this creature unless you pay {1}.",
            ),
            filter = GroupFilter.AllCreatures,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "307"
        artist = "Nicola Leonard"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64bc9b1d-5818-4d9e-b771-e49af4ff9a5c.jpg?1783948022"
    }
}
