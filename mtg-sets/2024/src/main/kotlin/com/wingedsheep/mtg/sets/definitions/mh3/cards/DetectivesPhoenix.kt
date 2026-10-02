package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Detective's Phoenix (Modern Horizons 3 #116)
 *
 * The graveyard permission is restricted to the bestow casting ability (`castUsing`), so from the
 * graveyard it is only ever cast bestowed — for {R} plus collecting evidence 6.
 */
val DetectivesPhoenix = card("Detective's Phoenix") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment Creature — Phoenix"
    power = 2
    toughness = 2
    oracleText = "Bestow—{R}, Collect evidence 6. (To pay this bestow cost, pay {R} and exile cards with total mana value 6 or greater from your graveyard.)\nFlying, haste\nEnchanted creature gets +2/+2 and has flying and haste.\nYou may cast this card from your graveyard using its bestow ability."

    keywordAbility(KeywordAbility.bestow("{R}", Costs.additional.CollectEvidence(6)))
    keywords(Keyword.FLYING, Keyword.HASTE)

    staticAbility { ability = ModifyStats(2, 2) }
    staticAbility { ability = GrantKeyword(Keyword.FLYING) }
    staticAbility { ability = GrantKeyword(Keyword.HASTE) }
    staticAbility { ability = MayCastSelfFromZones(listOf(Zone.GRAVEYARD), castUsing = Keyword.BESTOW) }

    metadata {
        ruling("2024-06-07", "If you can't exile enough cards to meet or exceed the required mana value, you can't choose to collect evidence at all.")
        ruling("2024-06-07", "Once you've announced that you're casting a spell, players can't take actions until you've finished doing so. Notably, opponents can't try to remove cards from your graveyard to stop you from collecting evidence.")
        ruling("2024-06-07", "On the stack, a spell with bestow is either a creature spell or an Aura spell. It's never both, although it's an enchantment spell in either case.")
        ruling("2024-06-07", "Unlike other Aura spells, an Aura spell with bestow isn't countered if its target is illegal as it begins to resolve. Rather, the effect making it an Aura spell ends, it loses enchant creature, it returns to being an enchantment creature spell, and it resolves and enters the battlefield as an enchantment creature.")
        ruling("2024-06-07", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature. It can attack (and its {T} abilities can be activated, if it has any) on the turn it becomes unattached if it's been under your control continuously, even as an Aura, since your most recent turn began.")
        ruling("2024-06-07", "If a permanent with bestow enters the battlefield by any method other than being cast, it will be an enchantment creature. You can't choose to pay the bestow cost and have it become an Aura.")
        ruling("2024-06-07", "Auras attached to a creature don't become tapped when the creature becomes tapped. Except in some rare cases, an Aura with bestow remains untapped when it becomes unattached and becomes a creature.")
        rarity = Rarity.RARE
        collectorNumber = "116"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2a01edd-dbc0-4ed4-b827-9b608290e9a1.jpg?1783911273"
    }
}
