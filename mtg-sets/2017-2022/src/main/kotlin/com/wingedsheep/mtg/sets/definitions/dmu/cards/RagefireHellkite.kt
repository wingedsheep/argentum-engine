package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ragefire Hellkite
 * {4}{R}{R}
 * Creature — Dragon
 * 5/3
 * Flying
 * Whenever this creature attacks, you may sacrifice another creature. If you do, this creature
 * gains double strike until end of turn.
 *
 * The Vampire Gourmand / Comet Crawler shape: an optional sacrifice of another creature you
 * control gates the payoff, so declining (or having no other creature) grants nothing.
 */
val RagefireHellkite = card("Ragefire Hellkite") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 5
    toughness = 3
    oracleText = "Flying\nWhenever this creature attacks, you may sacrifice another creature. If you do, " +
        "this creature gains double strike until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            Effects.SacrificeOwn(GameObjectFilter.Creature, excludeSource = true),
            then = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.Self)
        )
        description = "Whenever this creature attacks, you may sacrifice another creature. If you do, " +
            "this creature gains double strike until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "285"
        artist = "Xavier Ribeiro"
        flavorText = "\"Well, the good news is that sound wasn't the volcano erupting . . .\"\n—George, Coalition scout"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23ccd68d-0162-4a4c-892e-1726b8b28612.jpg?1783921243"
    }
}
