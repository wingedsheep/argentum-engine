package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.eternalize
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Resilient Khenra — Hour of Devastation #131 (canonical printing)
 * {1}{G} · Creature — Jackal Wizard · 2/2
 *
 * When this creature enters, you may have target creature get +X/+X until end of turn, where X
 * is this creature's power.
 * Eternalize {4}{G}{G}
 *
 * X is `sourcePower()` read as the trigger resolves — the triggered ability's last-known source
 * snapshot covers a Khenra (or eternalized token) that has left the battlefield by then — and
 * wrapped in `nonNegative` because a negative power counts as 0 (2017-07-14 ruling).
 */
val ResilientKhenra = card("Resilient Khenra") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Jackal Wizard"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you may have target creature get +X/+X until end of turn, " +
        "where X is this creature's power.\n" +
        "Eternalize {4}{G}{G} ({4}{G}{G}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a 4/4 black Zombie Jackal Wizard with no mana cost. Eternalize only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        val creature = target(TargetFilter.Creature)
        val x = DynamicAmounts.nonNegative(DynamicAmounts.sourcePower())
        effect = Effects.ModifyStats(power = x, toughness = x, target = creature)
    }

    eternalize("{4}{G}{G}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "131"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/033a69fb-2af9-426c-b1d6-3e5b18c78c9a.jpg?1783936014"
        ruling("2017-07-14", "The value of X is determined only as Resilient Khenra's triggered ability resolves. Once that happens, the value of X won't change later in the turn even if Resilient Khenra's power changes.")
        ruling("2017-07-14", "If Resilient Khenra leaves the battlefield before its triggered ability resolves, use its power as it last existed on the battlefield to determine the value of X.")
        ruling("2017-07-14", "If Resilient Khenra's power is negative as its triggered ability resolves, X is considered to be 0. (This is a change from previous rules.)")
        ruling("2017-07-14", "For each card with eternalize, a corresponding game play supplement token can be found in some Hour of Devastation booster packs. These supplements are not required to play with cards with eternalize; you can use the same items to represent an eternalized token as you would any other token.")
        ruling("2017-07-14", "If a creature card with eternalize is put into your graveyard during your main phase, you'll have priority immediately afterward. You can activate its eternalize ability before any player can try to exile it, such as with Crook of Condemnation, if it's legal for you to do so.")
        ruling("2017-07-14", "Once you've activated an eternalize ability, the card is immediately exiled. Opponents can't try to stop the ability by exiling the card with an effect such as that of Crook of Condemnation.")
        ruling("2017-07-14", "The token copies exactly what was printed on the original card and nothing else, except the characteristics specifically modified by eternalize. It doesn't copy any information about the object the card was before it was put into your graveyard.")
        ruling("2017-07-14", "The token is a Zombie in addition to its other types and is black instead of its other colors. Its base power and toughness are 4/4. It has no mana cost, and thus its mana value is 0. These are copiable values of the token that other effects may copy.")
        ruling("2017-07-14", "If the card copied by the token had any \"when [this permanent] enters the battlefield\" abilities, then the token also has those abilities and will trigger them when it's created. Similarly, any \"as [this permanent] enters the battlefield\" or \"[this permanent] enters the battlefield with\" abilities that the token has copied will also work.")
    }
}
