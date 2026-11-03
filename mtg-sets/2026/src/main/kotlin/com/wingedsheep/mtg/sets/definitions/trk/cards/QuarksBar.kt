package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Quark's Bar
 * Land
 *
 * This land enters tapped unless you control two or more other lands.
 * When this land enters untapped, choose one —
 * • This land deals 1 damage to each opponent.
 * • Surveil 1.
 * {T}: Add {R}.
 *
 * The red member of the Ni'Var Science Academy cycle: fast-land replacement plus the
 * `untapped()` self enters trigger, modal so the mode is chosen as it goes on the stack.
 * Neither mode targets.
 */
val QuarksBar = card("Quark's Bar") {
    manaCost = ""
    colorIdentity = "R"
    typeLine = "Land"
    oracleText = "This land enters tapped unless you control two or more other lands.\n" +
        "When this land enters untapped, choose one —\n" +
        "• This land deals 1 damage to each opponent.\n" +
        "• Surveil 1.\n" +
        "{T}: Add {R}."

    replacementEffect(EntersTapped(unlessCondition = Conditions.YouControlOtherAtLeast(2, GameObjectFilter.Land)))

    triggeredAbility {
        trigger = Triggers.self.matching(GameObjectFilter.Any.untapped()).enters()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)),
                "This land deals 1 damage to each opponent",
            ),
            Mode.noTarget(Patterns.Library.surveil(1), "Surveil 1"),
        )
        description = "When this land enters untapped, choose one — " +
            "this land deals 1 damage to each opponent; or surveil 1."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "292"
        artist = "Robin Olausson"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/8422ee4a-9845-429d-bda8-74852168dcba.jpg?1792632965"
    }
}
