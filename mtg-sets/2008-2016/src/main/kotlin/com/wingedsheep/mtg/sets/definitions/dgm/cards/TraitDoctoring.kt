package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.TextWordCategory
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Trait Doctoring
 * {U}
 * Sorcery
 * Change the text of target permanent by replacing all instances of one color word with another
 * or one basic land type with another until end of turn.
 * Cipher
 *
 * Crystal Spray's text change (both word categories, until end of turn) restricted to a permanent
 * target, plus the cipher keyword — the engine runs the encoding step after the text change.
 */
val TraitDoctoring = card("Trait Doctoring") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Change the text of target permanent by replacing all instances of one color word " +
        "with another or one basic land type with another until end of turn.\n" +
        "Cipher (Then you may exile this spell card encoded on a creature you control. Whenever " +
        "that creature deals combat damage to a player, its controller may cast a copy of the " +
        "encoded card without paying its mana cost.)"

    keywords(Keyword.CIPHER)

    spell {
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.ChangeWordInText(TextWordCategory.entries.toSet(), permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e21a7981-5940-4b75-907f-7600a742f946.jpg?1783940041"
        ruling("2013-04-15", "The spell with cipher is encoded on the creature as part of that spell's resolution, just after the spell's other effects. That card goes directly from the stack to exile. It never goes to the graveyard.")
        ruling("2013-04-15", "You choose the creature as the spell resolves. The cipher ability doesn't target that creature, although the spell with cipher may target that creature (or a different creature) because of its other abilities.")
        ruling("2013-04-15", "If the spell with cipher doesn't resolve, none of its effects will happen, including cipher. The card will go to its owner's graveyard and won't be encoded on a creature.")
        ruling("2013-04-15", "If the creature leaves the battlefield, the exiled card will no longer be encoded on any creature. It will stay exiled.")
        ruling("2013-04-15", "If you want to encode the card with cipher onto a noncreature permanent such as a Keyrune that can turn into a creature, that permanent has to be a creature before the spell with cipher starts resolving. You can choose only a creature to encode the card onto.")
        ruling("2013-04-15", "The copy of the card with cipher is created in and cast from exile.")
        ruling("2013-04-15", "You cast the copy of the card with cipher during the resolution of the triggered ability. Ignore timing restrictions based on the card's type.")
        ruling("2013-04-15", "If you choose not to cast the copy, or you can't cast it (perhaps because there are no legal targets available), the copy will cease to exist the next time state-based actions are performed. You won't get a chance to cast the copy at a later time.")
        ruling("2013-04-15", "The exiled card with cipher grants a triggered ability to the creature it's encoded on. If that creature loses that ability and subsequently deals combat damage to a player, the triggered ability won't trigger. However, the exiled card will continue to be encoded on that creature.")
        ruling("2013-04-15", "If another player gains control of the creature, that player will control the triggered ability. That player will create a copy of the encoded card and may cast it.")
        ruling("2013-04-15", "If a creature with an encoded card deals combat damage to more than one player simultaneously (perhaps because some of the combat damage was redirected), the triggered ability will trigger once for each player it deals combat damage to. Each ability will create a copy of the exiled card and allow you to cast it.")
        ruling("2013-04-15", "Trait Doctoring changes the color word or basic land type each time it appears in the permanent's type line and/or rules text. It doesn't change the name of the card or any instances of the word being used as part of a card's name.")
        ruling("2013-04-15", "You choose what word you're changing and what word you're changing it to as Trait Doctoring resolves.")
        ruling("2013-04-15", "You can target any permanent with Trait Doctoring, including one with no color words or basic land types on it.")
        ruling("2013-04-15", "Trait Doctoring's effect changes only the text printed on the permanent. It can't change words found in abilities it's been granted. For example, if you've changed \"green\" to \"blue,\" and the permanent gains protection from green (as opposed to having protection from green printed on it), that protection ability is unaffected. The permanent will have protection from green.")
        ruling("2013-04-15", "If the basic land type of a land is changed, the associated mana ability of that land will also change, but its name will not. For example, if you've changed \"Plains\" to \"Island,\" a card named Plains will have the basic land type Island and can be tapped for {U}, and it can no longer be tapped for {W}.")
        ruling("2013-04-15", "The type-changing effect can change part of a word such as \"nonblack\" or \"swampwalk\" if the part of the word is being used to refer to a color or basic land type.")
    }
}
