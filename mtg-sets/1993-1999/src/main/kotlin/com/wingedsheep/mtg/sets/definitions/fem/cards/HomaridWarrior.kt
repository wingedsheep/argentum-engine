package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Homarid Warrior
 * {4}{U}
 * Creature — Homarid Warrior
 * 3/3
 * {U}: This creature gains shroud until end of turn and doesn't untap during your next untap step.
 * Tap it.
 *
 * All three parts resolve together, so the creature ends up tapped, untargetable, and frozen
 * through its controller's next untap step. The same ability [DeepSpawn] carries.
 */
val HomaridWarrior = card("Homarid Warrior") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Homarid Warrior"
    oracleText = "{U}: This creature gains shroud until end of turn and doesn't untap during " +
        "your next untap step. Tap it. (A creature with shroud can't be the target of spells or abilities.)"
    power = 3
    toughness = 3

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.Composite(
            Effects.GrantKeyword(Keyword.SHROUD, EffectTarget.Self),
            GrantKeywordEffect(
                AbilityFlag.DOESNT_UNTAP.name,
                EffectTarget.Self,
                Duration.UntilAfterAffectedControllersNextUntap,
            ),
            Effects.Tap(EffectTarget.Self),
        )
        description = "{U}: This creature gains shroud until end of turn and doesn't untap during your next untap step. Tap it."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22a"
        artist = "Daniel Gelon"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/627ca588-917f-4768-a69d-3d93c1210390.jpg?1783947911"
    }
}
