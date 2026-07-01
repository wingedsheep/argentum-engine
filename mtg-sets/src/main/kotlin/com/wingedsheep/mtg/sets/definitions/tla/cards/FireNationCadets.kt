package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.firebendingAttackTrigger
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fire Nation Cadets — {R}
 * Creature — Human Soldier
 * 1/2
 *
 * This creature has firebending 2 as long as there's a Lesson card in your graveyard.
 *   (Whenever this creature attacks, add {R}{R}. This mana lasts until end of combat.)
 * {2}: This creature gets +1/+0 until end of turn.
 *
 * The conditional firebending is the genuinely new piece. Firebending has no engine handler — the
 * printed keyword is "display tag + an attack-triggered combat-duration [AddManaEffect]"
 * ([firebendingAttackTrigger]). "Has firebending 2 as long as <condition>" is modeled as a
 * [ConditionalStaticAbility] gating a [Scope.Self][GroupFilter.source] [GrantTriggeredAbility] that
 * installs that exact attack trigger on this creature only while the condition holds: when a Lesson
 * card is in your graveyard the trigger is live and attacking adds {R}{R}; remove the Lesson and the
 * grant — and therefore the trigger — disappears. The condition reuses
 * [Conditions.GraveyardContainsSubtype] ("there's a Lesson card in your graveyard"), the same check
 * Dragonfly Swarm uses. The {2} pump is a plain self-targeted [Effects.ModifyStats].
 */
val FireNationCadets = card("Fire Nation Cadets") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 2
    oracleText = "This creature has firebending 2 as long as there's a Lesson card in your graveyard. " +
        "(Whenever this creature attacks, add {R}{R}. This mana lasts until end of combat.)\n" +
        "{2}: This creature gets +1/+0 until end of turn."

    // "Has firebending 2 as long as a Lesson is in your graveyard": a Scope.Self grant of the
    // firebending attack trigger, gated on the graveyard condition so the trigger toggles live.
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantTriggeredAbility(
                ability = firebendingAttackTrigger(2),
                filter = GroupFilter.source(),
            ),
            condition = Conditions.GraveyardContainsSubtype(Subtype.LESSON),
        )
    }

    // {2}: This creature gets +1/+0 until end of turn.
    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Rafater"
        flavorText = "Fire Nation training ensures that even the smallest flame burns into a raging inferno."
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a3a862c-9c9a-41cd-94d1-2d0cff22a6cd.jpg?1764120921"
    }
}
