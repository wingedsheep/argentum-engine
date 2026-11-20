package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Nessian Asp — Theros #164
 * {4}{G} · Creature — Snake · 4/5
 *
 * Reach
 * {6}{G}: Monstrosity 4.
 */
val NessianAsp = card("Nessian Asp") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake"
    power = 4
    toughness = 5
    oracleText = "Reach\n" +
        "{6}{G}: Monstrosity 4. (If this creature isn't monstrous, put four +1/+1 counters on it and it becomes monstrous.)"

    keywords(Keyword.REACH)

    activatedAbility {
        cost = Costs.Mana("{6}{G}")
        effect = Effects.Monstrosity(4)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Alex Horley-Orlandelli"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d922596-4c2d-47fd-8c28-0e62972effbf.jpg?1783939742"
        ruling("2013-09-15", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2013-09-15", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
        ruling("2013-09-15", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
