package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Flitterstep Eidolon — unblockable on its own body (the creature flag); bestowed, the Aura's
 * statics hand the host +1/+1 and `CantBeBlocked(GroupFilter.attachedCreature())`
 * (Ghostblade Eidolon's shape with Cloak of Mists' evasion).
 */
val FlitterstepEidolon = card("Flitterstep Eidolon") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment Creature — Spirit"
    power = 1
    toughness = 1
    oracleText = "Bestow {5}{U} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "This creature can't be blocked.\n" +
        "Enchanted creature gets +1/+1 and can't be blocked."

    keywordAbility(KeywordAbility.bestow("{5}{U}"))
    flags(AbilityFlag.CANT_BE_BLOCKED)

    staticAbility { ability = ModifyStats(1, 1) }
    staticAbility { ability = CantBeBlocked(GroupFilter.attachedCreature()) }

    metadata {
        ruling("2013-09-15", "On the stack, a spell with bestow is either a creature spell or an Aura spell. It's never both, although it's an enchantment spell in either case.")
        ruling("2013-09-15", "Unlike other Aura spells, an Aura spell with bestow isn't countered if its target is illegal as it begins to resolve. Rather, the effect making it an Aura spell ends, it loses enchant creature, it returns to being an enchantment creature spell, and it resolves and enters the battlefield as an enchantment creature.")
        ruling("2013-09-15", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature. It can attack (and its {T} abilities can be activated, if it has any) on the turn it becomes unattached if it's been under your control continuously, even as an Aura, since your most recent turn began.")
        ruling("2013-09-15", "If a permanent with bestow enters the battlefield by any method other than being cast, it will be an enchantment creature. You can't choose to pay the bestow cost and have it become an Aura.")
        ruling("2013-09-15", "Auras attached to a creature don't become tapped when the creature becomes tapped. Except in some rare cases, an Aura with bestow remains untapped when it becomes unattached and becomes a creature.")
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a79cee0-6023-42bc-a934-69b99fd5fb02.jpg?1783939569"
    }
}
