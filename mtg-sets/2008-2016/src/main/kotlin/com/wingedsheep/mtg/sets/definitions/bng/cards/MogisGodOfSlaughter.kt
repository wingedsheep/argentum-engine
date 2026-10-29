package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mogis, God of Slaughter — Born of the Gods #151
 * {2}{B}{R} · Legendary Enchantment Creature — God · 7/5
 * Indestructible
 * As long as your devotion to black and red is less than seven, Mogis isn't a creature.
 * At the beginning of each opponent's upkeep, Mogis deals 2 damage to that player unless they
 * sacrifice a creature of their choice.
 *
 * The devotion clause is the two-colour God template (Athreos, Iroas). The punisher is Curse
 * Artifact's [Effects.PayOrSuffer] offered to the upkeep player ([Player.TriggeringPlayer]): that
 * player picks which creature to sacrifice, and one who controls no creature can't pay and takes
 * the 2 damage (ruling).
 */
val MogisGodOfSlaughter = card("Mogis, God of Slaughter") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Enchantment Creature — God"
    power = 7
    toughness = 5
    oracleText = "Indestructible\n" +
        "As long as your devotion to black and red is less than seven, Mogis isn't a creature.\n" +
        "At the beginning of each opponent's upkeep, Mogis deals 2 damage to that player unless " +
        "they sacrifice a creature of their choice."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(
            DynamicAmounts.devotionTo(Color.BLACK, Color.RED),
            ComparisonOperator.LT,
            7
        )
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.beginningOf(Step.UPKEEP)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Sacrifice(GameObjectFilter.Creature),
            suffer = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.TriggeringPlayer)),
            player = EffectTarget.PlayerRef(Player.TriggeringPlayer),
        )
        description = "At the beginning of each opponent's upkeep, Mogis deals 2 damage to that " +
            "player unless they sacrifice a creature of their choice."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "151"
        artist = "Peter Mohrbacher"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a0417bf-b735-46d7-9985-2d991051020f.jpg?1783939522"
        ruling("2014-02-01", "If the player can't sacrifice a creature (usually because they don't control one), Mogis will deal 2 damage to them.")
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
            "The type-changing ability that can make a God not be a creature functions only on the battlefield. " +
                "It's always a creature card in other zones, regardless of your devotion to its color. It's always " +
                "a creature spell while it's on the stack."
        )
    }
}
