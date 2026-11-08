package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ojutai Exemplars
 * {2}{W}{W}
 * Creature — Human Monk
 * 4/4
 * Whenever you cast a noncreature spell, choose one —
 * • Tap target creature.
 * • This creature gains first strike and lifelink until end of turn.
 * • Exile this creature, then return it to the battlefield tapped under its owner's control.
 *
 * Canonical printing: Dragons of Tarkir, the card's earliest real-expansion printing. Reprinted in
 * J25 as a `Printing` row.
 *
 * The blink mode is two moves on `Self`. The engine follows the object through the exile move the
 * instruction itself made, so the return finds it; if the Exemplars left the battlefield in
 * response, `Self` no longer names it and neither move happens (the first ruling). The plain
 * `Move` to the battlefield returns it under its owner's control as a new object (CR 400.7) —
 * out of combat, counters and Auras gone.
 */
val OjutaiExemplars = card("Ojutai Exemplars") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Monk"
    oracleText = "Whenever you cast a noncreature spell, choose one —\n" +
        "• Tap target creature.\n" +
        "• This creature gains first strike and lifelink until end of turn.\n" +
        "• Exile this creature, then return it to the battlefield tapped under its owner's control."
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = ModalEffect.chooseOne(
            mode("Tap target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.Tap(creature)
            },
            mode("Ojutai Exemplars gains first strike and lifelink until end of turn") {
                effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self) then
                    Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.Self)
            },
            mode("Exile Ojutai Exemplars, then return it to the battlefield tapped") {
                effect = Effects.Move(EffectTarget.Self, Zone.EXILE) then
                    Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD, placement = ZonePlacement.Tapped)
            },
        )
        description = "Whenever you cast a noncreature spell, choose one — tap target creature; " +
            "or Ojutai Exemplars gains first strike and lifelink until end of turn; or exile Ojutai " +
            "Exemplars, then return it to the battlefield tapped under its owner's control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "27"
        artist = "Willian Murai"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1205482-2f9d-463e-893f-18998aaf09c6.jpg?1783938615"
        ruling(
            "2015-02-25",
            "If Ojutai Exemplars leaves the battlefield in response to its ability with the third " +
                "mode chosen, that ability won't return Ojutai Exemplars to the battlefield. It will " +
                "stay wherever it is."
        )
        ruling(
            "2015-02-25",
            "After the ability returns Ojutai Exemplars to the battlefield, it will be a new object " +
                "with no connection to the Ojutai Exemplars that left the battlefield. It won't be in " +
                "combat or have any additional abilities it may have had when it left the battlefield. " +
                "Any +1/+1 counters on it or Auras attached to it are removed."
        )
    }
}
