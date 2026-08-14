package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Beorn the Fierce — The Hobbit #119
 * {3}{G}{G} · Legendary Creature — Bear Shapeshifter Warrior · Mythic
 * 6/6
 *
 * Trample
 * Other Bears you control get +2/+2.
 * At the beginning of combat on your turn, put a trample counter on up to one target creature you
 * control. It becomes a Bear in addition to its other types. Then if you control three or more
 * Bears, draw two cards.
 *
 * Modeling notes:
 *  - The anthem is the Elvish Champion shape with `.youControl()` added; `excludeSelf` carries the
 *    "Other". Beorn is himself a Bear, so he still counts toward the draw check below.
 *  - "Up to one target" is `TargetCreature(optional = true)` (→ `minCount = 0`), and the payload runs
 *    through [ForEachTargetEffect] so choosing zero targets skips the counter and the type change
 *    without fizzling the trigger — the draw clause still gets its chance.
 *  - The Bear subtype is [Duration.Permanent], not end-of-turn: the text has no duration, so the
 *    creature stays a Bear (CR 205.1b). It is [Effects.AddSubtype] rather than any "becomes"
 *    primitive that *sets* subtypes — "in addition to its other types" is purely additive.
 *  - The draw check is a plain [ConditionalEffect] evaluated after the type change, and the count is
 *    a projected battlefield read, so the creature that just became a Bear is already counted.
 */
val BeornTheFierce = card("Beorn the Fierce") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Bear Shapeshifter Warrior"
    power = 6
    toughness = 6
    oracleText = "Trample\n" +
        "Other Bears you control get +2/+2.\n" +
        "At the beginning of combat on your turn, put a trample counter on up to one target " +
        "creature you control. It becomes a Bear in addition to its other types. Then if you " +
        "control three or more Bears, draw two cards."

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 2,
            toughnessBonus = 2,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.BEAR).youControl(),
                excludeSelf = true
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.BeginCombat
        target(
            "up to one target creature you control",
            TargetCreature(optional = true, filter = TargetFilter.Creature.youControl())
        )
        effect = Effects.Composite(
            ForEachTargetEffect(
                listOf(
                    Effects.AddCounters(Counters.TRAMPLE, 1, EffectTarget.ContextTarget(0)),
                    Effects.AddSubtype(
                        Subtype.BEAR.value,
                        EffectTarget.ContextTarget(0),
                        Duration.Permanent
                    )
                )
            ),
            ConditionalEffect(
                condition = Conditions.YouControlAtLeast(
                    3,
                    GameObjectFilter.Creature.withSubtype(Subtype.BEAR)
                ),
                effect = Effects.DrawCards(2)
            )
        )
        description = "At the beginning of combat on your turn, put a trample counter on up to one " +
            "target creature you control. It becomes a Bear in addition to its other types. Then " +
            "if you control three or more Bears, draw two cards."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "119"
        artist = "Nia Kovalevski"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/367d5f8b-77ee-47f7-bc71-972d62c280a9.jpg?1784632151"
    }
}
