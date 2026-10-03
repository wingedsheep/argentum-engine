package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evolveTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Propagator Drone {1}{G} — Modern Horizons 3 #167 (uncommon)
 * Creature — Eldrazi Drone 2/2
 * Devoid
 * Creature tokens you control have evolve.
 * {3}{G}: Create a 0/1 colorless Eldrazi Spawn creature token with "Sacrifice this token: Add {C}."
 *
 * "Have evolve" is two statics over the same group: the [Keyword.EVOLVE] display keyword (which the
 * engine never reads) and a [GrantTriggeredAbility] carrying the one [evolveTriggeredAbility], so
 * each token evolves off its own P/T. Tokens on the battlefield see the Drone itself enter, as the
 * reminder text promises — the Drone's static applies the moment it's on the battlefield.
 */
private val creatureTokensYouControl = GroupFilter(GameObjectFilter.Creature.token().youControl())

val PropagatorDrone = card("Propagator Drone") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 2
    oracleText = "Devoid (This card has no color.)\n" +
        "Creature tokens you control have evolve. (They have \"Whenever a creature you control enters, " +
        "if it has greater power or toughness than this token, put a +1/+1 counter on this token.\" " +
        "They see this creature enter.)\n" +
        "{3}{G}: Create a 0/1 colorless Eldrazi Spawn creature token with \"Sacrifice this token: Add {C}.\""

    keywords(Keyword.DEVOID)

    staticAbility {
        ability = GrantKeyword(Keyword.EVOLVE, creatureTokensYouControl)
    }
    staticAbility {
        ability = GrantTriggeredAbility(evolveTriggeredAbility(), creatureTokensYouControl)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{G}")
        effect = Effects.CreateEldraziSpawn(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "167"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f5b2d89-d641-4815-8e6c-5dba0d31419e.jpg?1783911258"
        ruling("2024-06-07", "When comparing the stats of the two creatures for evolve, you always compare power to power and toughness to toughness.")
        ruling("2024-06-07", "If the creature that entered the battlefield leaves the battlefield before evolve tries to resolve, use its last known power and toughness to compare the stats.")
    }
}
