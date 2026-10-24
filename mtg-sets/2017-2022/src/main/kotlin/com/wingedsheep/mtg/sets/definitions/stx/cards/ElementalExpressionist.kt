package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Elemental Expressionist — Strixhaven: School of Mages #181 (canonical printing)
 * {U/R}{U/R}{U/R}{U/R} · Creature — Orc Wizard · 4/4
 *
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, choose target creature you
 * control. Until end of turn, it gains "If this creature would leave the battlefield, exile it
 * instead of putting it anywhere else" and "When this creature is put into exile, create a 4/4
 * blue and red Elemental creature token."
 *
 * Both riders are abilities the target gains for the turn: the replacement is a granted
 * `selfOnly` [RedirectZoneChange], which redirects only the creature that holds it; the token
 * maker is a granted self leaves-to-exile trigger. Each resolution grants another instance of the
 * trigger, so a creature targeted twice makes two Elementals (per the rulings).
 */
val ElementalExpressionist = card("Elemental Expressionist") {
    manaCost = "{U/R}{U/R}{U/R}{U/R}"
    colorIdentity = "UR"
    typeLine = "Creature — Orc Wizard"
    power = 4
    toughness = 4
    oracleText = "Magecraft — Whenever you cast or copy an instant or sorcery spell, choose target " +
        "creature you control. Until end of turn, it gains \"If this creature would leave the " +
        "battlefield, exile it instead of putting it anywhere else\" and \"When this creature is put " +
        "into exile, create a 4/4 blue and red Elemental creature token.\" (Each instance of that " +
        "ability triggers separately.)"

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.GrantReplacementEffect(
            replacement = RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(from = Zone.BATTLEFIELD),
                selfOnly = true,
            ),
            target = t,
            duration = Duration.EndOfTurn,
        ) then
            Effects.GrantTriggeredAbility(
                ability = TriggeredAbility.create(
                    trigger = Triggers.self.leaves(to = Zone.EXILE),
                    effect = Effects.CreateToken(
                        power = 4,
                        toughness = 4,
                        colors = setOf(Color.BLUE, Color.RED),
                        creatureTypes = setOf("Elemental"),
                    ),
                    descriptionOverride = "When this creature is put into exile, create a 4/4 blue " +
                        "and red Elemental creature token.",
                ),
                target = t,
                duration = Duration.EndOfTurn,
            )
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, choose " +
            "target creature you control. Until end of turn, it gains \"If this creature would leave " +
            "the battlefield, exile it instead of putting it anywhere else\" and \"When this creature " +
            "is put into exile, create a 4/4 blue and red Elemental creature token.\""
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "181"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/7473db42-905b-4fda-af8c-fd24d906b642.jpg?1783927316"
        ruling("2021-04-16", "Elemental Expressionist has received an update to its Oracle text. Specifically, the second ability granted to the target creature doesn't care that the creature's controller is the one exiling it. If the creature is put into exile for any reason, that ability will trigger.")
        ruling("2021-04-16", "If you target a creature token with Elemental Expressionist's ability, and that token would leave the battlefield, it is still exiled (very briefly) before it ceases to exist, and you will still create an Elemental token.")
        ruling("2021-04-16", "If the magecraft ability triggers and resolves more than once with the same creature as the target, that creature will have multiple instances of each of the two triggered abilities the magecraft ability grants it. Each instance of the second ability will trigger separately, and you'll create an Elemental token for each of them. (Multiple instances of the first granted ability are redundant.)")
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
    }
}
