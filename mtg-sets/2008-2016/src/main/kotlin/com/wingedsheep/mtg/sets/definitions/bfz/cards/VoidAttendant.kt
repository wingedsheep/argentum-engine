package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Void Attendant
 * {2}{G}
 * Creature — Eldrazi Processor
 * 2/3
 * Devoid (This card has no color.)
 * {1}{G}, Put a card an opponent owns from exile into that player's graveyard: Create a 1/1 colorless
 * Eldrazi Scion creature token. It has "Sacrifice this token: Add {C}."
 */
val VoidAttendant = card("Void Attendant") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi Processor"
    power = 2
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "{1}{G}, Put a card an opponent owns from exile into that player's graveyard: Create a 1/1 colorless " +
        "Eldrazi Scion creature token. It has \"Sacrifice this token: Add {C}.\""

    keywords(Keyword.DEVOID)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.PutFromExileIntoGraveyard())
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Eldrazi", "Scion"),
            activatedAbilities = listOf(
                ActivatedAbility(
                    cost = Costs.SacrificeSelf,
                    effect = Effects.AddColorlessMana(1),
                    isManaAbility = true,
                    timing = TimingRule.ManaAbility,
                    descriptionOverride = "Sacrifice this token: Add {C}.",
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/b/9/b999a0fe-d2d0-4367-9abb-6ce5f3764f19.jpg?1783938121",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "169"
        artist = "Viktor Titov"
        flavorText = "The Eldrazi don't merely extinguish life—they supplant it."
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f66eb97-b151-48b1-aff0-f1280af66291.jpg?1783938189"
        ruling("2015-08-25", "Once you announce that you're activating this ability, players can't respond until after you have paid its costs and completed activating it. Specifically, no one can try to remove the card from exile to stop you from activating the ability.")
        ruling("2015-08-25", "If a spell or ability requires that you put more than one exiled card into the graveyard, you may choose cards owned by different opponents. Each card chosen will be put into its owner's graveyard.")
        ruling("2015-08-25", "If a replacement effect will cause cards that would be put into a graveyard from anywhere to be exiled instead (such as the one created by Anafenza, the Foremost), you can still put an exiled card into its opponent's graveyard. The card becomes a new object and remains in exile. In this situation, you can't use a single exiled card if required to put more than one exiled card into the graveyard. Conversely, you could use the same card in this situation if two separate spells or abilities each required you to put a single exiled card into its owner's graveyard.")
        ruling("2015-08-25", "You can't look at face-down cards in exile unless an effect allows you to.")
        ruling("2015-08-25", "Face-down cards in exile are grouped using two criteria: what caused them to be exiled face down and when they were exiled face down. If you want to put a face-down card in exile into its owner's graveyard, you must first choose one of these groups and then choose a card from within that group at random. For example, say an artifact causes your opponent to exile their hand of three cards face down. Then on a later turn, that artifact causes your opponent to exile another two cards face down. If you use Wasteland Strangler to put one of those cards into their graveyard, you would pick the first or second pile and put a card chosen at random from that pile into the graveyard.")
        ruling("2015-08-25", "Eldrazi Scions are similar to Eldrazi Spawn, seen in the Zendikar block. Note that Eldrazi Scions are 1/1, not 0/1.")
        ruling("2015-08-25", "Eldrazi and Scion are each separate creature types. Anything that affects Eldrazi will affect these tokens, for example.")
        ruling("2015-08-25", "Sacrificing an Eldrazi Scion creature token to add {C} is a mana ability. It doesn't use the stack and can't be responded to.")
    }
}
