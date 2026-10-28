package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mirage Mirror
 * {3}
 * Artifact
 * {2}: This artifact becomes a copy of target artifact, creature, enchantment, or land until end of turn.
 *
 * The single-permanent copy shape (`affected = Self`, `Duration.EndOfTurn`, Deepfathom Echo's form):
 * only copiable values change (CR 707.2), so counters, tapped state and earlier non-copy effects stay,
 * and the Mirror does *not* keep this ability while it's a copy (first ruling). Repeated activations
 * overwrite each other and all wear off together at cleanup.
 */
val MirageMirror = card("Mirage Mirror") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{2}: This artifact becomes a copy of target artifact, creature, enchantment, or land until end of turn."

    activatedAbility {
        cost = Costs.Mana("{2}")
        val permanent = target(
            TargetFilter(
                GameObjectFilter.Artifact or GameObjectFilter.Creature or
                    GameObjectFilter.Enchantment or GameObjectFilter.Land
            )
        )
        effect = Effects.EachPermanentBecomesCopyOfTarget(
            target = permanent,
            affected = EffectTarget.Self,
            duration = Duration.EndOfTurn,
        )
        description = "{2}: This artifact becomes a copy of target artifact, creature, enchantment, or land until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "Craig J Spearing"
        flavorText = "Most mirrors show your reflection. Others show your potential."
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29148d7e-b398-4e19-a29e-d9a660ad5016.jpg?1783936001"
        ruling("2017-07-14", "Once Mirage Mirror's ability resolves, it no longer has that ability.")
        ruling("2017-07-14", "Mirage Mirror copies the printed values of the target permanent, plus any copy effects that have been applied to it. It won't copy counters on that permanent or effects that have changed its power, toughness, types, color, or so on. Notably, it won't copy effects that made the target permanent become a creature.")
        ruling("2017-07-14", "If you activate Mirage Mirror's ability multiple times in a turn in response to itself, then each time one of those abilities resolves, it will overwrite whatever Mirage Mirror is copying. Mirage Mirror will wind up as a copy of the permanent targeted by the last ability to resolve. When the turn ends, all instances of the ability will wear off at the same time.")
    }
}
