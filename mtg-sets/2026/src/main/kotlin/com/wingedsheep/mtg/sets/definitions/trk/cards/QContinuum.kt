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
 * Q Continuum
 * Land
 *
 * This land enters tapped unless you control two or more other lands.
 * When this land enters untapped, choose one —
 * • Target creature gets +1/+1 and gains vigilance until end of turn.
 * • Surveil 1.
 * {T}: Add {G}.
 *
 * The green member of the Ni'Var Science Academy cycle: fast-land replacement plus the
 * `untapped()` self enters trigger, modal so the mode and its target are chosen as it goes on
 * the stack.
 */
val QContinuum = card("Q Continuum") {
    manaCost = ""
    colorIdentity = "G"
    typeLine = "Land"
    oracleText = "This land enters tapped unless you control two or more other lands.\n" +
        "When this land enters untapped, choose one —\n" +
        "• Target creature gets +1/+1 and gains vigilance until end of turn.\n" +
        "• Surveil 1.\n" +
        "{T}: Add {G}."

    replacementEffect(EntersTapped(unlessCondition = Conditions.YouControlOtherAtLeast(2, GameObjectFilter.Land)))

    triggeredAbility {
        trigger = Triggers.self.matching(GameObjectFilter.Any.untapped()).enters()
        effect = ModalEffect.chooseOne(
            mode("Target creature gets +1/+1 and gains vigilance until end of turn") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(1, 1, creature) then
                    Effects.GrantKeyword(Keyword.VIGILANCE, creature)
            },
            Mode.noTarget(Patterns.Library.surveil(1), "Surveil 1"),
        )
        description = "When this land enters untapped, choose one — " +
            "target creature gets +1/+1 and gains vigilance until end of turn; or surveil 1."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "290"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd424080-e8ad-4a56-a820-20d8534fba31.jpg?1793447127"
    }
}
