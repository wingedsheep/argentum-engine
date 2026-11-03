package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ni'Var Science Academy
 * Land
 *
 * This land enters tapped unless you control two or more other lands.
 * When this land enters untapped, choose one —
 * • Target creature gains flying until end of turn.
 * • Surveil 1.
 * {T}: Add {U}.
 *
 * The fast-land replacement of Shipwreck Marsh plus the "enters untapped" trigger of Dwarven Mine:
 * `untapped()` on the self enters trigger reads the land's tap state *as it entered*, so tapping it
 * for mana in response doesn't stop the trigger. The trigger is modal (CR 603.3c), so the mode and
 * the flying mode's target are chosen as it goes on the stack.
 */
val NiVarScienceAcademy = card("Ni'Var Science Academy") {
    manaCost = ""
    colorIdentity = "U"
    typeLine = "Land"
    oracleText = "This land enters tapped unless you control two or more other lands.\n" +
        "When this land enters untapped, choose one —\n" +
        "• Target creature gains flying until end of turn.\n" +
        "• Surveil 1.\n" +
        "{T}: Add {U}."

    replacementEffect(EntersTapped(unlessCondition = Conditions.YouControlOtherAtLeast(2, GameObjectFilter.Land)))

    triggeredAbility {
        trigger = Triggers.self.matching(GameObjectFilter.Any.untapped()).enters()
        effect = ModalEffect.chooseOne(
            mode("Target creature gains flying until end of turn") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.GrantKeyword(Keyword.FLYING, creature)
            },
            Mode.noTarget(Patterns.Library.surveil(1), "Surveil 1"),
        )
        description = "When this land enters untapped, choose one — " +
            "target creature gains flying until end of turn; or surveil 1."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "287"
        artist = "Raphael Lacoste"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a785dcd0-2df5-4d31-a01a-61fa7a1f1806.jpg?1793273928"
    }
}
