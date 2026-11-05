package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Refuse // Cooperate — Hour of Devastation #156
 *
 * Refuse {3}{R} — Instant: Refuse deals damage to target spell's controller equal to that spell's
 *   mana value. (The spell itself is not countered.)
 * Cooperate {2}{U} — Instant: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Copy target instant or sorcery spell. You may choose new targets for the copy.
 */
val RefuseCooperate = card("Refuse // Cooperate") {
    layout = CardLayout.SPLIT
    colorIdentity = "UR"

    face("Refuse") {
        manaCost = "{3}{R}"
        typeLine = "Instant"
        oracleText = "Refuse deals damage to target spell's controller equal to that spell's mana value."

        spell {
            val spell = target(TargetFilter.SpellOnStack)
            // Mana value is read off the live spell, so an announced {X} counts (2017-07-14 ruling).
            effect = Effects.DealDamage(DynamicAmounts.manaValueOf(spell), EffectTarget.TargetController)
        }
    }

    face("Cooperate") {
        manaCost = "{2}{U}"
        typeLine = "Instant"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Copy target instant or sorcery spell. You may choose new targets for the copy."
        keywords(Keyword.AFTERMATH)

        spell {
            val spell = target(TargetFilter.InstantOrSorcerySpellOnStack)
            effect = Effects.CopyTargetSpell(spell)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "156"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/054b07d8-99ae-430b-8e54-f9601fa572e7.jpg?1783936005"
        ruling("2017-07-14", "If a spell has {X} in its mana cost, include the value chosen for that X when determining the mana value of that spell.")
        ruling("2017-07-14", "Cooperate can copy any instant or sorcery spell, not just one with targets.")
        ruling("2017-07-14", "The copy is created on the stack, so it's not \"cast.\" Abilities that trigger when a player casts a spell won't trigger.")
        ruling("2017-07-14", "If the spell that's copied is modal (that is, it says \"Choose one —\" or the like), the copy will have the same mode. A different mode can't be chosen.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
    }
}
