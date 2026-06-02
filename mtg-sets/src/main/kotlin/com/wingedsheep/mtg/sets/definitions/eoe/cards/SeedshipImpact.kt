package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Seedship Impact
 * {1}{G}
 * Instant
 * Destroy target artifact or enchantment. If its mana value was 2 or less, create a Lander token. (It's an artifact with "{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.")
 */
val SeedshipImpact = card("Seedship Impact") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Destroy target artifact or enchantment. If its mana value was 2 or less, create a Lander token. (It's an artifact with \"{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.\")"

    spell {
        val target = target("target artifact or enchantment", TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Enchantment)
        ))
        effect = Effects.Move(target, Zone.GRAVEYARD, byDestruction = true)
            .then(
                ConditionalEffect(
                    condition = Conditions.TargetSpellManaValueAtMost(DynamicAmount.Fixed(2)),
                    effect = Effects.CreateLander()
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Constantin Marin"
        flavorText = "A new brood finds its home on the doorstep of another's."
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d060d95f-f33a-4fc0-b002-c394d4cd82ce.jpg?1752947393"
    }
}
