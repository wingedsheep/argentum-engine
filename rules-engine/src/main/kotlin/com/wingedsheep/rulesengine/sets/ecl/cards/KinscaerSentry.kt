package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.OnAttack
import com.wingedsheep.rulesengine.ability.PutCreatureFromHandOntoBattlefieldEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Kinscaer Sentry
 *
 * {1}{W} Creature — Kithkin Soldier 2/2
 * First strike, lifelink
 * Whenever this creature attacks, you may put a creature card with mana value X or less
 * from your hand onto the battlefield tapped and attacking, where X is the number of
 * attacking creatures you control.
 */
object KinscaerSentry {
    val definition = CardDefinition.creature(
        name = "Kinscaer Sentry",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype.KITHKIN, Subtype.SOLDIER),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.FIRST_STRIKE, Keyword.LIFELINK),
        oracleText = "First strike, lifelink\n" +
                "Whenever this creature attacks, you may put a creature card with mana value X or less " +
                "from your hand onto the battlefield tapped and attacking, where X is the number of " +
                "attacking creatures you control.",
        metadata = ScryfallMetadata(
            collectorNumber = "22",
            rarity = Rarity.RARE,
            artist = "Kev Fang",
            imageUri = "https://cards.scryfall.io/normal/front/3/3/333bf101-14e8-4753-99bc-9174f42c4122.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Kinscaer Sentry") {
        keywords(Keyword.FIRST_STRIKE, Keyword.LIFELINK)

        // Attack trigger: Put creature from hand onto battlefield tapped and attacking
        triggered(
            trigger = OnAttack(selfOnly = true),
            effect = PutCreatureFromHandOntoBattlefieldEffect(
                maxManaValueSource = com.wingedsheep.rulesengine.ability.DynamicAmount.AttackingCreaturesYouControl,
                entersTapped = true,
                entersAttacking = true
            ),
            optional = true
        )
    }
}
