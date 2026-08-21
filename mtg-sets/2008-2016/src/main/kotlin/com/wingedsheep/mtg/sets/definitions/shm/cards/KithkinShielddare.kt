package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Kithkin Shielddare
 * {1}{W}
 * Creature — Kithkin Soldier
 * 1 / 1
 *
 * {W}, {T}: Target blocking creature gets +2/+2 until end of turn.
 *
 * - "Target blocking creature" is [Targets.BlockingCreature], whose filter is
 *   `GameObjectFilter.Creature.blocking()` — a creature-typed predicate plus the *state* predicate
 *   "is blocking", read off projected state. It is not restricted to creatures you control, so it
 *   can pump an opponent's blocker.
 * - The cost is a composite of the mana symbol and the tap symbol, in printed order.
 */
val KithkinShielddare = card("Kithkin Shielddare") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Kithkin Soldier"
    power = 1
    toughness = 1
    oracleText = "{W}, {T}: Target blocking creature gets +2/+2 until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        val blocker = target("target", Targets.BlockingCreature)
        effect = Effects.ModifyStats(2, 2, blocker)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "Christopher Moeller"
        flavorText = "The nova glyph is a potent symbol. A shield embossed with it can resist the force of even the most determined giant."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e5102a7-2147-4e22-b683-c08b4a725617.jpg?1783942768"
    }
}
