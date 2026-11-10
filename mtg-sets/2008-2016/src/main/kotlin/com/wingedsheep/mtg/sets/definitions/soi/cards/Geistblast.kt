package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Geistblast (SOI #160)
 * {2}{R}
 * Instant
 * Geistblast deals 2 damage to any target.
 * {2}{U}, Exile this card from your graveyard: Copy target instant or sorcery spell you control.
 * You may choose new targets for the copy.
 *
 * The graveyard ability composes the exile-from-graveyard cost (`activateFromZone = Zone.GRAVEYARD`
 * + `Costs.ExileSelf`, as on Gollum's Bite) with the spell-copy effect Lithoform Engine uses. No
 * timing restriction: it's activatable whenever you have priority.
 */
val Geistblast = card("Geistblast") {
    manaCost = "{2}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Geistblast deals 2 damage to any target.\n" +
        "{2}{U}, Exile this card from your graveyard: Copy target instant or sorcery spell you control. " +
        "You may choose new targets for the copy."

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(2, t)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        val spell = target(TargetFilter.InstantOrSorcerySpellOnStack.youControl())
        effect = Effects.CopyTargetSpell(target = spell)
        description = "{2}{U}, Exile this card from your graveyard: Copy target instant or sorcery spell " +
            "you control. You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19b1dfd9-b717-4c23-b8e5-a6ec835b278a.jpg?1783937752"
        ruling(
            "2016-04-08",
            "Geistblast's ability can copy any instant or sorcery spell, not just one with targets."
        )
        ruling(
            "2016-04-08",
            "When Geistblast's ability resolves, it creates a copy of the instant or sorcery spell. The copy is " +
                "created on the stack, so it's not \"cast.\" Abilities that trigger when a player casts a spell " +
                "won't trigger."
        )
        ruling(
            "2016-04-08",
            "If the spell being copied is modal, the copy will have the same mode. If it has an X whose value was " +
                "determined as it was cast, the copy will have the same value of X. If an alternative cost was " +
                "paid for the spell, the same alternative cost is considered to have been paid for the copy."
        )
    }
}
