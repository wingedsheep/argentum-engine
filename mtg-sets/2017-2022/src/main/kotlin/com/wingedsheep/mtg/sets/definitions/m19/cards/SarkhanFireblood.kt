package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Sarkhan, Fireblood {1}{R}{R}
 * Legendary Planeswalker — Sarkhan
 * Starting Loyalty: 3
 *
 * +1: You may discard a card. If you do, draw a card.
 * +1: Add two mana in any combination of colors. Spend this mana only to cast Dragon spells.
 * −7: Create four 5/5 red Dragon creature tokens with flying.
 *
 * The second +1 is a loyalty ability, not a mana ability: it uses the stack and is sorcery-speed
 * (see rulings). "Dragon spells" is the subtype, not the name — [ManaRestriction.SubtypeSpellsOnly]
 * reads the spell's subtypes, so a Kindred Dragon instant qualifies and Dragon's Hoard doesn't.
 */
val SarkhanFireblood = card("Sarkhan, Fireblood") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Sarkhan"
    startingLoyalty = 3
    oracleText = "+1: You may discard a card. If you do, draw a card.\n" +
        "+1: Add two mana in any combination of colors. Spend this mana only to cast Dragon spells.\n" +
        "−7: Create four 5/5 red Dragon creature tokens with flying."

    loyaltyAbility(+1) {
        effect = Effects.May(
            effect = Effects.IfYouDo(
                action = Patterns.Hand.discardCards(1),
                then = Effects.DrawCards(1),
            ),
            descriptionOverride = "You may discard a card. If you do, draw a card.",
        )
    }

    loyaltyAbility(+1) {
        effect = Effects.AddManaInAnyCombination(
            2,
            restriction = ManaRestriction.SubtypeSpellsOnly(setOf(Subtype.DRAGON.value)),
        )
    }

    loyaltyAbility(-7) {
        effect = Effects.CreateToken(
            power = 5,
            toughness = 5,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dragon"),
            keywords = setOf(Keyword.FLYING),
            count = 4,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "154"
        artist = "Grzegorz Rutkowski"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/3553a70e-75de-4a8c-9b20-92cd4a317101.jpg?1783934548"
        ruling("2018-07-13", "Because it's a loyalty ability, Sarkhan's second ability isn't a mana ability. It can be activated only any time you could cast a sorcery. It uses the stack and can be responded to.")
        ruling("2018-07-13", "A \"Dragon spell\" refers only to a spell that has the Dragon subtype, regardless of its name. For example, Dragon's Hoard isn't a Dragon spell.")
    }
}
