package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ControlEnchantedPermanent
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Hypnotic Siren (Journey into Nyx #42)
 *
 * Bestowed onto an opponent's creature it is a Persuasion with a +1/+1 and flying rider. The
 * Aura-only statics read the attached permanent, so they do nothing while the Siren is an
 * unattached 1/1 flyer.
 */
val HypnoticSiren = card("Hypnotic Siren") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment Creature — Siren"
    power = 1
    toughness = 1
    oracleText = "Bestow {5}{U}{U} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "Flying\n" +
        "You control enchanted creature.\n" +
        "Enchanted creature gets +1/+1 and has flying."

    keywordAbility(KeywordAbility.bestow("{5}{U}{U}"))
    keywords(Keyword.FLYING)

    staticAbility { ability = ControlEnchantedPermanent }
    staticAbility { ability = ModifyStats(1, 1) }
    staticAbility { ability = GrantKeyword(Keyword.FLYING) }

    metadata {
        ruling("2014-04-26", "Gaining control of a creature doesn't cause you to gain control of any Auras or Equipment attached to it.")
        ruling("2014-04-26", "Unlike other Aura spells, an Aura spell with bestow isn't countered if its target is illegal as it begins to resolve. Rather, the effect making it an Aura spell ends, it loses enchant creature, it returns to being an enchantment creature spell, and it resolves and enters the battlefield as an enchantment creature.")
        ruling("2014-04-26", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature.")
        ruling("2013-10-17", "If you cast Hypnotic Siren for its bestow cost, and you gain control of a creature with a constellation ability, that ability will trigger when Hypnotic Siren enters the battlefield. You'll control that triggered ability.")
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b2f562f-00d1-41da-a6ac-bc019cce7fe7.jpg?1783939446"
    }
}
