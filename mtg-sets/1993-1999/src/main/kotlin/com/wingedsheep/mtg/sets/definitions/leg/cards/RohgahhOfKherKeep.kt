package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rohgahh of Kher Keep
 * {2}{B}{B}{R}{R}
 * Legendary Creature — Kobold
 * 5/5
 * At the beginning of your upkeep, you may pay {R}{R}{R}. If you don't, tap Rohgahh and all
 * creatures named Kobolds of Kher Keep, then an opponent gains control of them.
 * Creatures you control named Kobolds of Kher Keep get +2/+2.
 *
 * The upkeep tax is the Demonic Hordes shape ([Effects.PayOrSuffer] over a mana cost). Declining
 * taps Rohgahh and *every* creature named Kobolds of Kher Keep — whoever controls it (ruling) —
 * then hands all of them to one opponent. That opponent isn't targeted: the controller picks as
 * the ability resolves, so [Effects.ChooseOpponent] records the pick and each [Effects.GiveControl]
 * reads it back through [Player.ChosenOpponent] (the Fateful Handoff shape); in two-player games
 * the pick is forced. The control change has no duration — it lasts indefinitely.
 *
 * The anthem is a plain name-filtered [ModifyStats] over Kobolds of Kher Keep the controller has.
 */
val RohgahhOfKherKeep = card("Rohgahh of Kher Keep") {
    manaCost = "{2}{B}{B}{R}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Kobold"
    power = 5
    toughness = 5
    oracleText = "At the beginning of your upkeep, you may pay {R}{R}{R}. If you don't, tap Rohgahh " +
        "and all creatures named Kobolds of Kher Keep, then an opponent gains control of them.\n" +
        "Creatures you control named Kobolds of Kher Keep get +2/+2."

    val kobolds = GroupFilter(GameObjectFilter.Creature.named("Kobolds of Kher Keep"))
    val newController = EffectTarget.PlayerRef(Player.ChosenOpponent)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Mana("{R}{R}{R}"),
            suffer = Effects.Tap(EffectTarget.Self) then
                Effects.ForEachInGroup(kobolds, Effects.Tap(EffectTarget.IterationEntity)) then
                Effects.ChooseOpponent("Choose an opponent to gain control of Rohgahh and the Kobolds") then
                Effects.GiveControl(permanent = EffectTarget.Self, newController = newController) then
                Effects.ForEachInGroup(
                    kobolds,
                    Effects.GiveControl(permanent = EffectTarget.IterationEntity, newController = newController)
                ),
            consequenceDescription = "tap Rohgahh and all creatures named Kobolds of Kher Keep, " +
                "then an opponent gains control of them"
        )
    }

    staticAbility {
        ability = ModifyStats(
            2, 2,
            GroupFilter(GameObjectFilter.Creature.named("Kobolds of Kher Keep").youControl())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "255"
        artist = "Edward P. Beard, Jr."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea0891f0-83ce-4eb7-b0a9-cbc8168bafff.jpg?1783948033"
        ruling(
            "2009-10-01",
            "Rohgahh's first ability isn't targeted. You don't choose the opponent that will gain " +
                "control of the creatures until the ability resolves."
        )
        ruling(
            "2009-10-01",
            "If you don't pay {R}{R}{R} as the first ability resolves, all creatures named Kobolds of " +
                "Kher Keep become tapped (even the ones you don't control), as does Rohgahh. The chosen " +
                "opponent gains control of Rohgahh and all creatures named Kobolds of Kher Keep " +
                "regardless of whether you control them or whether they were tapped by the ability."
        )
    }
}
