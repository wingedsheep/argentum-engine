package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWebSlingingToSpells
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Peter Parker // Amazing Spider-Man — Marvel's Spider-Man #10 (mythic)
 *
 * Front — Peter Parker · {1}{W} · Legendary Creature — Human Scientist Hero · 0/1
 *   When Peter Parker enters, create a 2/1 green Spider creature token with reach.
 *   {1}{G}{W}{U}: Transform Peter Parker. Activate only as a sorcery.
 *
 * Back — Amazing Spider-Man · Legendary Creature — Spider Human Hero · 4/4
 *   Vigilance, reach
 *   Each legendary spell you cast that's one or more colors has web-slinging {G}{W}{U}.
 *
 * Modeled as a transforming double-faced creature ([CardDefinition.doubleFacedCreature]); the front
 * owns the sorcery-speed [TransformEffect] flip. The back is a transformed face reached only via the
 * flip, so it carries no castable mana cost — its G/W/U colors come from a color indicator (CR 204).
 *
 *  - Front: an ETB [Effects.CreateToken] (2/1 green Spider with reach) and the transform ability.
 *  - Back: vigilance/reach; and a [GrantWebSlingingToSpells] static granting web-slinging {G}{W}{U}
 *    to every legendary, one-or-more-colored spell its controller casts — read by the web-slinging
 *    cast enumerator / handler via `WebSlinging.effectiveWebSlinging` alongside printed web-slinging.
 */

private val PeterParkerFront = card("Peter Parker") {
    manaCost = "{1}{W}"
    colorIdentity = "GWU"
    typeLine = "Legendary Creature — Human Scientist Hero"
    power = 0
    toughness = 1
    oracleText = "When Peter Parker enters, create a 2/1 green Spider creature token with reach.\n" +
        "{1}{G}{W}{U}: Transform Peter Parker. Activate only as a sorcery."

    // When Peter Parker enters, create a 2/1 green Spider creature token with reach.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Spider"),
            keywords = setOf(Keyword.REACH),
            imageUri = "https://cards.scryfall.io/normal/front/4/a/4a40f6e1-3545-4503-af3e-f0acfb735e3a.jpg?1783905184",
        )
        description = "When Peter Parker enters, create a 2/1 green Spider creature token with reach."
    }

    // {1}{G}{W}{U}: Transform Peter Parker. Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Mana("{1}{G}{W}{U}")
        effect = TransformEffect(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform Peter Parker. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "10"
        artist = "Thanh Tuấn"
        flavorText = "\"This batch of web fluid will just have to wait!\""
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3ce33422-5dba-4a42-8375-dd8ccc692a7b.jpg?1783905367"
    }
}

private val AmazingSpiderManBack = card("Amazing Spider-Man") {
    manaCost = ""
    colorIdentity = "GWU"
    colorIndicator = "GWU" // Transformed back face, no mana cost (CR 204).
    typeLine = "Legendary Creature — Spider Human Hero"
    power = 4
    toughness = 4
    oracleText = "Vigilance, reach\n" +
        "Each legendary spell you cast that's one or more colors has web-slinging {G}{W}{U}. (You " +
        "may cast a spell for its web-slinging cost if you also return a tapped creature you " +
        "control to its owner's hand.)"

    keywords(Keyword.VIGILANCE, Keyword.REACH)

    // Each legendary spell you cast that's one or more colors has web-slinging {G}{W}{U}.
    staticAbility {
        ability = GrantWebSlingingToSpells(
            cost = ManaCost.parse("{G}{W}{U}"),
            spellFilter = GameObjectFilter(
                cardPredicates = listOf(CardPredicate.IsLegendary, CardPredicate.IsColored)
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "10"
        artist = "Thanh Tuấn"
        flavorText = "\"Time to swing into action!\""
        imageUri = "https://cards.scryfall.io/normal/back/3/c/3ce33422-5dba-4a42-8375-dd8ccc692a7b.jpg?1783905367"
    }
}

val PeterParker: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = PeterParkerFront,
    backFace = AmazingSpiderManBack,
)
