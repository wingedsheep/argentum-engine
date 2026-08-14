package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glóin the Mighty // Easy Pickings — The Hobbit #99
 * {3}{R} · Legendary Creature — Dwarf Warrior · Uncommon
 * 4/3
 *
 * At the beginning of your first main phase, add {R}{R}.
 *
 * Adventure: Easy Pickings — {2}{R}, Sorcery — Adventure
 * Easy Pickings deals 1 damage to each creature your opponents control.
 *
 * The mana trigger is *not* a mana ability (CR 605.1b) — it triggers off a step beginning rather
 * than off a mana ability resolving, so it uses the stack like any other triggered ability. Modeled
 * as [Triggers.FirstMainPhase], which is the precombat main phase of the controller's turn; the
 * mana lands in the pool once the trigger resolves and empties at the end of that phase.
 *
 * Easy Pickings is a group effect, not a targeted one — [GroupFilter.AllCreaturesOpponentsControl]
 * snapshots the affected set before iterating, and [EffectTarget.Self] inside the body binds to the
 * creature currently being damaged. Hexproof and shroud don't apply; protection from red does, and
 * the engine's damage handling covers that.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the caster
 * cast it as the creature spell while it remains in exile.)
 */
val GloinTheMighty = card("Glóin the Mighty") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Dwarf Warrior"
    power = 4
    toughness = 3
    oracleText = "At the beginning of your first main phase, add {R}{R}."

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = Effects.AddMana(Color.RED, 2)
    }

    adventure("Easy Pickings") {
        manaCost = "{2}{R}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Easy Pickings deals 1 damage to each creature your opponents control. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.ForEachInGroup(
                filter = GroupFilter.AllCreaturesOpponentsControl,
                effect = Effects.DealDamage(1, EffectTarget.Self)
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "Colin Boyer"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/5793b8eb-2fc5-454d-8fa2-20346fef167a.jpg?1785324545"
    }
}
