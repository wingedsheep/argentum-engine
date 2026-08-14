package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thing in the Ice // Awoken Horror (Shadows over Innistrad #92 — the card's earliest printing;
 * also reprinted in Shadows of the Past and Innistrad Remastered)
 * {1}{U}
 * Creature — Horror 0/4 // Creature — Kraken Horror 7/8
 *
 * Front — Thing in the Ice ({1}{U}, Creature — Horror, 0/4)
 *   Defender
 *   This creature enters with four ice counters on it.
 *   Whenever you cast an instant or sorcery spell, remove an ice counter from this creature.
 *   Then if it has no ice counters on it, transform it.
 *
 * Back — Awoken Horror (Creature — Kraken Horror, 7/8, blue)
 *   When this creature transforms into Awoken Horror, return all non-Horror creatures to their
 *   owners' hands.
 *
 * Implementation:
 *  - "Enters with four ice counters" is a replacement effect (CR 614.1c), not an ETB trigger:
 *    [EntersWithCounters] over the new [Counters.ICE] counter, `selfOnly`.
 *  - The cast trigger is [Triggers.YouCastInstantOrSorcery] → [Effects.RemoveCounters] of one ice
 *    counter, then a [ConditionalEffect] on [Conditions.SourceCounterCountAtMost]`(ice, 0)` that
 *    flips it. Gating the transform on the *live* count after the removal is what makes the printed
 *    ruling hold: taking the last counter off any other way never transforms it, because only this
 *    ability's resolution runs the check.
 *  - The back's trigger is [Triggers.TransformsToBack] (the ability lives on the face that ends up
 *    up), returning every creature that isn't a Horror — both players' — via
 *    [Patterns.Group.returnAllToHand]. Awoken Horror is itself a Horror, so it never bounces
 *    itself; a *front-face* Thing in the Ice on the battlefield is a Horror too and also stays.
 */

private val NonHorrorCreatures: GroupFilter =
    GroupFilter(GameObjectFilter.Creature.notSubtype(Subtype("Horror")))

private val ThingInTheIceFront = card("Thing in the Ice") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Horror"
    power = 0
    toughness = 4
    oracleText = "Defender\n" +
        "This creature enters with four ice counters on it.\n" +
        "Whenever you cast an instant or sorcery spell, remove an ice counter from this creature. " +
        "Then if it has no ice counters on it, transform it."

    keywords(Keyword.DEFENDER)

    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.Named(Counters.ICE),
            count = 4,
            selfOnly = true,
        )
    )

    triggeredAbility {
        trigger = Triggers.YouCastInstantOrSorcery
        effect = Effects.Composite(
            Effects.RemoveCounters(Counters.ICE, 1, EffectTarget.Self),
            ConditionalEffect(
                condition = Conditions.SourceCounterCountAtMost(Counters.ICE, 0),
                effect = TransformEffect(EffectTarget.Self),
            ),
        )
        description = "Remove an ice counter from this creature. Then if it has no ice counters " +
            "on it, transform it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/359d1b13-6156-43b0-a9a7-6bfff36c1a91.jpg?1783937788"
        ruling(
            "2025-01-24",
            "An ability that triggers when a player casts a spell resolves before the spell that " +
                "caused it to trigger. It resolves even if that spell is countered or otherwise " +
                "leaves the stack without resolving."
        )
        ruling(
            "2025-01-24",
            "When Thing in the Ice's triggered ability transforms it, Awoken Horror's ability will " +
                "trigger and resolve before the spell that caused Thing in the Ice's last ability " +
                "to trigger."
        )
        ruling(
            "2025-01-24",
            "Removing all ice counters from Thing in the Ice some other way will not cause it to " +
                "transform. You'll need to cast an instant or sorcery spell and cause its last " +
                "ability to trigger."
        )
    }
}

private val AwokenHorror = card("Awoken Horror") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Kraken Horror"
    power = 7
    toughness = 8
    oracleText = "When this creature transforms into Awoken Horror, return all non-Horror " +
        "creatures to their owners' hands."

    triggeredAbility {
        trigger = Triggers.TransformsToBack
        effect = Patterns.Group.returnAllToHand(NonHorrorCreatures)
        description = "Return all non-Horror creatures to their owners' hands."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Svetlin Velinov"
        flavorText = "\"It serves as evidence of the ancient power of the deep, a reminder that " +
            "the sea is the only thing worthy of reverence.\"\n—Runo Stromkirk"
        imageUri = "https://cards.scryfall.io/normal/back/3/5/359d1b13-6156-43b0-a9a7-6bfff36c1a91.jpg?1783937788"
    }
}

val ThingInTheIce: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = ThingInTheIceFront,
    backFace = AwokenHorror,
)
