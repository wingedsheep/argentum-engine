package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thicket Basilisk
 * {3}{G}{G}
 * Creature — Basilisk
 * 2/4
 * Whenever this creature blocks or becomes blocked by a non-Wall creature, destroy that creature
 * at end of combat.
 *
 * Venom's shape on the creature itself: the filtered `blocksOrBecomesBlocked` trigger fires once
 * per non-Wall combat partner (a filter defaults `oncePerCombat` to false) with that partner as
 * `TriggeringEntity`, and a delayed end-of-combat trigger destroys it. Untargeted, so protection
 * doesn't stop it.
 */
val ThicketBasilisk = card("Thicket Basilisk") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Basilisk"
    power = 2
    toughness = 4
    oracleText = "Whenever this creature blocks or becomes blocked by a non-Wall creature, destroy " +
        "that creature at end of combat."

    triggeredAbility {
        trigger = Triggers.self.blocksOrBecomesBlocked(GameObjectFilter.Creature.notSubtype(Subtype.WALL))
        effect = Effects.CreateDelayedTrigger(
            step = Step.END_COMBAT,
            effect = Effects.Destroy(EffectTarget.TriggeringEntity),
        )
        description = "Whenever this creature blocks or becomes blocked by a non-Wall creature, " +
            "destroy that creature at end of combat."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Dan Frazier"
        flavorText = "Moss-covered statues littered the area, a macabre monument to the Basilisk's power."
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e92cce01-b3bd-4307-aae5-9a7c8fa386ab.jpg?1783948672"
        ruling("2004-10-04", "Protection from Green does not stop the Basilisk's ability because the ability is not targeted.")
        ruling("2004-10-04", "The ability destroys the creature at the end of the combat, which is after all first strike and normal damage dealing is done. This means that a creature may have to regenerate twice to survive the combat, once from damage and once again at end of combat.")
    }
}
