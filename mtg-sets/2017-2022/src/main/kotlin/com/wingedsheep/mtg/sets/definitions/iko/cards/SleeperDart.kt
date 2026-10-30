package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sleeper Dart
 * {2}
 * Artifact
 * When this artifact enters, draw a card.
 * {T}, Sacrifice this artifact: Target creature doesn't untap during its controller's next untap step.
 *
 * The untap lock is Crippling Chill's: [AbilityFlag.DOESNT_UNTAP] for
 * [Duration.UntilAfterAffectedControllersNextUntap]. It doesn't tap the creature.
 */
val SleeperDart = card("Sleeper Dart") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "When this artifact enters, draw a card.\n" +
        "{T}, Sacrifice this artifact: Target creature doesn't untap during its controller's next untap step."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(
            AbilityFlag.DOESNT_UNTAP,
            creature,
            Duration.UntilAfterAffectedControllersNextUntap()
        )
        description = "{T}, Sacrifice this artifact: Target creature doesn't untap during its controller's next untap step."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "240"
        artist = "Sidharth Chaturvedi"
        flavorText = "\"Sleep now, you big brute.\"\n—Kelsien, the Plague"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/311f827a-1585-483e-b0fd-3dc05969b485.jpg"
    }
}
