package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.scripting.AdditionalSourceTriggers
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lwaxana Troi, Charming Guest
 * {1}{R}
 * Legendary Creature — Betazoid Advisor
 * 2/2
 *
 * You may choose not to untap Lwaxana Troi during your untap step.
 * {2}, {T}: Choose another target creature you control. For as long as Lwaxana Troi remains
 * tapped, if a triggered ability of that creature triggers, that ability triggers an additional
 * time.
 *
 * The chosen creature gains an [AdditionalSourceTriggers] doubler scoped to itself
 * (`sourceItself()`, read relative to the holder), bounded by [Duration.WhileSourceTapped] on
 * Lwaxana: untapping her — or her leaving — ends it. `MAY_NOT_UNTAP` keeps it going turn after turn.
 */
val LwaxanaTroiCharmingGuest = card("Lwaxana Troi, Charming Guest") {
    manaCost = "{1}{R}"
    typeLine = "Legendary Creature — Betazoid Advisor"
    colorIdentity = "R"
    oracleText = "You may choose not to untap Lwaxana Troi during your untap step.\n" +
        "{2}, {T}: Choose another target creature you control. For as long as Lwaxana Troi remains " +
        "tapped, if a triggered ability of that creature triggers, that ability triggers an additional time."
    power = 2
    toughness = 2

    flags(AbilityFlag.MAY_NOT_UNTAP)

    activatedAbility {
        val creature = target(TargetFilter.OtherCreatureYouControl)
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.GrantStaticAbility(
            AdditionalSourceTriggers(
                sourceFilter = GameObjectFilter.Any.sourceItself(),
                excludeSelf = false,
                description = "If a triggered ability of this creature triggers, that ability triggers an additional time",
            ),
            creature,
            Duration.WhileSourceTapped("Lwaxana Troi"),
        )
        description = "{2}, {T}: Choose another target creature you control. For as long as Lwaxana Troi " +
            "remains tapped, if a triggered ability of that creature triggers, that ability triggers an additional time."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "157"
        artist = "Marta Nael"
        flavorText = "\"Hello, Jean-Luc.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19bf2f2f-4be2-4958-bfab-eb46c9ca1995.jpg?1793013189"
    }
}
