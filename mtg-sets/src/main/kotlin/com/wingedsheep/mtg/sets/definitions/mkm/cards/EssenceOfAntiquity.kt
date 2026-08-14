package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Essence of Antiquity — Murders at Karlov Manor #15
 * {3}{W}{W} · Artifact Creature — Golem · 1/10
 *
 * Disguise {2}{W}
 * When this creature is turned face up, creatures you control gain hexproof until end of turn.
 * Untap them.
 *
 * A combat trick disguised as a wall. Cast face down for {3}, attack or block with the 2/2, then
 * flip for {2}{W} — turning face up is a special action that can't be responded to (CR 701.34a),
 * so the hexproof lands before any removal spell can be cast in response to the flip itself. The
 * untap is the second half of the ambush: creatures that were tapped attacking are untapped and
 * can block.
 *
 * "Creatures you control … Untap them" is a single affected set captured at resolution
 * ([Effects.ForEachInGroup] over `Creature.youControl()`), so both halves — the grant and the
 * untap — apply to exactly the same creatures. The Golem itself is included; it is on the
 * battlefield the whole time, only its face changes.
 *
 * Note the composite order: hexproof is granted before the untap, but neither depends on the other,
 * and both are one-shot resolution effects rather than statics, so a creature that enters after the
 * trigger resolves gets nothing.
 */
val EssenceOfAntiquity = card("Essence of Antiquity") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Golem"
    oracleText = "Disguise {2}{W} (You may cast this card face down for {3} as a 2/2 creature " +
        "with ward {2}. Turn it face up any time for its disguise cost.)\n" +
        "When this creature is turned face up, creatures you control gain hexproof until end of " +
        "turn. Untap them."
    power = 1
    toughness = 10

    disguise = "{2}{W}"

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.Composite(
                Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.Self),
                Effects.Untap(EffectTarget.Self),
            ),
        )
        description = "When this creature is turned face up, creatures you control gain hexproof " +
            "until end of turn. Untap them."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Caio Monteiro"
        flavorText = "In a city as old as Ravnica, history takes on a life of its own—even when " +
            "you least expect it."
        imageUri = "https://cards.scryfall.io/normal/front/a/e/aee2945d-bf6d-4328-a482-df24c2973b56.jpg?1783912926"

        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to. Only a face-down permanent can " +
                "be turned face up this way; a face-down spell cannot."
        )
    }
}
