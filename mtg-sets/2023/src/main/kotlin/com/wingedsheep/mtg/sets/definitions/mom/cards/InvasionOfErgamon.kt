package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Invasion of Ergamon // Truga Cliffcharger — March of the Machine #233.
 * {R}{G} · Battle — Siege · defense 5 // Creature — Rhino 3/4
 *
 * Front: a Treasure, then the Invasion of Mercadia rummage ([Effects.May] around
 * [Effects.IfYouDo]) — nothing is drawn unless a card was actually discarded.
 * Back: the same may-discard gate in front of a revealed land-or-battle tutor to hand.
 */
private val InvasionOfErgamonFront = card("Invasion of Ergamon") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Battle — Siege"
    startingDefense = 5
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, create a Treasure token. Then you may discard a card. If you do, " +
        "draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateTreasure() then
            Effects.May(Effects.IfYouDo(Effects.Discard(1), Effects.DrawCards(1)))
        description = "When this Siege enters, create a Treasure token. Then you may discard a card. " +
            "If you do, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Manuel Castañón"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3e0fc49-f765-4bd1-a2b9-23e703388b50.jpg?1783916953"
    }
}

private val TrugaCliffcharger = card("Truga Cliffcharger") {
    manaCost = ""
    colorIdentity = "RG"
    colorIndicator = "RG"
    typeLine = "Creature — Rhino"
    power = 3
    toughness = 4
    oracleText = "Trample\n" +
        "When this creature enters, you may discard a card. If you do, search your library for a " +
        "land or battle card, reveal it, put it into your hand, then shuffle."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.IfYouDo(
                Effects.Discard(1),
                Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.Land or GameObjectFilter.Battle,
                    reveal = true,
                )
            )
        )
        description = "When this creature enters, you may discard a card. If you do, search your " +
            "library for a land or battle card, reveal it, put it into your hand, then shuffle."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Manuel Castañón"
        flavorText = "Throughout Ergamon's wild Truga Jungle, predator-prey relationships stopped until every last invader was crushed to dust."
        imageUri = "https://cards.scryfall.io/normal/back/b/3/b3e0fc49-f765-4bd1-a2b9-23e703388b50.jpg?1783916953"
    }
}

val InvasionOfErgamon: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfErgamonFront,
    backFace = TrugaCliffcharger,
)
