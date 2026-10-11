package com.wingedsheep.mtg.sets.definitions.tle.cards

import com.wingedsheep.sdk.core.BendType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zuko, Firebending Master
 * {1}{R}
 * Legendary Creature — Human Noble Ally
 * 2/2
 *
 * First strike
 * Firebending X, where X is the number of experience counters you have. (Whenever this creature
 * attacks, add X {R}. This mana lasts until end of combat.)
 * Whenever you cast a spell during combat, you get an experience counter.
 *
 * - Dynamic firebending: the fixed `firebending(n)` DSL only takes a constant, so the attack
 *   trigger is hand-wired like Fire Lord Zuko / Firebending Student — red mana equal to your
 *   experience counters with [ManaExpiry.END_OF_COMBAT], counted on resolution — plus the
 *   fire-bend notification the printed keyword emits, so "whenever you firebend" payoffs see it.
 * - Experience counters sit on the player ([CounterType.EXPERIENCE]); they outlast Zuko and are
 *   shared with every other source (the rulings).
 * - "During combat" is the combat phase on anyone's turn — a trigger-time restriction on the cast
 *   trigger (`IsInPhase(COMBAT, yoursOnly = false)`), the Glen Elendra Pranksters shape.
 */
val ZukoFirebendingMaster = card("Zuko, Firebending Master") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Noble Ally"
    power = 2
    toughness = 2
    oracleText = "First strike\n" +
        "Firebending X, where X is the number of experience counters you have. (Whenever this creature " +
        "attacks, add X {R}. This mana lasts until end of combat.)\n" +
        "Whenever you cast a spell during combat, you get an experience counter."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.AddMana(
            Color.RED,
            DynamicAmounts.playerCounterCount(CounterType.EXPERIENCE),
            expiry = ManaExpiry.END_OF_COMBAT,
        ) then Effects.EmitBend(BendType.FIRE)
        description = "Firebending X, where X is the number of experience counters you have. " +
            "Whenever this creature attacks, add X {R}. This mana lasts until end of combat."
    }

    triggeredAbility {
        trigger = Triggers.you.casts()
        triggerRestriction = Conditions.IsInPhase(Phase.COMBAT, yoursOnly = false)
        effect = Effects.AddCounters(CounterType.EXPERIENCE, 1, EffectTarget.Controller)
        description = "Whenever you cast a spell during combat, you get an experience counter."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "127"
        artist = "Kinota Eishi"
        flavorText = "\"I have changed.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/9/496a4fa5-b3e5-4020-b432-6d353095aa33.jpg?1783904817"
        ruling("2025-10-02", "The experience counter goes on you, the player, not on Zuko. You will keep that counter even if Zuko, Firebending Master dies.")
        ruling("2025-10-02", "Firebending abilities aren't mana abilities. They use the stack and can be responded to.")
        ruling("2025-10-02", "Mana from firebending abilities isn't lost until you leave combat and go to your second main phase. It can be used at any time during combat, even after combat damage has been dealt.")
        ruling("2025-10-02", "All experience counters are identical, no matter how you got them. For example, the firebending ability will count experience counters that you got from the last ability, from another ability, from another copy of Zuko, Firebending Master, and so on.")
    }
}
