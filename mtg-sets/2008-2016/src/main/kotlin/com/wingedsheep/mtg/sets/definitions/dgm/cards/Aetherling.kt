package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aetherling
 * {4}{U}{U}
 * Creature — Shapeshifter
 * 4/5
 * {U}: Exile this creature. Return it to the battlefield under its owner's control at the beginning of the next end step.
 * {U}: This creature can't be blocked this turn.
 * {1}: This creature gets +1/-1 until end of turn.
 * {1}: This creature gets -1/+1 until end of turn.
 */
val Aetherling = card("Aetherling") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shapeshifter"
    power = 4
    toughness = 5
    oracleText = "{U}: Exile this creature. Return it to the battlefield under its owner's control at the beginning of the next end step.\n" +
        "{U}: This creature can't be blocked this turn.\n" +
        "{1}: This creature gets +1/-1 until end of turn.\n" +
        "{1}: This creature gets -1/+1 until end of turn."

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Patterns.Exile.exileUntilEndStep(EffectTarget.Self)
        description = "Exile Aetherling. Return it to the battlefield at the beginning of the next end step."
    }

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
        description = "Aetherling can't be blocked this turn."
    }

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.ModifyStats(1, -1, EffectTarget.Self)
        description = "Aetherling gets +1/-1 until end of turn."
    }

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.ModifyStats(-1, 1, EffectTarget.Self)
        description = "Aetherling gets -1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c93313b-cf43-47e9-a911-717b4d14b0b5.jpg?1783940042"
        ruling(
            "2013-04-15",
            "Aetherling's first ability will return it to the battlefield only if that ability also exiled it. " +
                "If Aetherling left the battlefield in response to that ability, it won't return, even if it was " +
                "exiled by another spell or ability."
        )
        ruling(
            "2013-04-15",
            "Aetherling's last ability can be activated even if its power is 0 or less. If a creature would " +
                "assign 0 or less damage in combat, it doesn't assign combat damage at all."
        )
    }
}
