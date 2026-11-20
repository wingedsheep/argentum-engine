package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Keepsake Gorgon — Theros #93
 * {3}{B}{B} · Creature — Gorgon · 2/5
 *
 * Deathtouch
 * {5}{B}{B}: Monstrosity 1.
 * When this creature becomes monstrous, destroy target non-Gorgon creature an opponent controls.
 */
val KeepsakeGorgon = card("Keepsake Gorgon") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Gorgon"
    power = 2
    toughness = 5
    oracleText = "Deathtouch\n" +
        "{5}{B}{B}: Monstrosity 1. (If this creature isn't monstrous, put a +1/+1 counter on it and it becomes monstrous.)\n" +
        "When this creature becomes monstrous, destroy target non-Gorgon creature an opponent controls."

    keywords(Keyword.DEATHTOUCH)

    activatedAbility {
        cost = Costs.Mana("{5}{B}{B}")
        effect = Effects.Monstrosity(1)
    }

    triggeredAbility {
        trigger = Triggers.self.becomesMonstrous()
        val creature = target(TargetFilter.Creature.opponentControls().notSubtype(Subtype.GORGON))
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "93"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac988f7b-9a10-4b48-8215-50376dc8678c.jpg?1783939777"
        ruling("2013-09-15", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2013-09-15", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
        ruling("2013-09-15", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
