package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gallows at Willow Hill — Avacyn Restored #215
 * {3} · Artifact
 *
 * {3}, {T}, Tap three untapped Humans you control: Destroy target creature. Its controller creates a
 * 1/1 white Spirit creature token with flying.
 *
 * The Spirit goes to the destroyed creature's controller (Pongify's shape), and it is created even
 * if the creature survives — only an illegal target stops the whole ability.
 */
val GallowsAtWillowHill = card("Gallows at Willow Hill") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "{3}, {T}, Tap three untapped Humans you control: Destroy target creature. " +
        "Its controller creates a 1/1 white Spirit creature token with flying."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}"),
            Costs.Tap,
            Costs.TapPermanents(3, GameObjectFilter.Permanent.withSubtype("Human"))
        )
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature) then
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Spirit"),
                keywords = setOf(Keyword.FLYING),
                controller = EffectTarget.TargetController,
                imageUri = "https://cards.scryfall.io/normal/front/5/9/59e79ba0-33c8-46c8-8694-8bf854345fe7.jpg?1783940638"
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "215"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a840ee7-5728-4b1b-92ac-54612e5397b3.jpg?1783940652"
    }
}
