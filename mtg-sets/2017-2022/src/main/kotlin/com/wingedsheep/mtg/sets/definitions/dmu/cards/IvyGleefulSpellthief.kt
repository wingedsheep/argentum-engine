package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Ivy, Gleeful Spellthief (DMU #201)
 * {G}{U}
 * Legendary Creature — Faerie Rogue
 * 2/1
 *
 * Flying
 * Whenever a player casts a spell that targets only a single creature other than Ivy, you may copy
 * that spell. The copy targets Ivy.
 *
 * "Targets only a single creature" is `SpellCastPredicate.TargetsMatching(…, only = true)`: every
 * instance of "target" on the spell is the same creature, and no player. "The copy targets Ivy" is
 * the CR 707.10d copy-per-possible-target shape narrowed to Ivy herself — one copy if Ivy is a legal
 * target for the spell, none otherwise (ruling: "If Ivy isn't a legal target for the spell, no copy
 * can be created"). Ivy's controller makes and controls the copy, whoever cast the original.
 */
val IvyGleefulSpellthief = card("Ivy, Gleeful Spellthief") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Faerie Rogue"
    power = 2
    toughness = 1
    oracleText = "Flying\n" +
        "Whenever a player casts a spell that targets only a single creature other than Ivy, you may " +
        "copy that spell. The copy targets Ivy. (A copy of an Aura spell becomes a token.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts(
            requires = setOf(
                SpellCastPredicate.TargetsMatching(GameObjectFilter.Creature.notSourceItself(), only = true)
            )
        )
        effect = Effects.May(
            Effects.CopyForEachOtherPossibleTarget(candidates = GameObjectFilter.Creature.sourceItself())
        )
        description = "Whenever a player casts a spell that targets only a single creature other than " +
            "Ivy, you may copy that spell. The copy targets Ivy."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "201"
        artist = "Evyn Fong"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d94c15b7-6c8f-45a6-8734-975e3e3b790c.jpg?1783921286"

        ruling("2022-09-09", "The second ability triggers whenever a player casts a spell that targets only one creature and no other object or player. It doesn't matter who controls the creature.")
        ruling("2022-09-09", "If a spell has multiple targets but all of them are the same creature, Ivy, Gleeful Spellthief's second ability will trigger.")
        ruling("2022-09-09", "The copy is created even if the spell that caused the ability to trigger has been countered by the time the ability resolves. The copy resolves before the original spell.")
        ruling("2022-09-09", "If Ivy, Gleeful Spellthief isn't a legal target for the spell, no copy can be created.")
        ruling("2022-09-09", "The copy that Ivy, Gleeful Spellthief's second ability creates is created on the stack, so it's not \"cast.\" Other abilities that trigger when a player casts a spell won't trigger.")
        ruling("2022-09-09", "If the spell that's copied is modal (that is, it says \"Choose one —\" or the like), the copy will have the same mode. You can't choose a different one.")
        ruling("2022-09-09", "If the spell that's copied has an X whose value was determined as it was cast, the copy has the same value of X.")
        ruling("2022-09-09", "The controller of a copy can't choose to pay any additional costs for the copy. However, effects based on any additional costs that were paid for the original spell are copied as though those same costs were paid for the copy too. Notably, if the spell you copy was kicked, the copies will also be kicked.")
    }
}
