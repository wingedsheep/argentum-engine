package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceEquipCost
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Éowyn, Lady of Rohan
 * {2}{W}
 * Legendary Creature — Human Noble
 * 2/4
 *
 * At the beginning of combat on your turn, target creature gains your choice of first strike or
 * vigilance until end of turn. If that creature is equipped, it gains first strike and vigilance
 * until end of turn instead.
 * Equip abilities you activate cost {1} less to activate.
 *
 * The combat trigger composes existing primitives: a [ConditionalEffect] gated on
 * [Conditions.TargetMatchesFilter] for `GameObjectFilter.Creature.equipped()` — when the target is
 * equipped it grants both keywords; otherwise a [ModalEffect.chooseOne] lets the controller pick a
 * single keyword. The cost clause uses the new controller-scoped [ReduceEquipCost] static, which
 * the engine keys off `ActivatedAbility.isEquipAbility` to shave {1} off the generic equip cost.
 */
val EowynLadyOfRohan = card("Éowyn, Lady of Rohan") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Noble"
    power = 2
    toughness = 4
    oracleText = "At the beginning of combat on your turn, target creature gains your choice of first strike or vigilance until end of turn. If that creature is equipped, it gains first strike and vigilance until end of turn instead.\n" +
        "Equip abilities you activate cost {1} less to activate."

    triggeredAbility {
        trigger = Triggers.BeginCombat
        val creature = target("target creature", Targets.Creature)
        effect = ConditionalEffect(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.equipped(), targetIndex = 0),
            // Target is equipped: it gains first strike AND vigilance.
            effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature, Duration.EndOfTurn)
                .then(Effects.GrantKeyword(Keyword.VIGILANCE, creature, Duration.EndOfTurn)),
            // Otherwise: choose first strike OR vigilance.
            elseEffect = ModalEffect.chooseOne(
                Mode.noTarget(
                    Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature, Duration.EndOfTurn),
                    "First strike"
                ),
                Mode.noTarget(
                    Effects.GrantKeyword(Keyword.VIGILANCE, creature, Duration.EndOfTurn),
                    "Vigilance"
                )
            )
        )
    }

    // "Equip abilities you activate cost {1} less to activate."
    staticAbility {
        ability = ReduceEquipCost(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "Sean Vo"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e59710c4-24de-419e-a8a0-e8392d450c23.jpg?1686967724"
    }
}
