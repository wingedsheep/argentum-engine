package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost

/**
 * Champions of the Perfect
 * {3}{G}
 * Creature — Elf Warrior
 * 6/6
 *
 * As an additional cost to cast this spell, behold an Elf and exile it.
 * (Exile an Elf you control or an Elf card from your hand.)
 * Whenever you cast a creature spell, draw a card.
 * When this creature leaves the battlefield, return the exiled card to its owner's hand.
 */
val ChampionsOfThePerfect = card("Champions of the Perfect") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Elf Warrior"
    power = 6
    toughness = 6
    oracleText = "As an additional cost to cast this spell, behold an Elf and exile it. (Exile an Elf you control or an Elf card from your hand.)\nWhenever you cast a creature spell, draw a card.\nWhen this creature leaves the battlefield, return the exiled card to its owner's hand."

    additionalCost(AdditionalCost.BeholdAndExile(filter = Filters.WithSubtype("Elf")))

    triggeredAbility {
        trigger = Triggers.YouCastCreature
        effect = Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileToHand()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "171"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f359211-8be5-4818-b73c-14f24b7ddb21.jpg?1767658362"
    }
}
