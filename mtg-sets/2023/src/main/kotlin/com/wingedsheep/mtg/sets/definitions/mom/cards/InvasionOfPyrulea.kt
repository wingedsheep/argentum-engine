package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Invasion of Pyrulea // Gargantuan Slabhorn — March of the Machine #240.
 * {G}{U} · Battle — Siege · defense 4 // Creature — Beast 4/4
 *
 * When this Siege enters, scry 3, then reveal the top card of your library. If it's a land or
 * double-faced card, draw a card.
 * // Trample, ward {2}
 * // Other transformed permanents you control have trample and ward {2}.
 *
 * The draw is a real draw (the ruling: the revealed card is the one you draw), so it's
 * `Effects.If(whenMatches(revealed, …), DrawCards(1))` rather than a move to hand. The Slabhorn's
 * lord line reads CR 701.27g's "transformed permanent" through `transformed()`; the Slabhorn is
 * itself transformed, which is why the grant says "other".
 */
private val InvasionOfPyruleaFront = card("Invasion of Pyrulea") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Battle — Siege"
    startingDefense = 4
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, scry 3, then reveal the top card of your library. If it's a land " +
        "or double-faced card, draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            run(Patterns.Library.scry(3))
            val revealed = gather(CardSource.TopOfLibrary(1), revealed = true)
            run(Effects.If(
                condition = whenMatches(revealed, GameObjectFilter.Land or Filters.DoubleFaced),
                then = Effects.DrawCards(1),
            ))
        }
        description = "When this Siege enters, scry 3, then reveal the top card of your library. " +
            "If it's a land or double-faced card, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "240"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50180fda-6d67-41e9-8b2b-2e7c5876b8ab.jpg?1783916949"
        ruling("2023-04-14", "If you reveal a land or double-faced card for Invasion of Pyrulea's ability, that revealed card will be the one you draw.")
        ruling("2023-04-14", "A \"transformed permanent\" is a double-faced permanent with its back face up. Notably, modal double-faced permanents and melded permanents are never transformed permanents, no matter which faces are up.")
    }
}

private val GargantuanSlabhorn = card("Gargantuan Slabhorn") {
    manaCost = ""
    colorIdentity = "GU"
    colorIndicator = "GU"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "Trample, ward {2}\n" +
        "Other transformed permanents you control have trample and ward {2}."

    keywords(Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    val otherTransformed = GroupFilter(GameObjectFilter.Permanent.youControl().transformed(), excludeSelf = true)
    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, otherTransformed)
    }
    staticAbility {
        ability = GrantWard(WardCost.Mana("{2}"), otherTransformed)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "240"
        artist = "Nicholas Gregory"
        flavorText = "To the slabhorn, the branches of the Invasion Tree were as brittle as the " +
            "stems of a Pyrulean fern."
        imageUri = "https://cards.scryfall.io/normal/back/5/0/50180fda-6d67-41e9-8b2b-2e7c5876b8ab.jpg?1783916949"
    }
}

val InvasionOfPyrulea: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfPyruleaFront,
    backFace = GargantuanSlabhorn,
)
