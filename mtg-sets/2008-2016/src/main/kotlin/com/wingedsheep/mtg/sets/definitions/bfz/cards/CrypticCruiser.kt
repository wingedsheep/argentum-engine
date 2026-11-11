package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Cryptic Cruiser
 * {3}{U}
 * Creature — Eldrazi Processor
 * 3/3
 * Devoid (This card has no color.)
 * {2}{U}, Put a card an opponent owns from exile into that player's graveyard: Tap target creature.
 */
val CrypticCruiser = card("Cryptic Cruiser") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Processor"
    power = 3
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "{2}{U}, Put a card an opponent owns from exile into that player's graveyard: Tap target creature."

    keywords(Keyword.DEVOID)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}"), Costs.PutFromExileIntoGraveyard())
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "Svetlin Velinov"
        flavorText = "\"The seas are no less imperiled than the dry lands. This fight is mine as much as it is yours.\"\n—Kiora, to Gideon Jura"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff2061f7-5e51-4862-9650-0005002d04b6.jpg?1783938214"
        ruling("2015-08-25", "If a spell or ability requires that you put more than one exiled card into the graveyard, you may choose cards owned by different opponents. Each card chosen will be put into its owner's graveyard.")
        ruling("2015-08-25", "If a replacement effect will cause cards that would be put into a graveyard from anywhere to be exiled instead (such as the one created by Anafenza, the Foremost), you can still put an exiled card into its opponent's graveyard. The card becomes a new object and remains in exile. In this situation, you can't use a single exiled card if required to put more than one exiled card into the graveyard. Conversely, you could use the same card in this situation if two separate spells or abilities each required you to put a single exiled card into its owner's graveyard.")
        ruling("2015-08-25", "You can't look at face-down cards in exile unless an effect allows you to.")
        ruling("2015-08-25", "Face-down cards in exile are grouped using two criteria: what caused them to be exiled face down and when they were exiled face down. If you want to put a face-down card in exile into its owner's graveyard, you must first choose one of these groups and then choose a card from within that group at random. For example, say an artifact causes your opponent to exile their hand of three cards face down. Then on a later turn, that artifact causes your opponent to exile another two cards face down. If you use Wasteland Strangler to put one of those cards into their graveyard, you would pick the first or second pile and put a card chosen at random from that pile into the graveyard.")
    }
}
