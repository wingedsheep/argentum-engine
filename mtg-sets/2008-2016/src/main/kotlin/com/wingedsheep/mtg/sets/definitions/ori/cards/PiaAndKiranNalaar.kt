package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Pia and Kiran Nalaar
 * {2}{R}{R}
 * Legendary Creature — Human Artificer
 * 2/2
 * When Pia and Kiran Nalaar enters, create two 1/1 colorless Thopter artifact creature tokens with flying.
 * {2}{R}, Sacrifice an artifact: Pia and Kiran Nalaar deals 2 damage to any target.
 */
val PiaAndKiranNalaar = card("Pia and Kiran Nalaar") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Artificer"
    power = 2
    toughness = 2
    oracleText = "When Pia and Kiran Nalaar enters, create two 1/1 colorless Thopter artifact creature " +
        "tokens with flying.\n" +
        "{2}{R}, Sacrifice an artifact: Pia and Kiran Nalaar deals 2 damage to any target."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            count = 2,
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.Sacrifice(GameObjectFilter.Artifact))
        val t = target(Targets.Any)
        effect = Effects.DealDamage(2, t)
        description = "{2}{R}, Sacrifice an artifact: Pia and Kiran Nalaar deals 2 damage to any target."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "157"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0dcafa9e-3637-42ca-bdb2-45a2b5da7294.jpg?1783938327"
    }
}
