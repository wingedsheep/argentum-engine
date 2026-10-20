package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wall of Caltrops — Legends #42
 * {1}{W} · Creature — Wall 2/1
 *
 * Defender
 * Whenever this creature blocks a creature, if at least one other Wall creature is blocking that
 * creature and no non-Wall creatures are blocking that creature, this creature gains banding until
 * end of turn.
 *
 * `blocks(Creature)` fires once per blocked attacker with that attacker as `TriggeringEntity`, so
 * "that creature" is read live through [GameObjectFilter.blockingEntity]. The intervening "if" is
 * two battlefield presence checks over the attacker's blockers: another Wall (excluding this one)
 * must exist, and no non-Wall may.
 */
val WallOfCaltrops = card("Wall of Caltrops") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Wall"
    power = 2
    toughness = 1
    oracleText = "Defender (This creature can't attack.)\n" +
        "Whenever this creature blocks a creature, if at least one other Wall creature is blocking that " +
        "creature and no non-Wall creatures are blocking that creature, this creature gains banding until " +
        "end of turn. (If any creatures with banding you control are blocking a creature, you divide that " +
        "creature's combat damage, not its controller, among any of the creatures it's being blocked by.)"

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.self.blocks(GameObjectFilter.Creature)
        interveningIf = Conditions.All(
            Conditions.AnyPlayerControls(
                GameObjectFilter.Creature.withSubtype(Subtype.WALL)
                    .blockingEntity(EffectTarget.TriggeringEntity),
                excludeSelf = true
            ),
            Conditions.AnyPlayerControls(
                GameObjectFilter.Creature.notSubtype(Subtype.WALL)
                    .blockingEntity(EffectTarget.TriggeringEntity),
                negate = true
            )
        )
        effect = Effects.GrantKeyword(Keyword.BANDING, EffectTarget.Self)
        description = "Whenever this creature blocks a creature, if at least one other Wall creature is " +
            "blocking that creature and no non-Wall creatures are blocking that creature, this creature " +
            "gains banding until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "42"
        artist = "Brian Snõddy"
        flavorText = "\"Ow! Ow ow ow! Oooh, ow, OW!\""
        imageUri = "https://cards.scryfall.io/normal/front/6/6/664ad588-3002-4f63-93bd-38663171018f.jpg?1783948079"
    }
}
