package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Call of the Nightwing
 * {2}{U}{B}
 * Sorcery
 * Create a 1/1 blue and black Horror creature token with flying.
 * Cipher
 *
 * Cipher is the keyword alone — the engine runs the encoding step after the token is created, so the
 * spell may be encoded on the Horror it just made (2013-01-24 ruling).
 */
val CallOfTheNightwing = card("Call of the Nightwing") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Create a 1/1 blue and black Horror creature token with flying.\n" +
        "Cipher (Then you may exile this spell card encoded on a creature you control. Whenever " +
        "that creature deals combat damage to a player, its controller may cast a copy of the " +
        "encoded card without paying its mana cost.)"

    keywords(Keyword.CIPHER)

    spell {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE, Color.BLACK),
            creatureTypes = setOf("Horror"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/7/9/79a71b52-58f9-4945-9557-0fbcbbf5a241.jpg?1783940147"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef30a570-b988-42e8-8910-41fe39ffa260.jpg?1783940112"
        ruling("2013-01-24", "You can exile Call of the Nightwing encoded on the Horror creature token it just created.")
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
    }
}
