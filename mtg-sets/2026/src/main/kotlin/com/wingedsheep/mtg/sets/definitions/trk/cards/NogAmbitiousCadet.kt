package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nog, Ambitious Cadet
 * {B}{G}
 * Legendary Creature — Ferengi Rogue
 * 2/2
 *
 * Menace
 * {1}: If Nog is a Rogue, he becomes a Ferengi Scout with base power and toughness 3/4 and gains
 * "Whenever Nog becomes tapped, surveil 2."
 * {3}: If Nog is a Scout, he becomes a Ferengi Officer and gains "You may play lands from your
 * graveyard" and "You may play an additional land on each of your turns."
 *
 * The Evolved Sleeper upgrade chain: neither ability has a duration, "becomes a Ferengi Scout"
 * overwrites creature types (so the Rogue check fails afterwards), and each "If Nog is a …" check
 * reads projected subtypes on resolution. The gained player-scoped statics are
 * `ProjectedStaticAbility`s, so the land-play readers see them on Nog for his controller.
 */
val NogAmbitiousCadet = card("Nog, Ambitious Cadet") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Ferengi Rogue"
    power = 2
    toughness = 2
    oracleText = "Menace\n" +
        "{1}: If Nog is a Rogue, he becomes a Ferengi Scout with base power and toughness 3/4 and " +
        "gains \"Whenever Nog becomes tapped, surveil 2.\"\n" +
        "{3}: If Nog is a Scout, he becomes a Ferengi Officer and gains \"You may play lands from " +
        "your graveyard\" and \"You may play an additional land on each of your turns.\""

    keywords(Keyword.MENACE)

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.If(
            condition = Conditions.SourceHasSubtype(Subtype("Rogue")),
            then = Effects.SetCreatureSubtypes(
                subtypes = setOf("Ferengi", "Scout"),
                target = EffectTarget.Self,
                duration = Duration.Permanent
            ) then
                Effects.SetBasePowerAndToughness(
                    power = 3,
                    toughness = 4,
                    target = EffectTarget.Self,
                    duration = Duration.Permanent
                ) then
                Effects.GrantTriggeredAbility(
                    ability = TriggeredAbility.create(
                        trigger = Triggers.self.becomesTapped(),
                        effect = Effects.Surveil(2),
                        descriptionOverride = "Whenever Nog becomes tapped, surveil 2."
                    ),
                    target = EffectTarget.Self,
                    duration = Duration.Permanent
                )
        )
        description = "If Nog is a Rogue, he becomes a Ferengi Scout with base power and toughness " +
            "3/4 and gains \"Whenever Nog becomes tapped, surveil 2.\""
    }

    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.If(
            condition = Conditions.SourceHasSubtype(Subtype("Scout")),
            then = Effects.SetCreatureSubtypes(
                subtypes = setOf("Ferengi", "Officer"),
                target = EffectTarget.Self,
                duration = Duration.Permanent
            ) then
                Effects.GrantStaticAbility(MayPlayLandsFromGraveyard, EffectTarget.Self, Duration.Permanent) then
                Effects.GrantStaticAbility(GrantAdditionalLandDrop(), EffectTarget.Self, Duration.Permanent)
        )
        description = "If Nog is a Scout, he becomes a Ferengi Officer and gains \"You may play " +
            "lands from your graveyard\" and \"You may play an additional land on each of your turns.\""
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "249"
        artist = "Darren Tan"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b513dd60-8573-467b-9c6a-855a0ba86fca.jpg?1793101384"
    }
}
