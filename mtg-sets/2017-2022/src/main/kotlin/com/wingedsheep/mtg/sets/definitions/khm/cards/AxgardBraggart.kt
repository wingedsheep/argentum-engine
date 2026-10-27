package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Axgard Braggart — Kaldheim #1 (canonical printing)
 * {3}{W} · Creature — Dwarf Warrior · 3/3
 *
 * Boast — {1}{W}: Untap this creature. Put a +1/+1 counter on it.
 *
 * The untap and the counter are independent: the boast can be activated while already untapped
 * and still grows the creature (per the ruling).
 */
val AxgardBraggart = card("Axgard Braggart") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Warrior"
    power = 3
    toughness = 3
    oracleText = "Boast — {1}{W}: Untap this creature. Put a +1/+1 counter on it. (Activate only if " +
        "this creature attacked this turn and only once each turn.)"

    activatedAbility {
        isBoast = true
        cost = Costs.Mana("{1}{W}")
        effect = Effects.Untap(EffectTarget.Self) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "1"
        artist = "Svetlin Velinov"
        flavorText = "\"Heavy? Ha! The Trollking of Mossrun was heavy, and I tossed him five miles!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4de5ff64-6fe7-4fc5-be27-cdbaa14545ab.jpg?1783928290"
        ruling("2021-02-05", "You can activate Axgard Braggart's boast ability even if Axgard Braggart is already untapped. You'll still put a +1/+1 counter on it.")
        ruling("2021-02-05", "A boast ability can be activated at any point after the creature with that ability has been declared as an attacker. This can be before blockers are declared, after blockers are declared but before combat damage is dealt, during combat after combat damage is dealt, during the postcombat main phase, during the end step, or, in some unusual cases, during the cleanup step.")
        ruling("2021-02-05", "If a creature with a boast ability is put onto the battlefield attacking, it was never declared as an attacker. Its boast ability can't be activated that turn.")
    }
}
