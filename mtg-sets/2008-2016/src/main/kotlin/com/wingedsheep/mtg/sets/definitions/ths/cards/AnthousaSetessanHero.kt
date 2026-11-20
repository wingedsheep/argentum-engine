package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Anthousa, Setessan Hero — Theros #149 (canonical printing)
 * {3}{G}{G}
 * Legendary Creature — Human Warrior
 * 4/5
 * Heroic — Whenever you cast a spell that targets Anthousa, up to three target lands you control
 * each become 2/2 Warrior creatures until end of turn. They're still lands.
 *
 * Heroic is the `TargetsSource` cast trigger (Fabled Hero); the land animation is Nissa, Steward
 * of Elements' "up to N target lands you control" `ForEachTarget` + `BecomeCreature` shape.
 */
val AnthousaSetessanHero = card("Anthousa, Setessan Hero") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Warrior"
    power = 4
    toughness = 5
    oracleText = "Heroic — Whenever you cast a spell that targets Anthousa, up to three target lands " +
        "you control each become 2/2 Warrior creatures until end of turn. They're still lands."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        targets(TargetFilter(GameObjectFilter.Land.youControl()), count = 3, optional = true)
        effect = Effects.ForEachTarget(
            Effects.BecomeCreature(
                target = EffectTarget.ContextTarget(0),
                power = 2,
                toughness = 2,
                creatureTypes = setOf("Warrior"),
                duration = Duration.EndOfTurn,
            )
        )
        description = "Heroic — Whenever you cast a spell that targets Anthousa, up to three target " +
            "lands you control each become 2/2 Warrior creatures until end of turn. They're still lands."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "149"
        artist = "Howard Lyon"
        flavorText = "The warriors she leads can rarely keep pace with her, and neither can the tales."
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca89e4d4-185d-4a08-b7af-03f6eff38ba3.jpg?1783939750"
        ruling("2013-09-15", "If a land that hasn't been under your control continuously since your last turn began becomes a creature, it can't attack and its activated abilities that include {T} can't be activated. Notably, it can't be tapped for mana that turn.")
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
