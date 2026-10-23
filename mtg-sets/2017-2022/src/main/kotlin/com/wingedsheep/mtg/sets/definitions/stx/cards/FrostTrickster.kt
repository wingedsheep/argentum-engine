package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Frost Trickster — Strixhaven: School of Mages #43 (canonical printing)
 * {2}{U} · Creature — Bird Wizard 2/2
 *
 * Flying
 * When this creature enters, tap target creature an opponent controls. That creature doesn't
 * untap during its controller's next untap step.
 *
 * The Watertrap Weaver ETB: [Effects.Tap] then a DOESNT_UNTAP flag lasting until after the
 * affected controller's next untap step. An already-tapped creature is a legal target and still
 * skips that untap (ruling).
 */
val FrostTrickster = card("Frost Trickster") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird Wizard"
    power = 2
    toughness = 2
    oracleText = "Flying\nWhen this creature enters, tap target creature an opponent controls. That " +
        "creature doesn't untap during its controller's next untap step."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                creature,
                Duration.UntilAfterAffectedControllersNextUntap()
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Uriah Voth"
        flavorText = "The most important skill in a Mage Tower match is keeping one's cool."
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd79c9cc-0a8c-4d88-96e2-cb177134a18d.jpg?1783927380"
        ruling("2021-04-16", "Frost Trickster's ability can target a creature that's already tapped. That creature won't untap during its controller's next untap step.")
    }
}
