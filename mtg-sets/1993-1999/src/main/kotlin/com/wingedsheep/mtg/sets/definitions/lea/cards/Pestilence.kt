package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pestilence
 * {2}{B}{B}
 * Enchantment
 * At the beginning of the end step, if no creatures are on the battlefield, sacrifice this enchantment.
 * {B}: This enchantment deals 1 damage to each creature and each player.
 *
 * Modeling notes:
 *  - "At the beginning of the end step" fires in every player's end step, so
 *    `Triggers.anyPlayer.beginningOf(Step.END)` (as Old Flitterfang).
 *  - The "if no creatures are on the battlefield" clause is an intervening-if (CR 603.4): it is
 *    checked as the end step begins and again on resolution, so it rides on `interveningIf` with
 *    the global [Conditions.NoCreaturesOnBattlefield] (Drop of Honey's condition). A creature that
 *    dies later during the end step does not retroactively make it trigger (2011-06-01 ruling).
 *  - The activated ability is Pestilence Demon's line verbatim: a group pass over every creature
 *    plus a per-player pass, no targets.
 */
val Pestilence = card("Pestilence") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "At the beginning of the end step, if no creatures are on the battlefield, " +
        "sacrifice this enchantment.\n" +
        "{B}: This enchantment deals 1 damage to each creature and each player."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.NoCreaturesOnBattlefield
        effect = Effects.SacrificeTarget(EffectTarget.Self)
        description = "At the beginning of the end step, if no creatures are on the battlefield, " +
            "sacrifice this enchantment."
    }

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature),
            Effects.DealDamage(1, EffectTarget.IterationEntity)
        ) then
            Effects.ForEachPlayer(
                Player.Each,
                listOf(Effects.DealDamage(1, EffectTarget.Controller))
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Jesper Myrfors"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d42a6350-b16b-4e10-a273-e6cbb55dcb7a.jpg?1783948692"

        ruling(
            "2011-06-01",
            "Note that \"until end of turn\" effects wear off after \"at the beginning of the end step\" " +
                "triggered abilities, so an artifact that animates until end of turn can keep this on the battlefield."
        )
        ruling(
            "2011-06-01",
            "It will stay on the battlefield if there is a creature that is put into the graveyard during the " +
                "end step. This is because this ability will not trigger at all if there is at least one creature " +
                "on the battlefield as the end step begins."
        )
        ruling(
            "2004-10-04",
            "Each activation is considered a new damage effect. An activation can only be 1 point of damage."
        )
    }
}
