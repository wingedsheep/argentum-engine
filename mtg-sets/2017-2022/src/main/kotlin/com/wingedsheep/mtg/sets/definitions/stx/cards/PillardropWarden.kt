package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pillardrop Warden — Strixhaven: School of Mages #112 (canonical printing)
 * {3}{R} · Creature — Spirit Dwarf · 1/5
 *
 * Reach
 * {2}, {T}, Sacrifice this creature: Return target instant or sorcery card from your graveyard
 * to your hand. Activate only as a sorcery.
 *
 * Reach is the bare keyword; the ability is a {2} + tap + sacrifice-self composite cost,
 * sorcery-speed, returning a [TargetFilter.InstantOrSorceryInYourGraveyard] target to hand.
 */
val PillardropWarden = card("Pillardrop Warden") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Spirit Dwarf"
    power = 1
    toughness = 5
    oracleText = "Reach\n" +
        "{2}, {T}, Sacrifice this creature: Return target instant or sorcery card from your " +
        "graveyard to your hand. Activate only as a sorcery."

    keywords(Keyword.REACH)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf)
        val spellCard = target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.ReturnToHand(spellCard)
        timing = TimingRule.SorcerySpeed
        description = "Return target instant or sorcery card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "Antonio José Manzanedo"
        flavorText = "A giant statue of a dwarf is still a giant statue."
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d509f73-cd62-4116-9652-0ed801ed8b36.jpg?1783927350"
    }
}
