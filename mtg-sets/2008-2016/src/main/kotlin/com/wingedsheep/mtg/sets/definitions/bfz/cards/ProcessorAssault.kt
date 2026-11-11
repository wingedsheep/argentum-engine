package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Processor Assault
 * {1}{R}
 * Sorcery
 * Devoid (This card has no color.)
 * As an additional cost to cast this spell, put a card an opponent owns from exile into that player's graveyard.
 * Processor Assault deals 5 damage to target creature.
 */
val ProcessorAssault = card("Processor Assault") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Devoid (This card has no color.)\n" +
        "As an additional cost to cast this spell, put a card an opponent owns from exile into that player's graveyard.\n" +
        "Processor Assault deals 5 damage to target creature."

    keywords(Keyword.DEVOID)

    additionalCost(Costs.additional.PutFromExileIntoGraveyard())

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(5, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Jama Jurabaev"
        flavorText = "Ulamog's processors trail behind him, converting ruined matter into furious energy."
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a689975-d27b-4b8f-8d46-6e97c754ae81.jpg?1783938197"
        ruling("2015-08-25", "You must put exactly one card an opponent owns from exile into that player's graveyard to cast Processor Assault. You can't cast it without doing so, and you can't put multiple exiled cards into their owners' graveyards this way.")
        ruling("2015-08-25", "Players can respond to Processor Assault only after it's been cast and all of its costs have been paid. No one can try to remove the card from exile to stop you from casting the spell.")
        ruling("2015-08-25", "If a spell or ability requires that you put more than one exiled card into the graveyard, you may choose cards owned by different opponents. Each card chosen will be put into its owner's graveyard.")
        ruling("2015-08-25", "If a replacement effect will cause cards that would be put into a graveyard from anywhere to be exiled instead (such as the one created by Anafenza, the Foremost), you can still put an exiled card into its opponent's graveyard. The card becomes a new object and remains in exile. In this situation, you can't use a single exiled card if required to put more than one exiled card into the graveyard. Conversely, you could use the same card in this situation if two separate spells or abilities each required you to put a single exiled card into its owner's graveyard.")
    }
}
