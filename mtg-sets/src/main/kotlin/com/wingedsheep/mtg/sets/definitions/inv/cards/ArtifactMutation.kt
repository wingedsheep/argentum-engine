package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Artifact Mutation (INV 231)
 * {R}{G}
 * Instant
 * Destroy target artifact. It can't be regenerated. Create X 1/1 green Saproling creature tokens,
 * where X is that artifact's mana value.
 *
 * The token count reads the destroyed artifact's mana value via [DynamicAmount.EntityProperty];
 * mana value is a card characteristic that survives the move to the graveyard, so the count
 * resolves correctly after [Effects.Destroy] (CR 608.2g — last-known information). Mirrors
 * [AuraMutation]. If the artifact is an illegal target on resolution the whole spell is countered
 * by the rules and no Saprolings are created.
 *
 * "It can't be regenerated" is currently not enforced for the single-target [Effects.Destroy]
 * facade (only the [Effects.DestroyAll] pipeline carries a `noRegenerate` flag). This matches the
 * existing precedent for single-target destroy-and-can't-regenerate spells in this set
 * (e.g. Plague Spores); artifact regeneration shields are rare in practice.
 */
val ArtifactMutation = card("Artifact Mutation") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Instant"
    oracleText = "Destroy target artifact. It can't be regenerated. Create X 1/1 green Saproling " +
        "creature tokens, where X is that artifact's mana value."

    spell {
        val t = target("target artifact", Targets.Artifact)
        effect = Effects.Destroy(t) then CreateTokenEffect(
            count = DynamicAmount.EntityProperty(
                entity = EntityReference.Target(0),
                numericProperty = EntityNumericProperty.ManaValue
            ),
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling"),
            imageUri = "/images/tokens/inv-saproling.jpeg"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "231"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5eef49c-a80f-4622-ba77-999f9151c841.jpg?1562937931"
    }
}
