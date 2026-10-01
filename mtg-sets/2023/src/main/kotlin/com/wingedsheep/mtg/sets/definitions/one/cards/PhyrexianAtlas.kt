package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Phyrexian Atlas
 * {3}
 * Artifact
 * {T}: Add one mana of any color.
 * Corrupted — Whenever this artifact becomes tapped, each opponent who has three or more poison
 * counters loses 1 life.
 *
 * "Each opponent who …" is a per-opponent test, spelled as in Feed the Infection:
 * `ForEachPlayer(EachOpponent)` rebinds `Player.You` to the visited opponent, so
 * [Conditions.PoisonCountersAtLeast] reads that opponent's poison and the loss lands on them.
 */
val PhyrexianAtlas = card("Phyrexian Atlas") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "{T}: Add one mana of any color.\n" +
        "Corrupted — Whenever this artifact becomes tapped, each opponent who has three or more " +
        "poison counters loses 1 life."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.If(
                condition = Conditions.PoisonCountersAtLeast(3),
                then = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.You)),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "237"
        artist = "Septian Fajrianto"
        flavorText = "Since conquering Mirrodin, Phyrexian engineers have worked tirelessly to " +
            "transform the plane's original two layers into a more fitting array of nine."
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5adb509-6ad3-4838-925d-1cafa926b83a.jpg?1783917988"
    }
}
