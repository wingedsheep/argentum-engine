package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.emerge
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vexing Scuttler
 * {8}
 * Creature — Eldrazi Crab
 * 4/5
 *
 * Emerge {6}{U}
 * When you cast this spell, you may return target instant or sorcery card from your graveyard to
 * your hand.
 *
 * The return is a *cast* trigger: it resolves before the Scuttler itself, and still resolves if the
 * spell is countered.
 */
val VexingScuttler = card("Vexing Scuttler") {
    manaCost = "{8}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Crab"
    power = 4
    toughness = 5
    oracleText = "Emerge {6}{U} (You may cast this spell by sacrificing a creature and paying the " +
        "emerge cost reduced by that creature's mana value.)\n" +
        "When you cast this spell, you may return target instant or sorcery card from your graveyard " +
        "to your hand."

    emerge("{6}{U}")

    triggeredAbility {
        trigger = Triggers.self.isCast()
        val spellCard = target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.May(Effects.ReturnToHand(spellCard))
        description = "When you cast this spell, you may return target instant or sorcery card from " +
            "your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "11"
        artist = "James Paick"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c3dd0aa-f6e9-435c-af64-20e9de67efe9.jpg"
    }
}
