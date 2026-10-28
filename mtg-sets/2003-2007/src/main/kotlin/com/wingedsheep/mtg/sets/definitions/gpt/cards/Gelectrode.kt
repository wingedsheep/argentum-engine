package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gelectrode — Guildpact #113
 * {1}{U}{R} · Creature — Weird 0/1
 *
 * {T}: This creature deals 1 damage to any target.
 * Whenever you cast an instant or sorcery spell, you may untap this creature.
 */
val Gelectrode = card("Gelectrode") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Weird"
    power = 0
    toughness = 1
    oracleText = "{T}: This creature deals 1 damage to any target.\n" +
        "Whenever you cast an instant or sorcery spell, you may untap this creature."

    activatedAbility {
        cost = Costs.Tap
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t)
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.May(Effects.Untap(EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "Dan Murayama Scott"
        flavorText = "\"Diametrically opposing energies in self-sealed plasmodermic bubbles make great pets!\"\n" +
            "—Trivaz, Izzet mage"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83fa9c4a-7027-4ddd-93e0-3c5dcaa8e48b.jpg?1783943478"
    }
}
