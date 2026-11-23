package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bazaar Krovod — Return to Ravnica #7
 * {4}{W} · Creature — Beast · Uncommon · 2/5
 *
 * Whenever this creature attacks, another target attacking creature gets +0/+2 until end of turn.
 * Untap that creature.
 */
val BazaarKrovod = card("Bazaar Krovod") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Beast"
    power = 2
    toughness = 5
    oracleText = "Whenever this creature attacks, another target attacking creature gets +0/+2 until end of turn. " +
        "Untap that creature."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.AttackingCreature.other())
        effect = Effects.ModifyStats(0, 2, creature) then Effects.Untap(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Lars Grant-West"
        flavorText = "The Hauler's Collective wields great influence over the Ravnican merchant class. " +
            "Without it, little moves in the city."
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b07bb2fe-3a9b-47d0-864b-99a662d9544b.jpg?1783940377"
    }
}
