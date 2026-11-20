package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Chromanticore (Born of the Gods #144)
 *
 * A 4/4 with flying, first strike, vigilance, trample and lifelink on its own body; bestowed, the
 * Aura statics hand the host +4/+4 and the same five keywords.
 */
val Chromanticore = card("Chromanticore") {
    manaCost = "{W}{U}{B}{R}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Enchantment Creature — Manticore"
    power = 4
    toughness = 4
    oracleText = "Bestow {2}{W}{U}{B}{R}{G} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "Flying, first strike, vigilance, trample, lifelink\n" +
        "Enchanted creature gets +4/+4 and has flying, first strike, vigilance, trample, and lifelink."

    keywordAbility(KeywordAbility.bestow("{2}{W}{U}{B}{R}{G}"))
    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE, Keyword.VIGILANCE, Keyword.TRAMPLE, Keyword.LIFELINK)

    staticAbility { ability = ModifyStats(4, 4) }
    staticAbility { ability = GrantKeyword(Keyword.FLYING) }
    staticAbility { ability = GrantKeyword(Keyword.FIRST_STRIKE) }
    staticAbility { ability = GrantKeyword(Keyword.VIGILANCE) }
    staticAbility { ability = GrantKeyword(Keyword.TRAMPLE) }
    staticAbility { ability = GrantKeyword(Keyword.LIFELINK) }

    metadata {
        ruling("2013-09-15", "On the stack, a spell with bestow is either a creature spell or an Aura spell. It's never both, although it's an enchantment spell in either case.")
        ruling("2013-09-15", "Unlike other Aura spells, an Aura spell with bestow isn't countered if its target is illegal as it begins to resolve. Rather, the effect making it an Aura spell ends, it loses enchant creature, it returns to being an enchantment creature spell, and it resolves and enters the battlefield as an enchantment creature.")
        ruling("2013-09-15", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature. It can attack (and its {T} abilities can be activated, if it has any) on the turn it becomes unattached if it's been under your control continuously, even as an Aura, since your most recent turn began.")
        ruling("2013-09-15", "If a permanent with bestow enters the battlefield by any method other than being cast, it will be an enchantment creature. You can't choose to pay the bestow cost and have it become an Aura.")
        ruling("2013-09-15", "Auras attached to a creature don't become tapped when the creature becomes tapped. Except in some rare cases, an Aura with bestow remains untapped when it becomes unattached and becomes a creature.")
        rarity = Rarity.MYTHIC
        collectorNumber = "144"
        artist = "Min Yum"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d171b46-69e7-4e39-b3b2-97ed022cd933.jpg?1783939526"
    }
}
