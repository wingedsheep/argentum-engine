package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Clone Legion
 * {7}{U}{U}
 * Sorcery
 * For each creature target player controls, create a token that's a copy of that creature.
 *
 * `ForEachInGroup` snapshots the creatures the targeted player controls at resolution and
 * creates one copy per creature ([EffectTarget.IterationEntity]), under the caster's control
 * (the copy effect's default controller). A group loop that creates tokens shares one entry
 * group, so every token enters at the same time (2015-02-25 ruling), and the fresh copies are
 * not re-iterated.
 */
val CloneLegion = card("Clone Legion") {
    manaCost = "{7}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "For each creature target player controls, create a token that's a copy of that creature."

    spell {
        val player = target(Targets.Player)
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player)),
            effect = Effects.CreateTokenCopyOfTarget(target = EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "48"
        artist = "Svetlin Velinov"
        flavorText = "Their lust for battle was so great that they didn't care their enemies wore their own faces."
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45c6a201-1d4d-4ae5-a84e-098e094eaf61.jpg?1783938609"
        ruling(
            "2015-02-25",
            "Any enters-the-battlefield abilities of the copied creature will trigger when the token enters " +
                "the battlefield. Any \"as [this permanent] enters the battlefield\" or \"[this permanent] enters " +
                "the battlefield with\" abilities of the copied creature will also work."
        )
        ruling("2015-02-25", "The token creatures all enter the battlefield at the same time.")
        ruling(
            "2015-02-25",
            "A token that enters the battlefield as a copy of a face-down creature is a face-up colorless 2/2 " +
                "creature with no name, abilities, or creature types."
        )
        ruling(
            "2015-02-25",
            "If the copied creature is a token, the token created by Clone Legion copies the original " +
                "characteristics of that token as stated by the effect that put the token onto the battlefield."
        )
        ruling(
            "2015-02-25",
            "If the copied creature is copying something else, then the token enters the battlefield as " +
                "whatever that creature copied."
        )
        ruling("2015-02-25", "If the copied creature has {X} in its mana cost, X is considered to be zero.")
        ruling(
            "2015-02-25",
            "Each token copies exactly what was printed on the original creature and nothing else (unless that " +
                "permanent is copying something else or is a token; see below). It doesn't copy whether that " +
                "creature is tapped or untapped, whether it has any counters on it or Auras and Equipment attached " +
                "to it, or any non-copy effects that have changed its power, toughness, types, color, and so on."
        )
        ruling(
            "2015-02-25",
            "Remember that if you control more than one legendary permanent with the same name, you'll choose " +
                "one to remain on the battlefield and put the rest into their owners' graveyards."
        )
    }
}
