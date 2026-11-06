package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pia Nalaar
 * {2}{R}
 * Legendary Creature — Human Artificer
 * 2/2
 * When Pia Nalaar enters, create a 1/1 colorless Thopter artifact creature token with flying.
 * {1}{R}: Target artifact creature gets +1/+0 until end of turn.
 * {1}, Sacrifice an artifact: Target creature can't block this turn.
 */
val PiaNalaar = card("Pia Nalaar") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Artificer"
    oracleText = "When Pia Nalaar enters, create a 1/1 colorless Thopter artifact creature token with flying.\n" +
        "{1}{R}: Target artifact creature gets +1/+0 until end of turn.\n" +
        "{1}, Sacrifice an artifact: Target creature can't block this turn."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
        )
    }

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        val creature = target(TargetFilter(GameObjectFilter.ArtifactCreature))
        effect = Effects.ModifyStats(1, 0, creature)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Sacrifice(GameObjectFilter.Artifact))
        val creature = target(TargetFilter.Creature)
        effect = Effects.CantBlock(creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a2b2954-0734-4219-b6df-c6cc3dcd8d5a.jpg"
    }
}
