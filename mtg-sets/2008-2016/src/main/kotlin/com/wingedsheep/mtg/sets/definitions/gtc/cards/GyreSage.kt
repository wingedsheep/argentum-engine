package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evolve
import com.wingedsheep.sdk.model.Rarity

/**
 * Gyre Sage — Gatecrash #123
 * {1}{G} · Creature — Elf Druid · 1/2
 *
 * Evolve
 * {T}: Add {G} for each +1/+1 counter on this creature.
 *
 * The mana ability reads the +1/+1 counters on the Sage as it resolves; with none it adds nothing.
 */
val GyreSage = card("Gyre Sage") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 1
    toughness = 2
    oracleText = "Evolve (Whenever a creature you control enters, if that creature has greater power or " +
        "toughness than this creature, put a +1/+1 counter on this creature.)\n" +
        "{T}: Add {G} for each +1/+1 counter on this creature."

    evolve()

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddMana(Color.GREEN, DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE))
        description = "{T}: Add {G} for each +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "123"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/6345376a-3d2d-4fff-9430-5e90a96e2f0f.jpg?1783940117"
        ruling(
            "2013-04-15",
            "If evolve triggers, the stat comparison will happen again when the ability tries to resolve. If " +
                "neither stat of the new creature is greater, the ability will do nothing. If the creature that " +
                "entered the battlefield leaves the battlefield before evolve tries to resolve, use its last known " +
                "power and toughness to compare the stats."
        )
        ruling("2013-01-24", "Gyre Sage's last ability is a mana ability. It doesn't use the stack and can't be responded to.")
    }
}
