package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.CreateTokenEffect
import com.wingedsheep.rulesengine.ability.DealDamageEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Ajani, Outland Chaperone
 *
 * {1}{W}{W} Legendary Planeswalker — Ajani
 * Starting Loyalty: 3
 *
 * +1: Create a 1/1 green and white Kithkin creature token.
 * −2: Ajani deals 4 damage to target tapped creature.
 * −8: Look at the top X cards of your library, where X is your life total.
 *     You may put any number of nonland permanent cards with mana value 3 or less
 *     from among them onto the battlefield. Then shuffle.
 */
object AjaniOutlandChaperone {
    val definition = CardDefinition.planeswalker(
        name = "Ajani, Outland Chaperone",
        manaCost = ManaCost.parse("{1}{W}{W}"),
        subtypes = setOf(Subtype.AJANI),
        startingLoyalty = 3,
        oracleText = "+1: Create a 1/1 green and white Kithkin creature token.\n" +
                "−2: Ajani deals 4 damage to target tapped creature.\n" +
                "−8: Look at the top X cards of your library, where X is your life total. " +
                "You may put any number of nonland permanent cards with mana value 3 or less " +
                "from among them onto the battlefield. Then shuffle.",
        metadata = ScryfallMetadata(
            collectorNumber = "4",
            rarity = Rarity.MYTHIC,
            artist = "Daren Bader",
            imageUri = "https://cards.scryfall.io/normal/front/6/1/6124a691-ae83-4d22-a177-0aee65b47064.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Ajani, Outland Chaperone") {
        // +1: Create a 1/1 green and white Kithkin creature token
        planeswalkerAbility(
            loyaltyCost = 1,
            effect = CreateTokenEffect(
                count = 1,
                power = 1,
                toughness = 1,
                colors = setOf(Color.GREEN, Color.WHITE),
                creatureTypes = setOf("Kithkin")
            )
        )

        // −2: Ajani deals 4 damage to target tapped creature
        // Note: Targeting tapped creatures requires a filter that will be checked at resolution
        planeswalkerAbility(
            loyaltyCost = -2,
            effect = DealDamageEffect(
                amount = 4,
                target = EffectTarget.TargetTappedCreature
            )
        )

        // −8: Ultimate ability - look at top X cards, put permanents CMC 3 or less onto battlefield
        // Note: This complex ability requires specialized effect handling
        // Placeholder for now - full implementation needs:
        // - LookAtTopCardsEffect with dynamic X based on life total
        // - Filter for nonland permanents with MV <= 3
        // - Put onto battlefield effect
        planeswalkerAbility(
            loyaltyCost = -8,
            effect = CreateTokenEffect(  // Placeholder - actual would be complex library manipulation
                count = 0,
                power = 0,
                toughness = 0,
                colors = emptySet(),
                creatureTypes = emptySet()
            )
        )
    }
}
