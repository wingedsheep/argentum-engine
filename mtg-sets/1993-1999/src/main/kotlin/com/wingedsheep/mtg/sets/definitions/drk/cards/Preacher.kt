package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetChooser
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Preacher
 * {1}{W}{W}
 * Creature — Human Cleric
 * 1/1
 * You may choose not to untap this creature during your untap step.
 * {T}: For as long as this creature remains tapped, gain control of target creature of an
 * opponent's choice they control.
 *
 * Every clause of this card is load-bearing on a different piece:
 *
 * - The optional untap is `AbilityFlag.MAY_NOT_UNTAP` — not `DOESNT_UNTAP`, which is mandatory.
 *   Staying tapped is what keeps the stolen creature, so the choice has to be the controller's.
 * - The theft is bounded by [Duration.WhileSourceTapped]: control reverts the moment the Preacher
 *   untaps (or leaves), which is exactly what makes the untap clause matter rather than being flavour.
 * - The *opponent* picks the creature, and picks from their own — `TargetChooser.Opponent` over a
 *   "creature an opponent controls" filter. Target legality is still measured relative to the
 *   ability's controller (CR 601.2c and the Cuombajj Witches rulings), so an opponent can't hand over
 *   something with hexproof to blank the ability.
 *
 * In a multiplayer game the chooser is one opponent and the filter is "not mine", so a chooser could
 * in principle offer up a *third* player's creature; the printed "they control" narrows it further
 * than the engine's controller-relative filter can express today.
 */
val Preacher = card("Preacher") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "You may choose not to untap this creature during your untap step.\n" +
        "{T}: For as long as this creature remains tapped, gain control of target creature of an " +
        "opponent's choice they control."

    flags(AbilityFlag.MAY_NOT_UNTAP)

    activatedAbility {
        cost = Costs.Tap
        target = TargetCreature(
            filter = TargetFilter.CreatureOpponentControls,
            chooser = TargetChooser.Opponent,
        )
        effect = Effects.GainControl(
            EffectTarget.ContextTarget(0),
            Duration.WhileSourceTapped("this creature"),
        )
        description = "{T}: For as long as this creature remains tapped, gain control of target " +
            "creature of an opponent's choice they control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "16"
        artist = "Quinton Hoover"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e03d335-d259-4ab4-814f-9333cfd3afc9.jpg?1783947946"
    }
}
