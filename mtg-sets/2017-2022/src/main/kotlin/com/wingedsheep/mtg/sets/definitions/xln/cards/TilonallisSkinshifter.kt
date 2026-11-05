package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tilonalli's Skinshifter {2}{R}
 * Creature — Human Shaman
 * 0/1
 *
 * Haste
 * Whenever this creature attacks, it becomes a copy of another target nonlegendary attacking
 * creature until end of turn.
 *
 * The copy is [Effects.EachPermanentBecomesCopyOfTarget] narrowed to `affected = Self` with an
 * end-of-turn duration (the Lazav, Familiar Stranger shape). Only the card's copiable values are
 * swapped, so the Skinshifter stays declared as an attacker, keeps any counters and resolved
 * effects, and reverts at cleanup.
 */
val TilonallisSkinshifter = card("Tilonalli's Skinshifter") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    power = 0
    toughness = 1
    oracleText = "Haste\nWhenever this creature attacks, it becomes a copy of another target " +
        "nonlegendary attacking creature until end of turn."
    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val attacker = target(TargetFilter(GameObjectFilter.Creature.nonlegendary().attacking()).other())
        effect = Effects.EachPermanentBecomesCopyOfTarget(
            target = attacker,
            affected = EffectTarget.Self,
            duration = Duration.EndOfTurn,
        )
        description = "Whenever this creature attacks, it becomes a copy of another target " +
            "nonlegendary attacking creature until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "170"
        artist = "Jason Rainville"
        flavorText = "\"I am the ferocity of the Burning Sun. I am hide and feather, fang and claw.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aab2765d-cfc4-4381-8cce-adce5aaf05ad.jpg?1783935734"
        ruling(
            "2017-09-29",
            "If the target of the ability of Tilonalli's Skinshifter becomes an illegal target, " +
                "Tilonalli's Skinshifter won't become a copy of anything. It will remain a 0/1 attacking creature."
        )
        ruling(
            "2017-09-29",
            "Tilonalli's Skinshifter copies the printed values of the target creature, plus any copy effects " +
                "that have been applied to it. It won't copy counters on that creature or effects that have " +
                "changed its power, toughness, types, color, or so on."
        )
        ruling(
            "2017-09-29",
            "Because attackers have already been declared, any abilities Tilonalli's Skinshifter copies that " +
                "trigger when it or other creatures attack won't trigger."
        )
        ruling(
            "2017-09-29",
            "Tilonalli's Skinshifter remains a copy of the creature through the end step. Damage is removed " +
                "from it at the same time that it stops being a copy."
        )
    }
}
