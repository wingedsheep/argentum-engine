package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustBeBlocked
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Raphael, Ninja Destroyer
 * {2}{R}{R}
 * Legendary Creature — Mutant Ninja Turtle
 * 4/4
 *
 * Raphael must be blocked if able.
 * Enrage — Whenever Raphael is dealt damage, add that much {R}. Until end of turn,
 * you don't lose this mana as steps and phases end.
 *
 * The "don't lose this mana as steps and phases end" clause is the engine default —
 * mana pools only empty at end of turn here (see ManaExpiry) — so the Enrage payout is
 * a plain `AddMana` of the damage dealt.
 */
val RaphaelNinjaDestroyer = card("Raphael, Ninja Destroyer") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Mutant Ninja Turtle"
    oracleText = "Raphael must be blocked if able.\nEnrage — Whenever Raphael is dealt damage, add that much {R}. Until end of turn, you don't lose this mana as steps and phases end."
    power = 4
    toughness = 4

    staticAbility {
        ability = MustBeBlocked(allCreatures = false)
    }

    triggeredAbility {
        trigger = Triggers.TakesDamage
        effect = Effects.AddMana(
            Color.RED,
            DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT)
        )
        description = "Enrage — Whenever Raphael is dealt damage, add that much {R}. Until end of turn, you don't lose this mana as steps and phases end."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "102"
        artist = "Fajareka Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eeffadda-cb71-4434-89bf-36db1a36da0b.jpg?1769006156"
    }
}
