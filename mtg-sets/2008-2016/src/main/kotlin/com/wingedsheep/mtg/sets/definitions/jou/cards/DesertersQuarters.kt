package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Deserter's Quarters — Journey into Nyx #160 (canonical printing)
 * {2} · Artifact
 *
 * You may choose not to untap this artifact during your untap step.
 * {6}, {T}: Tap target creature. It doesn't untap during its controller's untap step for as long
 * as this artifact remains tapped.
 *
 * Same lock as Phyrexian Gremlins: the ability taps the creature and grants it
 * [AbilityFlag.DOESNT_UNTAP] for [Duration.WhileSourceTapped], so it stays out of its controller's
 * untap step only while the Quarters is tapped — even if the creature is untapped some other way in
 * the meantime. [AbilityFlag.MAY_NOT_UNTAP] lets the controller hold the Quarters tapped.
 */
val DesertersQuarters = card("Deserter's Quarters") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "You may choose not to untap this artifact during your untap step.\n" +
        "{6}, {T}: Tap target creature. It doesn't untap during its controller's untap step for as long " +
        "as this artifact remains tapped."

    flags(AbilityFlag.MAY_NOT_UNTAP)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{6}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(AbilityFlag.DOESNT_UNTAP, creature, Duration.WhileSourceTapped("this artifact"))
        description = "{6}, {T}: Tap target creature. It doesn't untap during its controller's untap step " +
            "for as long as this artifact remains tapped."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Daniel Ljunggren"
        flavorText = "In Akros, the penalty for running from battle is one night's stay in the Deserter's Quarters."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f7d3b2d-e06e-4d30-9ed8-2a52aa7e31c6.jpg?1783939400"
        ruling(
            "2014-04-26",
            "The activated ability can target any creature, not just one that's untapped."
        )
        ruling(
            "2014-04-26",
            "As long as Deserter's Quarters remains tapped, the effect stopping the creature from untapping " +
                "during its controller's untap step remains even if that creature untaps some other way " +
                "(for example, with Colossal Heroics)."
        )
    }
}
