package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Impetuous Devils — Eldritch Moon #132
 * {2}{R}{R} · Creature — Devil · 6/1
 *
 * Trample, haste
 * When this creature attacks, up to one target creature defending player controls blocks it this combat if able.
 * At the beginning of the end step, sacrifice this creature.
 *
 * Attack lure as Avalanche Tusker (KTK) with Fighter Class's "up to one"; end-step sacrifice as Ball Lightning.
 */
val ImpetuousDevils = card("Impetuous Devils") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Devil"
    oracleText = "Trample, haste\nWhen this creature attacks, up to one target creature defending player controls blocks it this combat if able.\nAt the beginning of the end step, sacrifice this creature."
    power = 6
    toughness = 1

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.Creature.defendingPlayerControls(), optional = true)
        effect = Effects.ForceBlock(creature)
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        effect = SacrificeSelfEffect
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "132"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1ba7acc0-6ef0-4c34-be23-118e1872271f.jpg?1783937460"
        ruling(
            "2016-07-13",
            "If a creature affected by the first triggered ability of Impetuous Devils is tapped or is affected by a spell or ability that says it can't block, then it doesn't block. If there's a cost associated with having that creature block, its controller isn't forced to pay that cost, so it doesn't have to block in that case either."
        )
    }
}
