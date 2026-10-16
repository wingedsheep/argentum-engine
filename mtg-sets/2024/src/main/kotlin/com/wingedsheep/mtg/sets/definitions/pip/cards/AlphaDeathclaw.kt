package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Alpha Deathclaw — "enters or becomes monstrous" is the two triggers, each destroying a target
 * permanent. The monstrous trigger only fires when the monstrosity ability actually makes it
 * monstrous (CR 701.37a, 701.37b), so a second activation does nothing.
 */
val AlphaDeathclaw = card("Alpha Deathclaw") {
    manaCost = "{4}{B}{G}"
    typeLine = "Creature — Lizard Mutant"
    power = 6
    toughness = 6
    oracleText = "Menace, trample\n" +
        "When this creature enters or becomes monstrous, destroy target permanent.\n" +
        "{5}{B}{G}: Monstrosity 4. (If this creature isn't monstrous, put four +1/+1 counters on it and it becomes monstrous.)"

    keywords(Keyword.MENACE, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.Destroy(permanent)
    }

    triggeredAbility {
        trigger = Triggers.self.becomesMonstrous()
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.Destroy(permanent)
    }

    activatedAbility {
        cost = Costs.Mana("{5}{B}{G}")
        effect = Effects.Monstrosity(4)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca4c8b04-66af-4cb3-8003-9088d8344b20.jpg?1783912392"
        ruling("2024-03-08", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2024-03-08", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
        ruling("2024-03-08", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
