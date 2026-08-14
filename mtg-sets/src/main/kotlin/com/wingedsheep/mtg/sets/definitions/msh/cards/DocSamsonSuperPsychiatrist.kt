package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Doc Samson, Super Psychiatrist — Marvel Super Heroes #164
 * {4}{G} · Legendary Creature — Gamma Doctor Hero · Uncommon
 * 3/6
 *
 * If you would put one or more counters on a permanent you control, put that many plus one of
 * each of those kinds of counters on that permanent instead.
 * {T}: Add X mana of any one color, where X is Doc Samson's power.
 *
 * The counter clause is Hardened Scales' [ModifyCounterPlacement] widened on both axes: the
 * counter kind is [CounterTypeFilter.Any] ("one or more counters", not just +1/+1 — and the
 * `+1` applies per kind, which is what "one of each of those kinds" means) and the recipient is
 * [RecipientFilter.PermanentYouControl] ("a permanent you control", not merely a creature).
 * Modelling it as the placement *replacement* rather than a trigger is what makes the printed
 * interactions fall out: a permanent entering with counters enters with one extra of each kind,
 * two Doc Samsons stack, and the controller orders it against other placement replacements per
 * CR 616.1.
 *
 * **Fidelity note.** [ModifyCounterPlacement] has no `placedByYou` flag, so the printed
 * "**If you would put** …" is modelled as the Winding Constrictor "if counters would be put"
 * wording. The difference only shows when an *opponent's* effect puts counters on a permanent
 * you control (e.g. an opposing fight/bolster-style effect, or an opponent's proliferate): the
 * printed card does not add the extra counter there, this model does.
 *
 * "Add X mana of any one color" is [AddManaOfChoiceEffect] over [ManaColorSet.AnyColor] with a
 * dynamic amount — the player picks one color and gets that many of it (as opposed to
 * `AddDynamicManaEffect`, which splits a total across colors). X reads the source's *projected*
 * power at resolution via [DynamicAmounts.sourcePower], so counters and lords are included and a
 * 0-power Doc Samson produces no mana.
 */
val DocSamsonSuperPsychiatrist = card("Doc Samson, Super Psychiatrist") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Gamma Doctor Hero"
    power = 3
    toughness = 6
    oracleText = "If you would put one or more counters on a permanent you control, put that many " +
        "plus one of each of those kinds of counters on that permanent instead.\n" +
        "{T}: Add X mana of any one color, where X is Doc Samson's power."

    replacementEffect(
        ModifyCounterPlacement(
            modifier = 1,
            appliesTo = EventPattern.CounterPlacementEvent(
                counterType = CounterTypeFilter.Any,
                recipient = RecipientFilter.PermanentYouControl
            ),
            // "If **you** would put ..." — the placer matters, not just the recipient. Without
            // this the card reads like Winding Constrictor ("if counters would be put"), and an
            // opponent proliferating or otherwise adding counters to your permanent would feed
            // Doc Samson too.
            placedByYou = true
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = AddManaOfChoiceEffect(
            colorSet = ManaColorSet.AnyColor,
            amount = DynamicAmounts.sourcePower()
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add X mana of any one color, where X is Doc Samson's power."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "164"
        artist = "Ryan Pancoast"
        flavorText = "\"Go ahead, Bruce. Let out your feelings.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19dc5fcc-d05d-41a0-84c5-2dec996f3e4f.jpg?1783902920"
    }
}
