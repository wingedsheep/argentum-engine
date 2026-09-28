package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Invasion of Ravnica // Guildpact Paragon — March of the Machine #1.
 * {5} · Battle — Siege · defense 4 // Artifact Creature — Construct 5/5
 *
 * When this Siege enters, exile target nonland permanent an opponent controls that isn't exactly
 * two colors.
 * // Whenever you cast a spell that's exactly two colors, look at the top six cards of your
 * library. You may reveal a card that's exactly two colors from among them and put it into your
 * hand. Put the rest on the bottom of your library in a random order.
 *
 * "Exactly two colors" is `exactlyColors(2)` (CR 105.2); colorless and three-color permanents are
 * both legal targets for the front face.
 */
private val InvasionOfRavnicaFront = card("Invasion of Ravnica") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Battle — Siege"
    startingDefense = 4
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, exile target nonland permanent an opponent controls that isn't " +
        "exactly two colors."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(
            TargetFilter(GameObjectFilter.NonlandPermanent.opponentControls().notExactlyColors(2))
        )
        effect = Effects.Exile(permanent)
        description = "When this Siege enters, exile target nonland permanent an opponent " +
            "controls that isn't exactly two colors."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Leon Tukker"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73f8fc4f-2f36-4932-8d04-3c2651c116dc.jpg?1783917079"
    }
}

private val GuildpactParagon = card("Guildpact Paragon") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 5
    toughness = 5
    oracleText = "Whenever you cast a spell that's exactly two colors, look at the top six cards of " +
        "your library. You may reveal a card that's exactly two colors from among them and put it " +
        "into your hand. Put the rest on the bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.exactlyColors(2))
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(count = 6, player = Player.You))
            val (kept, toBottom) = chooseUpToSplit(
                1,
                from = looked,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Any.exactlyColors(2),
                showAllCards = true,
                prompt = "You may reveal a card that's exactly two colors to put into your hand.",
                selectedLabel = "Put into hand",
                remainderLabel = "Put on bottom"
            )
            toHand(kept, revealed = true)
            toLibraryBottom(toBottom, order = CardOrder.Random)
        }
        description = "Whenever you cast a spell that's exactly two colors, look at the top six " +
            "cards of your library. You may reveal a card that's exactly two colors from among them " +
            "and put it into your hand. Put the rest on the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Leon Tukker"
        imageUri = "https://cards.scryfall.io/normal/back/7/3/73f8fc4f-2f36-4932-8d04-3c2651c116dc.jpg?1783917079"
    }
}

val InvasionOfRavnica: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfRavnicaFront,
    backFace = GuildpactParagon,
)
