package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Reroute Systems
 * {W}
 * Instant
 * Choose one —
 * • Target artifact or creature gains indestructible until end of turn. (Damage and effects that say "destroy" don't destroy it.)
 * • Reroute Systems deals 2 damage to target tapped creature.
 */
val RerouteSystems = card("Reroute Systems") {
    manaCost = "{W}"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Target artifact or creature gains indestructible until end of turn. (Damage and effects that say \"destroy\" don't destroy it.)\n• Reroute Systems deals 2 damage to target tapped creature."

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: Target artifact or creature gains indestructible until end of turn
            Mode.withTarget(
                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.ContextTarget(0)),
                TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature)),
                "Target artifact or creature gains indestructible until end of turn"
            ),
            // Mode 2: Reroute Systems deals 2 damage to target tapped creature
            Mode.withTarget(
                Effects.DealDamage(2, EffectTarget.ContextTarget(0)),
                Targets.TappedCreature,
                "Reroute Systems deals 2 damage to target tapped creature"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "Sergey Glushakov"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3bbdba38-2b99-4226-98e3-6d2580345d6d.jpg?1752946673"
    }
}
