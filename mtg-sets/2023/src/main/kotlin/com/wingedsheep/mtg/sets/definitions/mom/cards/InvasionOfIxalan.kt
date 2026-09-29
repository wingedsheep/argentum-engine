package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Invasion of Ixalan // Belligerent Regisaur — March of the Machine #191.
 * {1}{G} · Battle — Siege · defense 4 // Creature — Dinosaur 4/3
 *
 * Front: a top-five dig for a permanent card (revealed into hand), the rest to the bottom in a
 * random order. Back: trample, and "whenever you cast a spell" it gains indestructible until end
 * of turn.
 */
private val InvasionOfIxalanFront = card("Invasion of Ixalan") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Battle — Siege"
    startingDefense = 4
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, look at the top five cards of your library. You may reveal a " +
        "permanent card from among them and put it into your hand. Put the rest on the bottom of " +
        "your library in a random order."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 5,
            filter = GameObjectFilter.Permanent,
            prompt = "You may reveal a permanent card from among them and put it into your hand",
        )
        description = "When this Siege enters, look at the top five cards of your library. You may " +
            "reveal a permanent card from among them and put it into your hand. Put the rest on the " +
            "bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "191"
        artist = "Viktor Titov"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa20fc16-e106-4953-ace4-5ac9c7fec97b.jpg?1783916974"
    }
}

private val BelligerentRegisaur = card("Belligerent Regisaur") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G"
    typeLine = "Creature — Dinosaur"
    power = 4
    toughness = 3
    oracleText = "Trample\nWhenever you cast a spell, this creature gains indestructible until end of turn."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "191"
        artist = "Viktor Titov"
        flavorText = "\"Careful. Its vision is based on fear, and it can smell movement . . . or " +
            "something like that.\"\n—Gregor, amateur zoologist"
        imageUri = "https://cards.scryfall.io/normal/back/f/a/fa20fc16-e106-4953-ace4-5ac9c7fec97b.jpg?1783916974"
    }
}

val InvasionOfIxalan: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfIxalanFront,
    backFace = BelligerentRegisaur,
)
