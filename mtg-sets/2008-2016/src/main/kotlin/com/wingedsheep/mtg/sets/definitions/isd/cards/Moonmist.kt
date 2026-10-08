package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Moonmist
 * {1}{G}
 * Instant
 *
 * Transform all Humans. Prevent all combat damage that would be dealt this turn by creatures
 * other than Werewolves and Wolves. (Only double-faced cards can be transformed.)
 *
 * "Transform all Humans" walks every Human permanent and transforms it — the Glissa / Elesh Norn
 * group-transform shape. A Human that isn't double-faced is a silent no-op (CR 701.27c), and a
 * Werewolf's night face isn't a Human, so it isn't flipped back. The prevention is the Frontline
 * Strategist shield ([Effects.PreventCombatDamageFrom]) over non-Werewolf, non-Wolf creatures: the
 * source filter is read as each combat damage event happens, so per the rulings a creature that
 * enters later, or stops being a Werewolf/Wolf, is judged at the time the damage would be dealt.
 */
val Moonmist = card("Moonmist") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Transform all Humans. Prevent all combat damage that would be dealt this turn by " +
        "creatures other than Werewolves and Wolves. (Only double-faced cards can be transformed.)"

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.HUMAN)),
            Effects.Transform(EffectTarget.IterationEntity)
        ) then Effects.PreventCombatDamageFrom(
            source = GameObjectFilter.Creature.notSubtype(Subtype.WEREWOLF).notSubtype(Subtype.WOLF),
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "195"
        artist = "Ryan Yee"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57153c3f-9e55-418c-b67b-36901f29f9c1.jpg?1783940913"
        ruling("2011-09-22", "Moonmist causes any double-faced Human to transform, not just Werewolves.")
        ruling("2011-09-22", "Whether or not a creature is a Werewolf or a Wolf is checked only as combat damage is dealt. If the creature isn't a Werewolf or a Wolf at that time, its combat damage will be prevented.")
        ruling("2011-09-22", "Moonmist will prevent combat damage dealt by a creature that isn't a Werewolf or a Wolf even if that creature wasn't on the battlefield (or was a Werewolf or a Wolf) when Moonmist resolved.")
    }
}
