package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kellogg, Dangerous Mind
 * {1}{B}{R}
 * Legendary Creature — Human Mercenary
 * 3/2
 *
 * First strike, haste
 * Whenever Kellogg attacks, create a Treasure token.
 * Sacrifice five Treasures: Gain control of target creature for as long as you control Kellogg.
 * Activate only as a sorcery.
 *
 * The steal is Aladdin's [Duration.WhileYouControlSource]: the projector drops the control effect the
 * moment Kellogg leaves the battlefield or changes controller, and an effect created after Kellogg is
 * already gone ends at once — the ruling's "you won't gain control of the target creature at all".
 */
val KelloggDangerousMind = card("Kellogg, Dangerous Mind") {
    manaCost = "{1}{B}{R}"
    typeLine = "Legendary Creature — Human Mercenary"
    power = 3
    toughness = 2
    oracleText = "First strike, haste\n" +
        "Whenever Kellogg attacks, create a Treasure token.\n" +
        "Sacrifice five Treasures: Gain control of target creature for as long as you control Kellogg. " +
        "Activate only as a sorcery."

    keywords(Keyword.FIRST_STRIKE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CreateTreasure(
            imageUri = "https://cards.scryfall.io/normal/front/1/d/1d74d012-66ca-4a0c-9e90-e31e96730d4d.jpg?1783912040"
        )
    }

    activatedAbility {
        cost = Costs.SacrificeMultiple(5, GameObjectFilter.Artifact.withSubtype("Treasure"))
        val creature = target(TargetFilter.Creature)
        effect = Effects.GainControl(creature, Duration.WhileYouControlSource("Kellogg"))
        timing = TimingRule.SorcerySpeed
        description = "Sacrifice five Treasures: Gain control of target creature for as long as you control Kellogg."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "106"
        artist = "Josh Hass"
        flavorText = "\"We both know how this will end. So... you ready?\""
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2b22b32-be24-48e3-aa81-9256190df02c.jpg?1783912387"
        ruling("2024-03-08", "If you lose control of Kellogg before its last ability resolves, you won't gain control of the target creature at all.")
    }
}
