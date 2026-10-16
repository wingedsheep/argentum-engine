package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions

/**
 * Synth Infiltrator — Fallout #40
 * {3}{U}{U} · Artifact Creature — Synth · 0/0
 *
 * Improvise
 * You may have this creature enter as a copy of any creature on the battlefield, except it's a
 * Synth artifact creature in addition to its other types.
 *
 * The same optional [EntersAsCopy] replacement as Phyrexian Metamorph / Mirrorhall Mimic: "any"
 * creature, so either player's is fair game; the exception unions in the Artifact and Creature card
 * types and the Synth subtype. Declining leaves a 0/0 that dies to state-based actions.
 */
val SynthInfiltrator = card("Synth Infiltrator") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Synth"
    power = 0
    toughness = 0
    oracleText = "Improvise (Your artifacts can help cast this spell. Each artifact you tap after " +
        "you're done activating mana abilities pays for {1}.)\n" +
        "You may have this creature enter as a copy of any creature on the battlefield, except it's a " +
        "Synth artifact creature in addition to its other types."

    keywords(Keyword.IMPROVISE)

    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.Creature,
            exceptions = CopyExceptions(
                addedCardTypes = setOf(CardType.ARTIFACT, CardType.CREATURE),
                addedSubtypes = setOf(Subtype("Synth")),
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "40"
        artist = "Leonardo Borazio"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3fe16bc4-8457-4c5f-902a-0746112e80a4.jpg?1783912409"
        ruling("2024-03-08", "Improvise doesn't change Synth Infiltrator's mana cost or mana value, and it can't reduce Synth Infiltrator's cost below {U}{U}.")
        ruling("2024-03-08", "Except for being a Synth artifact creature in addition to its other types, Synth Infiltrator copies exactly what was printed on the original creature and nothing else (unless that creature is copying something else or is a token; see below). It doesn't copy whether that creature is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, and so on.")
        ruling("2024-03-08", "If the copied creature has {X} in its mana cost, X is 0.")
        ruling("2024-03-08", "If the copied creature is a token, Synth Infiltrator copies the original characteristics of that token as stated by the effect that created that token, with the noted exceptions.")
        ruling("2024-03-08", "If the copied creature is copying something else, then Synth Infiltrator enters the battlefield as whatever that creature copied, with the noted exceptions.")
        ruling("2024-03-08", "Any enters-the-battlefield abilities of the copied creature will trigger when Synth Infiltrator enters the battlefield. Any \"as [this creature] enters the battlefield\" or \"[this creature] enters the battlefield with\" abilities of the copied creature will also work.")
        ruling("2024-03-08", "You can choose not to have Synth Infiltrator enter the battlefield as a copy of another creature. If you do, it will probably die due to having 0 toughness. (This is probably not the kind of innovation the Institute would approve of.)")
    }
}
