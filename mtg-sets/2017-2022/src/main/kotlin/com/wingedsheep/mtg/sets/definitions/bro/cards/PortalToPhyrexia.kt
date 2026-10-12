package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Portal to Phyrexia — The Brothers' War #240
 * {9} · Artifact
 *
 * When this artifact enters, each opponent sacrifices three creatures of their choice.
 * At the beginning of your upkeep, put target creature card from a graveyard onto the battlefield
 * under your control. It's a Phyrexian in addition to its other types.
 *
 * The enters trigger is the Barter in Blood edict at `count = 3` over [Player.EachOpponent]; the
 * upkeep trigger is Reanimate's "from a graveyard … under your control" move followed by Terror of
 * Towashi's permanent Phyrexian type grant on the same target.
 */
val PortalToPhyrexia = card("Portal to Phyrexia") {
    manaCost = "{9}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "When this artifact enters, each opponent sacrifices three creatures of their choice.\n" +
        "At the beginning of your upkeep, put target creature card from a graveyard onto the battlefield " +
        "under your control. It's a Phyrexian in addition to its other types."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature,
            count = 3,
            target = EffectTarget.PlayerRef(Player.EachOpponent)
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val creatureCard = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.Move(
            creatureCard,
            Zone.BATTLEFIELD,
            controllerOverride = EffectTarget.Controller,
            fromZone = Zone.GRAVEYARD
        ) then
            Effects.AddCreatureType("Phyrexian", creatureCard, Duration.Permanent)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "240"
        artist = "Svetlin Velinov"
        flavorText = "After five thousand years, the nightmare returned."
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f608efc-0dbc-4cc3-aadd-ed473bfc29ab.jpg?1783920015"
    }
}
