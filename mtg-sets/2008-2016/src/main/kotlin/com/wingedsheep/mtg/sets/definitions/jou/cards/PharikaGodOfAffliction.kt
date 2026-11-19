package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pharika, God of Affliction
 * {1}{B}{G}
 * Legendary Enchantment Creature — God
 * 5/5
 *
 * Indestructible
 * As long as your devotion to black and green is less than seven, Pharika isn't a creature.
 * {B}{G}: Exile target creature card from a graveyard. Its owner creates a 1/1 black and green
 * Snake enchantment creature token with deathtouch.
 *
 * The token goes to the exiled card's *owner* ([Player.OwnerOf] reads the target's `ownerId`),
 * not to Pharika's controller — exiling an opponent's creature card hands that opponent the Snake.
 */
val PharikaGodOfAffliction = card("Pharika, God of Affliction") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Enchantment Creature — God"
    power = 5
    toughness = 5
    oracleText = "Indestructible\n" +
        "As long as your devotion to black and green is less than seven, Pharika isn't a creature.\n" +
        "{B}{G}: Exile target creature card from a graveyard. Its owner creates a 1/1 black and green " +
        "Snake enchantment creature token with deathtouch."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(
            DynamicAmounts.devotionTo(Color.BLACK, Color.GREEN),
            ComparisonOperator.LT,
            7
        )
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    activatedAbility {
        cost = Costs.Mana("{B}{G}")
        val creatureCard = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.Exile(creatureCard) then Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK, Color.GREEN),
            creatureTypes = setOf("Snake"),
            keywords = setOf(Keyword.DEATHTOUCH),
            enchantmentToken = true,
            controller = EffectTarget.PlayerRef(Player.OwnerOf("target creature card")),
            imageUri = "https://cards.scryfall.io/normal/front/5/9/5910a7f2-71a7-4e85-8feb-4a0da228eb15.jpg?1783939392"
        )
        description = "{B}{G}: Exile target creature card from a graveyard. Its owner creates a 1/1 black " +
            "and green Snake enchantment creature token with deathtouch."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "154"
        artist = "Peter Mohrbacher"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75ec8548-5790-4eac-8780-cdd126438192.jpg?1783939402"
        ruling(
            "2020-01-24",
            "Your devotion to two colors is the number of mana symbols among mana costs of permanents you control " +
                "that are the first color, the second, or both. If an effect counts your devotion to two colors, a " +
                "hybrid symbol that is both of those colors is counted just once."
        )
        ruling(
            "2020-01-24",
            "If a God stops being a creature, it loses the type creature and the creature type God. It continues " +
                "to be a legendary enchantment."
        )
        ruling(
            "2020-01-24",
            "The abilities of Gods function as long as they're on the battlefield, regardless of whether they're creatures."
        )
    }
}
