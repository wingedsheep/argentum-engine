package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thespian's Stage — Gatecrash #248 (canonical printing)
 * Land
 *
 * {T}: Add {C}.
 * {2}, {T}: This land becomes a copy of target land, except it has this ability.
 *
 * The Dimir Doppelganger shape aimed at a land on the battlefield: [Effects.EachPermanentBecomesCopyOfTarget]
 * with `affected = Self` replaces the Stage's copiable values with the target's (CR 707.2 — counters,
 * tapped state and non-copy effects stay behind, so it doesn't untap and an animated manland copies
 * as the plain land), and `retainActivatingAbility` re-grants the `{2}, {T}` ability durably so the
 * Stage can be re-aimed later. The copy has no duration (`Duration.Permanent`), and the Stage's
 * first ability ({T}: Add {C}) is lost along with the rest of its printed text, per the rulings.
 * "Target land" carries no controller restriction.
 */
val ThespiansStage = card("Thespian's Stage") {
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{2}, {T}: This land becomes a copy of target land, except it has this ability."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val land = target(TargetFilter.Land)
        effect = Effects.EachPermanentBecomesCopyOfTarget(
            target = land,
            affected = EffectTarget.Self,
            duration = Duration.Permanent,
            retainActivatingAbility = true,
        )
        description = "{2}, {T}: This land becomes a copy of target land, except it has this ability."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "248"
        artist = "John Avon"
        flavorText = "Amid rumors of war, the third act of *The Absolution of the Guildpact* was " +
            "quickly rewritten as a tragedy."
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6f27909-e5cd-44c0-91c4-21624f692fd9.jpg?1783940087"
        ruling(
            "2018-12-07",
            "The copy effect created by the last activated ability doesn't have a duration. It will " +
                "last until Thespian's Stage leaves the battlefield or another copy effect overwrites " +
                "it. The permanent will no longer have the first ability of Thespian's Stage."
        )
        ruling(
            "2018-12-07",
            "A land's copiable values are those printed on it, as modified by other copy effects. " +
                "Counters and other effects aren't copied. Notably, if you copy a land that is also a " +
                "creature because of a temporary effect (such as Celestial Colonnade), Thespian's " +
                "Stage will become just the \"unanimated\" land."
        )
        ruling(
            "2018-12-07",
            "Thespian's Stage doesn't become untapped when it becomes a copy, even if the target " +
                "land is untapped."
        )
        ruling(
            "2018-12-07",
            "No enters-the-battlefield abilities of the land Thespian's Stage is copying will " +
                "trigger. Thespian's Stage was already on the battlefield. Similarly, no \"as this " +
                "land enters the battlefield\" or \"this land enters the battlefield with\" effects, " +
                "such as that of Dark Depths, will apply."
        )
    }
}
