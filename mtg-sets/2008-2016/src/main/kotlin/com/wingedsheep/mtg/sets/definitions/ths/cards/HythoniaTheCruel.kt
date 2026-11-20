package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Hythonia the Cruel — Theros #91
 * {4}{B}{B} · Legendary Creature — Gorgon · 4/6
 *
 * Deathtouch
 * {6}{B}{B}: Monstrosity 3.
 * When Hythonia becomes monstrous, destroy all non-Gorgon creatures.
 *
 * The wipe reads projected subtypes, so Hythonia (a Gorgon) survives unless something has
 * stripped her creature types.
 */
val HythoniaTheCruel = card("Hythonia the Cruel") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Gorgon"
    power = 4
    toughness = 6
    oracleText = "Deathtouch\n" +
        "{6}{B}{B}: Monstrosity 3. (If this creature isn't monstrous, put three +1/+1 counters on it and it becomes monstrous.)\n" +
        "When Hythonia becomes monstrous, destroy all non-Gorgon creatures."

    keywords(Keyword.DEATHTOUCH)

    activatedAbility {
        cost = Costs.Mana("{6}{B}{B}")
        effect = Effects.Monstrosity(3)
    }

    triggeredAbility {
        trigger = Triggers.self.becomesMonstrous()
        effect = Effects.DestroyAll(GameObjectFilter.Creature.notSubtype(Subtype.GORGON))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "91"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9fbc814-4281-4ac6-a844-68bcd848f229.jpg?1783939778"
        ruling("2013-09-15", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2013-09-15", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
        ruling("2013-09-15", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
