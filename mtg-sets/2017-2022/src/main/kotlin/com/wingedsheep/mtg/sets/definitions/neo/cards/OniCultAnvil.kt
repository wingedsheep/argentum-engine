package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oni-Cult Anvil
 * {B}{R}
 * Artifact
 * Whenever one or more artifacts you control leave the battlefield during your turn, create a 1/1
 * colorless Construct artifact creature token. This ability triggers only once each turn.
 * {T}, Sacrifice an artifact: This artifact deals 1 damage to each opponent. You gain 1 life.
 *
 * "One or more … triggers only once each turn" is a per-artifact leaves trigger under the
 * `oncePerTurn` cap: the cap collapses simultaneous departures to one trigger (the detector dedupes
 * a single pass per source and ability) and stops any later departure that turn, which is exactly
 * the batch reading. "During your turn" is a `triggerRestriction`, checked only as the event
 * happens. The Anvil leaving itself counts — `Triggers.a` (not `another`) looks back at its own
 * departure, so sacrificing the Anvil to its own ability still makes a Construct.
 */
val OniCultAnvil = card("Oni-Cult Anvil") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Artifact"
    oracleText = "Whenever one or more artifacts you control leave the battlefield during your turn, " +
        "create a 1/1 colorless Construct artifact creature token. This ability triggers only once each turn.\n" +
        "{T}, Sacrifice an artifact: This artifact deals 1 damage to each opponent. You gain 1 life."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.youControl()).leaves()
        triggerRestriction = Conditions.IsYourTurn
        oncePerTurn = true
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Construct"),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/2/7/2770f887-7e96-44d1-864a-8c0504a39785.jpg?1783923710"
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Sacrifice(GameObjectFilter.Artifact))
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "230"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c84db1e2-855c-4169-a9cc-a54c73c14e0c.jpg?1783923832"
    }
}
