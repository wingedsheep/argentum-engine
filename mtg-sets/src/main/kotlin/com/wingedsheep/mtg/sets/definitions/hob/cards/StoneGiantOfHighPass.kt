package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Stone-Giant of High Pass
 * {5}{R}{R}
 * Creature — Giant
 * 7/7
 *
 * Whenever this creature enters or attacks, create a 3/1 colorless Wall artifact creature token
 * with defender named Stone Boulder.
 * {2}{R}, Sacrifice an artifact: This creature deals 4 damage to any target.
 *
 * "Enters or attacks" is two separate triggered abilities on the same card (the printed wording is
 * a shorthand for both), matching Sentinel of the Nameless City.
 *
 * The token carries a printed name that isn't derived from its creature type, so it uses the raw
 * [CreateTokenEffect] rather than the `Effects.CreateToken` facade — the facade names tokens
 * "<Type> Token" and can't express "Stone Boulder".
 */
private fun stoneBoulderToken() = CreateTokenEffect(
    power = 3,
    toughness = 1,
    colors = emptySet(),
    creatureTypes = setOf("Wall"),
    keywords = setOf(Keyword.DEFENDER),
    name = "Stone Boulder",
    artifactToken = true,
    imageUri = "https://cards.scryfall.io/normal/front/3/4/3440e247-a733-4609-9d80-d4a5fc58ae46.jpg?1785502811",
)

val StoneGiantOfHighPass = card("Stone-Giant of High Pass") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant"
    oracleText = "Whenever this creature enters or attacks, create a 3/1 colorless Wall artifact " +
        "creature token with defender named Stone Boulder.\n" +
        "{2}{R}, Sacrifice an artifact: This creature deals 4 damage to any target."
    power = 7
    toughness = 7

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = stoneBoulderToken()
        description = "Whenever this creature enters, create a 3/1 colorless Wall artifact " +
            "creature token with defender named Stone Boulder."
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = stoneBoulderToken()
        description = "Whenever this creature attacks, create a 3/1 colorless Wall artifact " +
            "creature token with defender named Stone Boulder."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.Sacrifice(GameObjectFilter.Artifact))
        val t = target("any target", AnyTarget())
        effect = Effects.DealDamage(4, t)
        description = "{2}{R}, Sacrifice an artifact: This creature deals 4 damage to any target."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "113"
        artist = "Miklós Ligeti"
        flavorText = "The stone-giants were out and were hurling rocks at one another for a game."
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f4f4683-ffd2-447a-932b-276f7fa17cca.jpg?1785496221"
    }
}
