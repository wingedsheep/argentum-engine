package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Abomination
 * {3}{B}{B}
 * Creature — Horror
 * 2/6
 * Whenever this creature blocks or becomes blocked by a green or white creature, destroy that
 * creature at end of combat.
 *
 * Thicket Basilisk's shape with a colour filter instead of the Wall exclusion: the filtered
 * `blocksOrBecomesBlocked` trigger fires once per green-or-white combat partner, with that partner
 * as `TriggeringEntity`, and a delayed end-of-combat trigger destroys it. The colour is checked when
 * the block is declared, so a later colour change doesn't save the creature.
 */
val Abomination = card("Abomination") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror"
    power = 2
    toughness = 6
    oracleText = "Whenever this creature blocks or becomes blocked by a green or white creature, " +
        "destroy that creature at end of combat."

    triggeredAbility {
        trigger = Triggers.self.blocksOrBecomesBlocked(
            GameObjectFilter.Creature.withAnyColor(Color.GREEN, Color.WHITE)
        )
        effect = Effects.CreateDelayedTrigger(
            step = Step.END_COMBAT,
            effect = Effects.Destroy(EffectTarget.TriggeringEntity),
        )
        description = "Whenever this creature blocks or becomes blocked by a green or white " +
            "creature, destroy that creature at end of combat."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a69e2cf8-5ecb-485a-92a2-b4e0a7959f1f.jpg?1783948069"
        ruling("2004-10-04", "The color of the blocking/blocked creature is checked only at declaration of the block. If the creature is green or white at that time, it will be destroyed at end of combat even if it changes color before combat ends.")
    }
}
