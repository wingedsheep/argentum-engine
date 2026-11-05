package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Thaumatic Compass // Spires of Orazca (Ixalan — the card's earliest printing)
 * {2}
 * Artifact // Land
 *
 * Front — Thaumatic Compass (Artifact, {2})
 *   {3}, {T}: Search your library for a basic land card, reveal it, put it into your hand,
 *   then shuffle.
 *   At the beginning of your end step, if you control seven or more lands, transform this artifact.
 *
 * Back — Spires of Orazca (Land)
 *   {T}: Add {C}.
 *   {T}: Untap target attacking creature an opponent controls and remove it from combat.
 *
 * Implementation:
 *  - Tutor is the Lay of the Land pipeline ([Patterns.Library.searchLibrary], basic land → hand,
 *    revealed).
 *  - End-step transform is an intervening-if on [Conditions.ControlLandsAtLeast]`(7)`, the
 *    Growing Rites of Itlimoc shape.
 *  - Spires' combat ability is [Effects.Untap] then [Effects.RemoveFromCombat] on one target
 *    (Reconnaissance's composition, aimed at an opponent's attacker). Removing the creature from
 *    combat doesn't undo that it attacked (2017-09-29 ruling).
 */

private val ThaumaticCompassFront = card("Thaumatic Compass") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "{3}, {T}: Search your library for a basic land card, reveal it, put it into " +
        "your hand, then shuffle.\n" +
        "At the beginning of your end step, if you control seven or more lands, transform " +
        "this artifact."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.HAND,
            reveal = true,
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.ControlLandsAtLeast(7)
        effect = Effects.Transform(EffectTarget.Self)
        description = "At the beginning of your end step, if you control seven or more lands, " +
            "transform Thaumatic Compass."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "249"
        artist = "Yeong-Hao Han"
        flavorText = "\"Clearly, it indicates the aetheric azimuth of . . .\"\n—Jace Beleren"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/392af78e-34d5-4b1b-8b29-0e702271e4d7.jpg?1783935706"
    }
}

private val SpiresOfOrazca = card("Spires of Orazca") {
    manaCost = ""
    typeLine = "Land"
    oracleText = "(Transforms from Thaumatic Compass.)\n" +
        "{T}: Add {C}.\n" +
        "{T}: Untap target attacking creature an opponent controls and remove it from combat."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        val attacker = target(TargetFilter(GameObjectFilter.Creature.attacking().opponentControls()))
        effect = Effects.Untap(attacker) then Effects.RemoveFromCombat(attacker)
        description = "Untap target attacking creature an opponent controls and remove it from combat."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "249"
        artist = "Yeong-Hao Han"
        flavorText = "\". . . a cryptic objective.\"\n—Jace Beleren"
        imageUri = "https://cards.scryfall.io/normal/back/3/9/392af78e-34d5-4b1b-8b29-0e702271e4d7.jpg?1783935706"
        ruling(
            "2017-09-29",
            "Removing a creature from combat doesn't change the fact that it attacked, even " +
                "though it's no longer an attacking creature. Notably, raid abilities will still " +
                "be satisfied if every attacking creature is removed from combat.",
        )
    }
}

val ThaumaticCompass: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = ThaumaticCompassFront,
    backFace = SpiresOfOrazca,
)
