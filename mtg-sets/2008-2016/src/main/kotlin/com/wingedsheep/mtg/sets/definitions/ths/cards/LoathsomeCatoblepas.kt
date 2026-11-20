package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Loathsome Catoblepas
 * {5}{B}
 * Creature — Beast
 * 3/3
 *
 * {2}{G}: This creature must be blocked this turn if able.
 * When this creature dies, target creature an opponent controls gets -3/-3 until end of turn.
 *
 * "Must be blocked if able" asks for at least one blocker, not every able blocker, so
 * [Effects.MustBeBlocked] runs with `allCreatures = false` (Gaea's Protector's shape).
 */
val LoathsomeCatoblepas = card("Loathsome Catoblepas") {
    manaCost = "{5}{B}"
    colorIdentity = "BG"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 3
    oracleText = "{2}{G}: This creature must be blocked this turn if able.\n" +
        "When this creature dies, target creature an opponent controls gets -3/-3 until end of turn."

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        effect = Effects.MustBeBlocked(EffectTarget.Self, allCreatures = false)
        description = "This creature must be blocked this turn if able."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val victim = target(TargetFilter.Creature.opponentControls())
        effect = Effects.ModifyStats(-3, -3, victim, Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "Christopher Burdett"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a8cff2f-ba52-4d22-83e8-13c56368f1df.jpg?1783939775"
        ruling(
            "2013-09-15",
            "If Loathsome Catoblepas is attacking and its activated ability has resolved, the defending " +
                "player must assign at least one blocker to it during the declare blockers step if that " +
                "player controls any creatures that could block it."
        )
        ruling(
            "2013-09-15",
            "The activated ability doesn't target any creature or make any specific creature have to " +
                "block. Activating it more than once in a turn provides no additional benefit."
        )
    }
}
