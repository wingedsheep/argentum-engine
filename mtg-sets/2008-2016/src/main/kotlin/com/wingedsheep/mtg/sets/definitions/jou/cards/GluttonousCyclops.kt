package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gluttonous Cyclops
 * {5}{R}
 * Creature — Cyclops
 * 5/4
 * {5}{R}{R}: Monstrosity 3. (If this creature isn't monstrous, put three +1/+1 counters on it and it becomes monstrous.)
 */
val GluttonousCyclops = card("Gluttonous Cyclops") {
    manaCost = "{5}{R}"
    typeLine = "Creature — Cyclops"
    power = 5
    toughness = 4
    oracleText = "{5}{R}{R}: Monstrosity 3. (If this creature isn't monstrous, put three +1/+1 counters on it and it becomes monstrous.)"

    activatedAbility {
        cost = Costs.Mana("{5}{R}{R}")
        effect = Effects.Monstrosity(3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "99"
        artist = "Steve Prescott"
        flavorText = "The cyclops had learned to never eat a shepherd. Instead he gently flung the \"pit\" aside to grow a new flock."
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e583f6be-3d78-4085-9059-c3c989a4977f.jpg?1783939424"
        ruling("2014-04-26", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2014-04-26", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
        ruling("2014-04-26", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
