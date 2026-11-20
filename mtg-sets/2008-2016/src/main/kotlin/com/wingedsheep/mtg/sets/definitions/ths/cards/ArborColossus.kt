package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Arbor Colossus — Theros #150
 * {2}{G}{G}{G} · Creature — Giant · 6/6
 *
 * Reach
 * {3}{G}{G}{G}: Monstrosity 3.
 * When this creature becomes monstrous, destroy target creature with flying an opponent controls.
 */
val ArborColossus = card("Arbor Colossus") {
    manaCost = "{2}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Giant"
    power = 6
    toughness = 6
    oracleText = "Reach\n" +
        "{3}{G}{G}{G}: Monstrosity 3. (If this creature isn't monstrous, put three +1/+1 counters on it and it becomes monstrous.)\n" +
        "When this creature becomes monstrous, destroy target creature with flying an opponent controls."

    keywords(Keyword.REACH)

    activatedAbility {
        cost = Costs.Mana("{3}{G}{G}{G}")
        effect = Effects.Monstrosity(3)
    }

    triggeredAbility {
        trigger = Triggers.self.becomesMonstrous()
        val creature = target(TargetFilter.Creature.withKeyword(Keyword.FLYING).opponentControls())
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "150"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfbd7c65-ebe5-47a6-8d2f-9a8c63a30a7b.jpg?1783939749"
        ruling("2013-09-15", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2013-09-15", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
        ruling("2013-09-15", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
