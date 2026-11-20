package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.MustBeBlocked
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Noble Quarry — Born of the Gods #130 (canonical printing)
 * {2}{G}
 * Enchantment Creature — Unicorn
 * 1/1
 * Bestow {5}{G}
 * All creatures able to block this creature or enchanted creature do so.
 * Enchanted creature gets +1/+1.
 *
 * The Lure clause is two `MustBeBlocked(allCreatures = true)` statics: one source-scoped (binds
 * only while Noble Quarry is itself an attacking creature) and one on the attached creature (binds
 * only while it is bestowed onto an attacker).
 */
val NobleQuarry = card("Noble Quarry") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Unicorn"
    power = 1
    toughness = 1
    oracleText = "Bestow {5}{G} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "All creatures able to block this creature or enchanted creature do so.\n" +
        "Enchanted creature gets +1/+1."

    keywordAbility(KeywordAbility.bestow("{5}{G}"))

    staticAbility { ability = MustBeBlocked(allCreatures = true) }
    staticAbility { ability = MustBeBlocked(allCreatures = true, filter = GroupFilter.attachedCreature()) }
    staticAbility { ability = ModifyStats(1, 1) }

    metadata {
        ruling("2013-09-15", "On the stack, a spell with bestow is either a creature spell or an Aura spell. It's never both, although it's an enchantment spell in either case.")
        ruling("2013-09-15", "Unlike other Aura spells, an Aura spell with bestow isn't countered if its target is illegal as it begins to resolve. Rather, the effect making it an Aura spell ends, it loses enchant creature, it returns to being an enchantment creature spell, and it resolves and enters the battlefield as an enchantment creature.")
        ruling("2013-09-15", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature. It can attack (and its {T} abilities can be activated, if it has any) on the turn it becomes unattached if it's been under your control continuously, even as an Aura, since your most recent turn began.")
        ruling("2013-09-15", "If a permanent with bestow enters the battlefield by any method other than being cast, it will be an enchantment creature. You can't choose to pay the bestow cost and have it become an Aura.")
        ruling("2013-09-15", "Auras attached to a creature don't become tapped when the creature becomes tapped. Except in some rare cases, an Aura with bestow remains untapped when it becomes unattached and becomes a creature.")
        rarity = Rarity.UNCOMMON
        collectorNumber = "130"
        artist = "Michael C. Hayes"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd8447c3-4fce-4971-a3d7-e5671fe269f5.jpg?1783939533"
    }
}
