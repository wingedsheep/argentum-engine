package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Agents of S.H.I.E.L.D. — Marvel Super Heroes #5
 * {2}{W} · Creature — Human Spy Hero · 2/4
 *
 * Whenever a creature you control attacks alone, that creature gets +1/+1 until end of turn.
 *
 * ANY-bound [Triggers.attacks] with [AttackPredicate.Alone] over "creature you control"; "that
 * creature" is the lone attacker, i.e. [EffectTarget.TriggeringEntity] — so the buff lands on
 * whichever creature attacked alone, including Agents of S.H.I.E.L.D. itself.
 */
val AgentsOfShield = card("Agents of S.H.I.E.L.D.") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Spy Hero"
    power = 2
    toughness = 4
    oracleText = "Whenever a creature you control attacks alone, that creature gets +1/+1 until end of turn."

    triggeredAbility {
        trigger = Triggers.attacks(
            filter = GameObjectFilter.Creature.youControl(),
            requires = setOf(AttackPredicate.Alone),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.ModifyStats(1, 1, EffectTarget.TriggeringEntity)
        description = "Whenever a creature you control attacks alone, that creature gets +1/+1 " +
            "until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Borja Pindado"
        flavorText = "Behind every S.H.I.E.L.D. solo mission are dozens of intelligence operatives, " +
            "logistics coordinators, and backup agents."
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9b5e156-0764-44e0-b0c7-de2561ea9e04.jpg?1783902978"
    }
}
