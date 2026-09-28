package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Invasion of Vryn // Overloaded Mage-Ring — March of the Machine #64.
 * {3}{U} · Battle — Siege · defense 4 // Artifact
 *
 * When this Siege enters, draw three cards, then discard a card.
 * // {1}, {T}, Sacrifice this artifact: Copy target spell you control. You may choose new targets
 * for the copy.
 */
private val InvasionOfVrynFront = card("Invasion of Vryn") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Battle — Siege"
    startingDefense = 4
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, draw three cards, then discard a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Hand.loot(draw = 3)
        description = "When this Siege enters, draw three cards, then discard a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Leon Tukker"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0ef6737-8e85-42ca-8ee6-6a34f9462de3.jpg?1783917038"
    }
}

private val OverloadedMageRing = card("Overloaded Mage-Ring") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U"
    typeLine = "Artifact"
    oracleText = "{1}, {T}, Sacrifice this artifact: Copy target spell you control. You may choose " +
        "new targets for the copy. (A copy of a permanent spell becomes a token.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        val spell = target(TargetFilter.SpellOnStack.youControl())
        effect = Effects.CopyTargetSpell(target = spell)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Leon Tukker"
        flavorText = "\"On my signal, disable the safeguards. Let's blow these metal freaks right " +
            "back to their own world!\"\n—Gav Beleren, mage-ring engineer"
        imageUri = "https://cards.scryfall.io/normal/back/c/0/c0ef6737-8e85-42ca-8ee6-6a34f9462de3.jpg?1783917038"
    }
}

val InvasionOfVryn: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfVrynFront,
    backFace = OverloadedMageRing,
)
