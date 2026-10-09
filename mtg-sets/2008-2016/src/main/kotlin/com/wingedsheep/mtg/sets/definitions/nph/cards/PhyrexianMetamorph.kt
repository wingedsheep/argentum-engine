package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions

/**
 * Phyrexian Metamorph — New Phyrexia #42
 * {3}{U/P} · Artifact Creature — Phyrexian Shapeshifter · 0/0
 *
 * You may have this creature enter as a copy of any artifact or creature on the battlefield,
 * except it's an artifact in addition to its other types.
 *
 * The same [EntersAsCopy] replacement as Sculpting Steel / Hulking Metamorph: "any" artifact or
 * creature, so either player's is fair game, and the exception adds only the Artifact card type
 * (copying a noncreature artifact leaves it a noncreature). Declining leaves a 0/0 that dies to
 * state-based actions. The {U/P} pip is parsed by `ManaCost.parse` (CR 107.4f).
 */
val PhyrexianMetamorph = card("Phyrexian Metamorph") {
    manaCost = "{3}{U/P}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Phyrexian Shapeshifter"
    power = 0
    toughness = 0
    oracleText = "({U/P} can be paid with either {U} or 2 life.)\n" +
        "You may have this creature enter as a copy of any artifact or creature on the battlefield, " +
        "except it's an artifact in addition to its other types."

    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.CreatureOrArtifact,
            exceptions = CopyExceptions(addedCardTypes = setOf(CardType.ARTIFACT)),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Jana Schirmer & Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2e27911-87cb-49a0-a34f-6afe4bddd592.jpg?1783941318"
        ruling("2011-06-01", "If Phyrexian Metamorph copies a noncreature artifact, it is no longer a creature.")
        ruling("2011-06-01", "If the chosen creature is copying something else (for example, if the chosen creature is a Clone), then your Phyrexian Metamorph enters as whatever the chosen creature copied, except it's also an artifact.")
        ruling("2011-06-01", "You can choose not to copy anything. In that case, Phyrexian Metamorph simply enters as a 0/0 artifact creature and is put into its owner's graveyard as a state-based action (unless something else is raising its toughness).")
    }
}
